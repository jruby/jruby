########################################################################
# tc_call.rb
#
# Test case for the Method#call instance method and the Method#[]
# instance method.
########################################################################
require 'test/unit'

class MethodCall
   def none; nil; end
   def one(arg1); 1; end
   def multi(arg1, *args); args.inject(arg1){ |m, x| m += x }; end
   def default(arg1, arg2=nil, *args); arg2; end
end

class TC_Method_Call_InstanceMethod < Test::Unit::TestCase
   def setup
      @none    = MethodCall.new.method(:none)
      @one     = MethodCall.new.method(:one)
      @multi   = MethodCall.new.method(:multi)
      @default = MethodCall.new.method(:default)
   end

   def test_call_basic
      assert_respond_to(@none, :call)
      assert_nothing_raised{ @none.call }
   end

   def test_brackets_alias_basic
      assert_respond_to(@none, :[])
      assert_nothing_raised{ @none[] }
   end

   def test_call_no_arguments
      assert_equal(nil, @none.call)
   end

   def test_brackets_alis_no_arguments
      assert_equal(nil, @none[])
   end

   def test_call_one_argument
      assert_equal(1, @one.call('foo'))
   end

   def test_brackets_alias_one_argument
      assert_equal(1, @one['foo'])
   end

   def test_call_splat_arguments
      assert_equal(1, @multi.call(1))
      assert_equal(5, @multi.call(1,1,2,1))
   end

   def test_brackets_alias_splat_arguments
      assert_equal(1, @multi[1])
      assert_equal(5, @multi[1,1,2,1])
   end

   def test_call_default_arguments
      assert_equal(nil, @default.call(1))
      assert_equal(7, @default.call(1,7))
      assert_equal(7, @default.call(1,7,2))
   end

   def test_brackets_alias_default_arguments
      assert_equal(nil, @default[1])
      assert_equal(7, @default[1,7])
      assert_equal(7, @default[1,7,2])
   end

   def test_call_expected_errors
      assert_raise(ArgumentError){ @none.call(1) }
      assert_raise(ArgumentError){ @one.call }
      assert_raise(ArgumentError){ @one.call(1,2) }
      assert_raise(ArgumentError){ @multi.call }
      assert_raise(ArgumentError){ @default.call }
   end

   def test_brackets_alias_expected_errors
      assert_raise(ArgumentError){ @none[1] }
      assert_raise(ArgumentError){ @one[] }
      assert_raise(ArgumentError){ @one[1,2] }
      assert_raise(ArgumentError){ @multi[] }
      assert_raise(ArgumentError){ @default[] }
   end

   def teardown
      @one   = nil
      @two   = nil
      @three = nil
      @four  = nil
   end
end
