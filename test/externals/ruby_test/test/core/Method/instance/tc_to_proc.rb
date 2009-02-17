########################################################################
# tc_to_proc.rb
#
# Test case for the Method#to_proc instance method.
########################################################################
require 'test/unit'

class MethodProc
   def test_method(arg1)
      arg1
   end
end

class TC_Method_ToProc_InstanceMethod < Test::Unit::TestCase
   def setup
      @method = MethodProc.new.method(:test_method)
      @proc   = nil
   end

   def test_to_proc_basic
      assert_respond_to(@method, :to_proc)
      assert_nothing_raised{ @method.to_proc }
   end

   def test_to_proc_is_proc
      assert_nothing_raised{ @proc = @method.to_proc }
      assert_kind_of(Proc, @proc)
      assert_respond_to(@proc, :[])
      assert_respond_to(@proc, :==)
      assert_respond_to(@proc, :arity)
      assert_respond_to(@proc, :binding)
      assert_respond_to(@proc, :call)
      assert_respond_to(@proc, :to_proc)
      assert_respond_to(@proc, :to_s)
   end

   def test_to_proc_call
      assert_nothing_raised{ @proc = @method.to_proc }
      assert_equal(5, @proc.call(5))
   end

   def test_to_proc_expected_errors
      assert_raise(ArgumentError){ @method.to_proc(true) }
   end

   def teardown
      @method     = nil
      @proc    = nil
   end
end
