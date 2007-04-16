require 'test/unit'
require 'java'

# This tests JRuby-specific environment stuff, like JRUBY_VERSION
class TestEnv < Test::Unit::TestCase
  def test_jruby_version
    assert defined? JRUBY_VERSION
    assert String === JRUBY_VERSION
    assert_equal(org.jruby.runtime.Constants::VERSION, JRUBY_VERSION)
  end
end
