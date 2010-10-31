require 'test/unit'
require 'test/test_helper'
require 'java'

# This tests JRuby-specific environment stuff, like JRUBY_VERSION
class TestEnv < Test::Unit::TestCase
  include TestHelper

  def test_jruby_version
    assert defined? JRUBY_VERSION
    assert String === JRUBY_VERSION
    assert_equal(org.jruby.runtime.Constants::VERSION, JRUBY_VERSION)
  end

  if WINDOWS
    def test_case_insensitive_on_windows
      path = ENV['PATH']
      cased_path = ENV['path']

      assert_equal(path, cased_path)
    end

    def test_env_java_case_sensitivity_on_windows
      bit = ENV_JAVA['java.home']
      cased_bit = ENV_JAVA['Java.Home']

      assert_not_equal(bit, cased_bit)
    end
  end
end
