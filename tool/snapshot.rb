#!/usr/bin/env ruby

require 'yaml'

abort "jruby.properties filename location needed" unless ARGV[0]

def update_jruby_properties(revision)
  properties = File.open(ARGV[0]) {|f| f.read}
  properties.sub!(/Revision: \d+/, "Revision: #{revision}")
  File.open(ARGV[0], "w") {|f| f << properties }
end

# look through the last MAX_GIT_COMMITS for a git-svn-id: for jruby
# if a git-svn-id for jruby is found return: [url, tag, revision, git_commits_searched]
# else nil
def find_last_git_rev
  re = /commit (.......).*/
  last_commit = `git rev-list HEAD --pretty=raw --no-color --max-count=1`
  return nil unless match = re.match(last_commit)
  return match[1]
end

if revision = find_last_git_rev
  update_jruby_properties(revision)
end

