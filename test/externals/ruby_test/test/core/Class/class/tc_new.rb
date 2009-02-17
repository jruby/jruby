######################################################################
# tc_new.rb
#
# Test case for the Class.new method.
######################################################################
require 'test/unit'

class TC_Class_New_Class < Test::Unit::TestCase
   class FooNew; end

   def setup
      @obj = nil
      @foo = FooNew.new
      @singleton = class << @foo; self; end
      @block = lambda{
         def hello
            "hello"
         end
      }
   end

   def test_new_basic
      assert_nothing_raised{ @obj = Class.new }
      assert_kind_of(Object, @obj)
   end

   def test_new_alternate_superclass
      assert_nothing_raised{ @obj = Class.new(Array) }
      assert_kind_of(Class, @obj)
      assert_kind_of(Array, @obj.new)
   end

   def test_new_with_block
      assert_nothing_raised{ Class.new{ } }
      assert_nothing_raised{ @obj = Class.new(&@block) }
      assert_equal('hello', @obj.new.hello)
   end

   def test_new_expected_errors
      assert_raise(LocalJumpError){ Class.new{ yield } }
      assert_raise(TypeError){ @singleton.new }
   end

   def teardown
      @obj   = nil
      @foo   = nil
      @block = nil
      @singleton = nil
   end
end
