require 'test/unit'
require 'jruby'
require 'test/jruby/test_helper'

class TestContextClassloader < Test::Unit::TestCase
  include TestHelper
  JThread = java.lang.Thread
  Runnable = java.lang.Runnable
  
  def setup
    @jruby_classloader = JRuby.runtime.jruby_class_loader
  end
  
  def test_main_thread
    assert_equal(JRuby.runtime.jruby_class_loader.parent, org.jruby.Ruby.java_class.class_loader || java.lang.Thread.current_thread.context_class_loader)
  end
  
  def test_ruby_thread
    t = Thread.new { JThread.current_thread.context_class_loader }
    assert_equal(@jruby_classloader, t.value)
  end
end
