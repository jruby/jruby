require 'test/unit'

class TC_Exception_Status_InstanceMethod < Test::Unit::TestCase
   def setup
      @err = nil
      begin; exit(99); rescue SystemExit => @err; end
   end

   def test_status_basic
      assert_respond_to(@err, :status)
      assert_nothing_raised{ @err.status }
      assert_kind_of(Fixnum, @err.status)
   end

   def test_status
      assert_equal(99, @err.status)
      begin; exit(-1); rescue SystemExit => @err; end
      assert_equal(-1, @err.status)
   end
   
   # Only the SystemExit class implements this method
   def test_status_expected_errors
      assert_raise(ArgumentError){ @err.status(99) }
      begin; 1/0; rescue Exception => @err; end
      assert_raise(NoMethodError){ @err.status }
   end

   def teardown
      @err = nil
   end
end