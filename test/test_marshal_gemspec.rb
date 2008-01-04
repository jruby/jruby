require 'test/unit'
require 'rubygems'
require 'yaml'

# This test demonstrates and verifies the marshalling fix for JRUBY-1877.
class TestMarshalGemspec < Test::Unit::TestCase
  def setup
    @index = YAML::load <<-YAML
--- !ruby/object:Gem::Cache
gems:
  activerecord-jdbcderby-adapter-0.6: !ruby/object:Gem::Specification
    rubygems_version: 0.9.4
    specification_version: 1
    name: activerecord-jdbcderby-adapter
    version: !ruby/object:Gem::Version
      version: "0.6"
    date: 2007-11-04 22:00:00 -08:00
    summary: Derby JDBC adapter for JRuby on Rails.
    require_paths:
    - lib
    email: nick@nicksieger.com, ola.bini@gmail.com
    homepage: http://jruby-extras.rubyforge.org/ActiveRecord-JDBC
    rubyforge_project: jruby-extras
    description: Install this gem to use Derby with JRuby on Rails.
    autorequire:
    default_executable:
    bindir: bin
    has_rdoc: true
    required_ruby_version: !ruby/object:Gem::Version::Requirement
      requirements:
      - - ">"
        - !ruby/object:Gem::Version
          version: 0.0.0
      version:
    platform: ruby
    signing_key:
    cert_chain: []

    post_install_message:
    authors:
    - Nick Sieger, Ola Bini and JRuby contributors
    files: []

    test_files: []

    rdoc_options: []

    extra_rdoc_files: []

    executables: []

    extensions: []

    requirements: []

    dependencies:
    - !ruby/object:Gem::Dependency
      name: activerecord-jdbc-adapter
      version_requirement:
      version_requirements: !ruby/object:Gem::Version::Requirement
        requirements:
        - - ">="
          - !ruby/object:Gem::Version
            version: "0.6"
        version:
    - !ruby/object:Gem::Dependency
      name: jdbc-derby
      version_requirement:
      version_requirements: !ruby/object:Gem::Version::Requirement
        requirements:
        - - ">="
          - !ruby/object:Gem::Version
            version: 10.2.2.0
        version:
YAML
    @gemspec = @index.search("activerecord-jdbcderby-adapter").first
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

  def test_dump_and_load_index_from_yaml
    newindex = Marshal.load(Marshal.dump(@index))
    newspec = newindex.search("activerecord-jdbcderby-adapter").first
    assert_gemspec(newspec)
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
      s.has_rdoc = true
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