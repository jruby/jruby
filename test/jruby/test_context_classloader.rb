require 'test/unit'
require 'test/jruby/test_helper'

class TestContextClassloader < Test::Unit::TestCase
  include TestHelper
  
  def setup
    require 'jruby'
    @jruby_classloader = JRuby.runtime.jruby_class_loader
  end
  
  def test_main_thread
    # This launches externally because our test run doesn't call through Main,
    # so the executing thread's context classloader is not set to JRuby's.
    assert_equal("true\n",
      jruby('-rjava -rjruby -e "p(JRuby.runtime.jruby_class_loader == java.lang.Thread.current_thread.context_class_loader)"'))
  end
  
  def test_ruby_thread
    t = Thread.new { java.lang.Thread.current_thread.context_class_loader }
    assert_equal(@jruby_classloader, t.value)
  end
end
