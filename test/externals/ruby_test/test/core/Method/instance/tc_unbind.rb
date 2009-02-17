########################################################################
# tc_unbind.rb
#
# Test case for the Method#unbind instance method.
########################################################################
require 'test/unit'

class MethodUnbind
   def some_method
      'unbind'
   end
end

class TC_Method_Unbind_InstanceMethod < Test::Unit::TestCase
   def setup
      @unbind = MethodUnbind.new
      @method = @unbind.method(:some_method)
      @ubmeth = nil
   end

   def test_unbind_basic
      assert_respond_to(@method, :unbind)
      assert_nothing_raised{ @method.unbind }
      assert_kind_of(UnboundMethod, @method.unbind)
   end

   def test_unbind
      assert_nothing_raised{ @ubmeth = @method.unbind }
      assert_kind_of(UnboundMethod, @ubmeth)
      assert_respond_to(@ubmeth, :arity)
      assert_respond_to(@ubmeth, :bind)
   end

   def test_unbind_expected_errors
      assert_raise(ArgumentError){ @method.unbind(true) }
   end

   def teardown
      @unbind = nil
      @method = nil
      @ubmeth = nil
   end
end
