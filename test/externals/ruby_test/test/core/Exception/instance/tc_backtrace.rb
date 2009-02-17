###############################################################################
# tc_backtrace.rb
#
# Test case for the Exception#backtrace instance method.
###############################################################################
require 'test/unit'

class TC_Exception_Backtrace_InstanceMethod < Test::Unit::TestCase
   def setup
      @exception = RuntimeError.new
   end

   def test_backtrace_basic
      assert_respond_to(@exception, :backtrace)
      assert_nothing_raised{ @exception.backtrace }
      assert_nil(@exception.backtrace)
   end

   def test_backtrace
      begin; 1/0; rescue Exception => @exception; end 
      assert_not_nil(@exception.backtrace)
      assert_kind_of(Array, @exception.backtrace)
   end

   def test_backtrace_format
      begin; 1/0; rescue Exception => @exception; end 
      assert_not_nil(@exception.backtrace.first =~ /.*?tc_backtrace\.rb:\d+:in.+/)
   end

   def test_backtrace_expected_errors
      assert_raise(ArgumentError){ @exception.backtrace(true) }
   end

   def teardown
      @exception = nil
   end
end
