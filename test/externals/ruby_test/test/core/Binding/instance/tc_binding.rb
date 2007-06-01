##########################################################################
# tc_binding.rb
#
# Test case for Binding objects.
##########################################################################
require 'test/unit'

class FooBinding
   def initialize(val)
      @secret = val
   end

   def test_method
      "hello"
   end

   def get_binding
      return binding()
   end
end

class TC_Binding < Test::Unit::TestCase
   def setup
      @foo1 = FooBinding.new('x')
      @foo2 = FooBinding.new(99)
      @foo3 = FooBinding.new(nil)
   end

   def test_binding_basic
      assert_nothing_raised{ @foo1.get_binding }
      assert_kind_of(Binding, @foo1.get_binding)
   end

   def test_get_binding_of_object
      assert_nothing_raised{ @foo1.get_binding }
      assert_nothing_raised{ @foo2.get_binding }
      assert_nothing_raised{ @foo3.get_binding }
   end

   def test_binding_eval
      assert_equal('x', eval("@secret", @foo1.get_binding))
      assert_equal(99, eval("@secret", @foo2.get_binding))
      assert_equal(nil, eval("@secret", @foo3.get_binding))

      assert_equal(FooBinding, eval("FooBinding", @foo1.get_binding))
      assert_equal("hello", eval("test_method", @foo1.get_binding))
   end

   def teardown
      @foo1 = nil
      @foo2 = nil
      @foo3 = nil
   end
end
