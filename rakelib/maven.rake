
# Assumes this file is in rakelib and that a top-level pom.xml file exists.
def maven_retrieve_pom_version
  require 'rexml/document'
  file = File.new(File.join(File.dirname(__FILE__), '..', 'pom.xml'))
  REXML::Document.new(file).elements.each("project/version"){|e| return e.text}
  raise Errno::ENOENT.new "Cannot find project pom.xml"
end

namespace :maven do
  desc "Update versions in maven poms with string passed in ENV['VERSION']"
  task :updatepoms do
    version = ENV['VERSION'] or abort("Pass the new version with VERSION={version}")
    system "mvn versions:set -DnewVersion=#{version}"
  end
end
