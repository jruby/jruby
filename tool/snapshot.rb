#!/usr/bin/env ruby

require 'yaml'

abort "jruby.properties filename location needed" unless ARGV[0]

def update_jruby_properties(url, tag, revision)
  properties = File.open(ARGV[0]) {|f| f.read}
#  properties.sub!(/^version.jruby=.*$/, "version.jruby=#{tag}-#{revision}")
  properties.sub!(/Revision: \d+/, "Revision: #{revision}")
  properties << "\nurl=#{url}\nrevision=#{revision}\n"
  File.open(ARGV[0], "w") {|f| f << properties }
end

MAX_GIT_COMMITS = 100

# look through the last MAX_GIT_COMMITS for a git-svn-id: for jruby
# if a git-svn-id for jruby is found return: [url, tag, revision, git_commits_searched]
# else nil
def find_last_git_svn_rev
  re = /git-svn-id: https:\/\/svn.codehaus.org\/jruby\/(.*)\/(.*)@(.*) /
  return (0..MAX_GIT_COMMITS).each do |n|
    last_commit = `git rev-list HEAD --pretty=raw --no-color --max-count=1 --skip=#{n}`
    break nil if last_commit.empty? || n == MAX_GIT_COMMITS
    next unless match = re.match(last_commit)
    if match[1][/(tags|branches)/]
      tag = tag = match[2]
    else
      tag = "trunk"
    end
    revision = match[3]
    url = last_commit[/git-svn-id: (https:.*)@/, 1]
    break [url, tag, revision, n] if url && tag && revision
  end
end

if File.exist? '.svn'
  svn_props = YAML::load(`svn info`)
  # true if we are working from a svn checkout
  url = svn_props["URL"]
  revision = svn_props["Revision"].to_s
  path = url =~ /#{svn_props["Repository Root"]}\/(.*)/ && $1
  tag = case path
  when /trunk/
    "trunk"
  when /(tags|branches)\/(.*)/
    "#{$1.sub(/e?s$/, '')}-#{$2}"
  else
    path.gsub(%r{/}, '-')
  end
  update_jruby_properties(url, tag, revision)
elsif (url, tag, revision, git_commits_searched = find_last_git_svn_rev)[0]
  revision << "+#{git_commits_searched}" if git_commits_searched > 0
  update_jruby_properties(url, tag, revision)
end

