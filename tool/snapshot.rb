#!/usr/bin/env ruby

require 'yaml'

abort "jruby.properties filename location needed" unless ARGV[0]

svn_props = YAML::load(`svn info`)

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

properties = File.open(ARGV[0]) {|f| f.read}
properties.sub!(/^version.jruby=.*$/, "version.jruby=#{tag}-#{revision}")
properties.sub!(/Revision: \d+/, "Revision: #{revision}")
properties << "\nurl=#{url}\nrevision=#{revision}\n"

File.open(ARGV[0], "w") {|f| f << properties }
