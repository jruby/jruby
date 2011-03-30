require File.dirname(__FILE__) + "/../spec_helper"

require 'rubygems'
require 'rubygems/format'
require 'rubygems/maven_gemify'
require 'yaml'

begin
  Gem::Maven::Gemify.verbose = true if $DEBUG || ENV['DEBUG']
  Gem::Maven::Gemify.maven

  describe Gem::MavenUtils, "maven_name?" do
    include Gem::MavenUtils
    it "matches colon-separated artifacts and needs tp start with 'mvn:'" do
      maven_name?('mvn:commons-lang:commons-lang').should be_true
    end

    it "does not match things that look like a windows filename" do
      maven_name?('c:ommons-lang:commons-lang').should be_false
      maven_name?('c:/temp/somefile').should be_false
    end

    it "does not match URLs" do
      maven_name?('http://example.com/some.gem').should be_false
    end
    it "does not match without 'mvn:' prefix" do
      maven_name?('commons-lang:commons-lang').should be_false
    end
  end

  describe Gem::SpecFetcher do
    it "downloads specs for maven artifacts" do
      matching = Gem::SpecFetcher.fetcher.find_matching Gem::Dependency.new("mvn:commons-lang:commons-lang")
      matching.should_not be_empty
    end
  end

  describe Gem::Maven::Gemify do
    it "creates an instance of the Maven class" do
      Gem::Maven::Gemify.maven.should be_kind_of(org.apache.maven.Maven)
    end

    it "gets a list of versions for a maven artifact" do
      Gem::Maven::Gemify.new.get_versions("mvn:commons-lang:commons-lang").should include("2.5.0")
    end

    it "gets a list of versions for a maven artifact from external repository" do
      Gem::Maven::Gemify.new('https://repository.jboss.org/nexus/content/groups/public-jboss').get_versions("mvn:org.jboss.logging:jboss-logging").should include("3.0.0.b.5")
    end

    it "allows use of colons as artifact delimiters" do
      Gem::Maven::Gemify.new.get_versions("mvn:commons-lang:commons-lang").should include("2.5.0")
    end

    it "generates a gemspec file for the maven artifact" do
      specfile = Gem::Maven::Gemify.new.generate_spec("mvn:commons-lang:commons-lang", "2.5.0")
      specfile.should_not be_nil
      gemspec = Gem::Specification.from_yaml(File.read(specfile))
      gemspec.name.should == "mvn:commons-lang:commons-lang"
      gemspec.version.should == Gem::Version.new("2.5.0")
    end

    it "generates a .gem for the maven artifact" do
      gemfile = Gem::Maven::Gemify.new.generate_gem("mvn:commons-lang:commons-lang", "2.5.0")
      format = Gem::Format.from_file_by_path gemfile
      format.file_entries.detect{|fe| fe[0]["path"] =~ /commons-lang\.jar/}.should be_true
    end

    it "generates a spec with a Gem::Dependency list for artifacts with dependencies" do
      specfile = Gem::Maven::Gemify.new.generate_spec("mvn:commons-logging:commons-logging", "1.1.1")
      specfile.should_not be_nil
      gemspec = Gem::Specification.from_yaml(File.read(specfile))
      gemspec.dependencies.length.should == 1
      gemspec.dependencies[0].name.should == 'mvn:junit:junit'
      gemspec.dependencies[0].type.should == :development
    end

    it "allows a non-standard maven repository" do
      gemify = Gem::Maven::Gemify.new "http://maven.glassfish.org/content/groups/public/"
      specfile = gemify.generate_spec("mvn:com.sun.akuma:akuma", "1.3")
      specfile.should_not be_nil
      gemspec = Gem::Specification.from_yaml(File.read(specfile))
      gemspec.dependencies.length.should == 1
      gemspec.dependencies[0].name.should == 'mvn:net.java.dev.jna:jna'
    end

    it "accepts a variety of string or URI parameters to #initialize" do
      expected = ["http://repository.codehaus.org/"]
      Gem::Maven::Gemify.new("http://repository.codehaus.org/").repositories.map(&:to_s).should == expected
      Gem::Maven::Gemify.new(URI.parse("http://repository.codehaus.org/")).repositories.map(&:to_s).should == expected
      Gem::Maven::Gemify.new(["http://repository.codehaus.org/"]).repositories.map(&:to_s).should == expected
      Gem::Maven::Gemify.new("mvn://repository.codehaus.org/").repositories.map(&:to_s).should == expected
      Gem::Maven::Gemify.new(URI.parse("mvn://repository.codehaus.org/")).repositories.map(&:to_s).should == expected
      Gem::Maven::Gemify.new(URI.parse("mvn:central")).repositories.should == [nil]
    end
  end
rescue => e
  puts(e, *e.backtrace) if $DEBUG || ENV['DEBUG']
  # Skipping maven specs w/o M3 or ruby-maven gem installed
end
