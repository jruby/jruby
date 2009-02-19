# JRUBY-3253: Lack of default jruby.home in embedded usage causes NPE
require 'test/unit'
require 'java'

class TestMissingJRubyHome < Test::Unit::TestCase
  def test_missing_jruby_home
    old = java.lang.System.get_property('jruby.home')
    java.lang.System.clear_property('jruby.home')
    begin
      runtime = org.jruby.Ruby.new_instance
      assert_nothing_raised do
        runtime.eval_scriptlet('require "rbconfig"')
      end
    ensure
      java.lang.System.set_property('jruby.home', old)
    end
  end
end