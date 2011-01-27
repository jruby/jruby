require 'test/unit'
require 'yaml'

class YAMLUnit19 < Test::Unit::TestCase
    # JRUBY-5387
    def test_argument_error_with_invalid_syntax
      assert_raise(ArgumentError) do
        YAML.load(<<EOY)
Gem::Specification.new do |s|
  s.platform    = Gem::Platform::RUBY
  s.name        = 'railties'
  s.version     = version
  s.summary     = 'Tools for creating, working with, and running Rails applications.'
  s.description = 'Rails internals: application bootup, plugins, generators, and rake tasks.'
EOY
      end
    end

end
