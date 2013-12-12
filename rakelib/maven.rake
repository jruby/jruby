
require 'fileutils'

# Assumes this file is in rakelib and that a top-level pom.xml file exists.
def maven_retrieve_pom_version
  require 'rexml/document'
  file = File.new(File.join(File.dirname(__FILE__), '..', 'pom.xml'))
  REXML::Document.new(file).elements.each("project/version"){|e| return e.text}
  raise Errno::ENOENT.new "Cannot find project pom.xml"
end

namespace :maven do
  desc "Prepare for the release"
  task :prepare_release do
    system "mvn versions:set"
    system "mvn clean install -Pall"
    tree = File.expand_path(File.join(File.dirname(__FILE__), '..', 'target', 'tree.txt') )
    FileUtils.mkdir_p( File.dirname( tree ) )
    FileUtils.rm_f( tree )
    system "mvn dependency:tree -Doutput=#{tree} -DappendOutput"
    deps = File.read( tree )
    raise "found SNAPSHOTS #{deps}" if deps.match 'SNAPSHOT'
  end

  desc "Deploy release and bump version"
  task :deploy_release do
    system "mvn clean deploy -Psonatype-oss-release,release"
    system "mvn versions:set"
  end
end
