require 'test/unit'
require 'jruby'
require 'test/test_helper'

class TestContextClassloader < Test::Unit::TestCase
  include TestHelper
  JThread = java.lang.Thread
  Runnable = java.lang.Runnable
  
  def setup
    @jruby_classloader = JRuby.runtime.jruby_class_loader
  end
  
  def test_main_thread
    # This launches externally because our test run doesn't call through Main,
    # so the executing thread's context classloader is not set to JRuby's.
    assert_equal("true\n",
      jruby('-rjava -rjruby -e "p(JRuby.runtime.jruby_class_loader == java.lang.Thread.current_thread.context_class_loader)"'))
  end
  
  def test_ruby_thread
    t = Thread.new { JThread.current_thread.context_class_loader }
    assert_equal(@jruby_classloader, t.value)
  end
end
