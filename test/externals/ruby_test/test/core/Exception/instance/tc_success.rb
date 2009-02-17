###############################################################################
# tc_success.rb
#
# Test case for the Exception#success? instance method. Actually, this method
# only exists on the SystemExit subclass.
###############################################################################
require 'test/unit'
require 'test/helper'

class TC_Exception_Success_InstanceMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @err = nil
      begin; exit(99); rescue SystemExit => @err; end
   end

   def test_success_basic
      assert_respond_to(@err, :success?)
      assert_nothing_raised{ @err.success? }
      assert_kind_of(Boolean, @err.success?)
   end

   # 0 or nil are success, everything else is not.
   def test_success
      assert_equal(false, @err.success?)
      begin; exit(0); rescue SystemExit => @err; end
      assert_equal(true, @err.success?)
      begin; rescue SystemExit => @err; end
      assert_equal(true, @err.success?)
   end
   
   # Only the SystemExit class implements this method
   def test_success_expected_errors
      assert_raise(ArgumentError){ @err.success?(99) }
      begin; 1/0; rescue Exception => @err; end
      assert_raise(NoMethodError){ @err.success? }
   end

   def teardown
      @err = nil
   end
end
