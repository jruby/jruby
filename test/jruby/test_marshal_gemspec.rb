require 'test/unit'
require 'rubygems'
require 'yaml'

# This test demonstrates and verifies the marshalling fix for JRUBY-1877.
class TestMarshalGemspec < Test::Unit::TestCase
  def setup
    require 'yaml'
    @gemspec = YAML::load(<<-YAML, aliases: true, permitted_classes: [Symbol, Time, Gem::Specification, Gem::Version, Gem::Dependency, Gem::Requirement])
--- !ruby/object:Gem::Specification 
name: activerecord-jdbcderby-adapter
version: !ruby/object:Gem::Version 
  prerelease: 
  version: 1.2.1
platform: ruby
authors: 
  - Nick Sieger, Ola Bini and JRuby contributors
autorequire: 
bindir: bin
cert_chain: []

date: 2011-11-23 00:00:00 Z
dependencies: 
  - !ruby/object:Gem::Dependency 
    name: activerecord-jdbc-adapter
    prerelease: false
    requirement: &id001 !ruby/object:Gem::Requirement 
      none: false
      requirements: 
        - - ~>
          - !ruby/object:Gem::Version 
            version: 1.2.1
    type: :runtime
    version_requirements: *id001
  - !ruby/object:Gem::Dependency 
    name: jdbc-derby
    prerelease: false
    requirement: &id002 !ruby/object:Gem::Requirement 
      none: false
      requirements: 
        - - ~>
          - !ruby/object:Gem::Version 
            version: 10.6.0
    type: :runtime
    version_requirements: *id002
description: Install this gem to use Derby with JRuby on Rails.
email: nick@nicksieger.com, ola.bini@gmail.com
executables: []

extensions: []

extra_rdoc_files: []

files: []

homepage: https://github.com/jruby/activerecord-jdbc-adapter
licenses: []

post_install_message: 
rdoc_options: []

require_paths: 
  - lib
required_ruby_version: !ruby/object:Gem::Requirement 
  none: false
  requirements: 
    - - ">="
      - !ruby/object:Gem::Version 
        version: "0"
required_rubygems_version: !ruby/object:Gem::Requirement 
  none: false
  requirements: 
    - - ">="
      - !ruby/object:Gem::Version 
        version: "0"
requirements: []

rubyforge_project: jruby-extras
rubygems_version: 1.8.12
signing_key: 
specification_version: 3
summary: Derby JDBC adapter for JRuby on Rails.
test_files: []
YAML
  end

  def assert_gemspec(newspec)
    assert @gemspec
    assert newspec
    assert_equal @gemspec.name, newspec.name
    assert_equal @gemspec.dependencies.size, newspec.dependencies.size
    assert_equal @gemspec.dependencies[0], newspec.dependencies[0]
    assert_equal @gemspec.dependencies[1], newspec.dependencies[1]
  end

  def test_dump_and_load_gemspec_from_yaml
    assert_gemspec(Marshal.load(Marshal.dump(@gemspec)))
  end

  def test_dump_and_load_from_source
    @gemspec = Gem::Specification.new do |s|
      s.name = %q{activerecord-jdbcderby-adapter}
      s.version = "0.6"
      s.specification_version = 1 if s.respond_to? :specification_version=
      s.required_rubygems_version = nil if s.respond_to? :required_rubygems_version=
      s.authors = ["Nick Sieger, Ola Bini and JRuby contributors"]
      s.date = %q{2007-11-04}
      s.description = %q{Install this gem to use Derby with JRuby on Rails.}
      s.email = %q{nick@nicksieger.com, ola.bini@gmail.com}
      s.homepage = %q{http://jruby-extras.rubyforge.org/ActiveRecord-JDBC}
      s.require_paths = ["lib"]
      s.required_ruby_version = Gem::Requirement.new("> 0.0.0")
      s.rubyforge_project = %q{jruby-extras}
      s.rubygems_version = %q{1.0.1}
      s.summary = %q{Derby JDBC adapter for JRuby on Rails.}
      s.add_dependency(%q<activerecord-jdbc-adapter>, [">= 0.6"])
      s.add_dependency(%q<jdbc-derby>, [">= 10.2.2.0"])
    end
    test_dump_and_load_gemspec_from_yaml
  end
end