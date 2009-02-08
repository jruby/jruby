require 'test/unit'

# JRUBY-2921
class TestMethodOverrideAndCaching < Test::Unit::TestCase
  def calling_meth1
    MySub.meth1
  end
 
  def calling_meth2
    MySub.meth2
  end
 
  class MySuper
    def self.meth1
      'MySuper::meth1'
    end
    def self.meth2
      'MySuper::meth2'
    end
  end
 
  class MySub < MySuper
  end
 
  def test_jruby_2921
    assert_equal 1, MySub.methods.select {|m| m == 'meth1'}.size
    assert_equal 1, MySub.methods.select {|m| m == 'meth2'}.size
 
    assert_equal('MySuper::meth1', MySub::meth1)
    assert_equal('MySuper::meth1', calling_meth1)
    assert_equal('MySuper::meth2', MySub::meth2)
    # Note: calling_meth2 is not called here
 
    self.class.class_eval " 
      class MySub
        def self.meth1
          'MySub::meth1'
        end
        def self.meth2
          'MySub::meth2'
        end
      end
    "
 
    assert_equal 1, MySub.methods.select {|m| m == 'meth1'}.size
    assert_equal 1, MySub.methods.select {|m| m == 'meth2'}.size
 
    assert_equal('MySub::meth1', MySub::meth1)
    assert_equal('MySub::meth1', calling_meth1)
    assert_equal('MySub::meth2', MySub::meth2)
    assert_equal('MySub::meth2', calling_meth2)
  end
end

