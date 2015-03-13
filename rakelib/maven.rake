#-*- mode: ruby -*-

require 'fileutils'

# Assumes this file is in rakelib and that a top-level pom.xml file exists.
def maven_retrieve_pom_version
  require 'rexml/document'
  file = File.new(File.join(File.dirname(__FILE__), '..', 'pom.xml'))
  REXML::Document.new(file).elements.each("project/version"){|e| return e.text}
  raise Errno::ENOENT.new "Cannot find project pom.xml"
end

def maven
  unless @__maven__
    require 'maven/ruby/maven'
    @__maven__ = Maven::Ruby::Maven.new
    @__maven__.embedded = true
  end
  @__maven__
rescue LoadError => e
  warn 'make sure you have ruby-maven gem installed'
  raise e
end

namespace :maven do

  desc "Dump pom.xml files"
  task :dump_poms do
    maven.install( '-Pall' )
  end

  desc "Set new version"
  task :set_version => [:do_set_version, :dump_poms ]

  task :do_set_version do
    version = readline
    version.strip!
    File.open( '../VERSION', 'w' ) { |f| f.print version }
  end

  desc "Prepare for the release"
  task :prepare_release => :do_set_version do
    maven.exec( :clean, :install, '-Prelease' )
  end

  desc "Deploy release and bump version"
  task :deploy_release do
    system "mvn clean deploy -Psonatype-oss-release,release"
  end
end
