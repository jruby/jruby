######################################################################
# tc_getegid.rb
#
# Test case for the Process::Sys.getegid module method.
######################################################################
require 'test/unit'

class TC_ProcessSys_Getegid_ModuleMethod < Test::Unit::TestCase
   def setup
      @egid = Process.egid
   end

   def test_getegid_basic
      assert_respond_to(Process::Sys, :getegid)
      assert_nothing_raised{ Process::Sys.getegid }
   end

   def test_getegid
      assert_equal(@egid, Process::Sys.getegid)
   end

   def test_getegid_expected_errors
      assert_raises(ArgumentError){ Process::Sys.getegid(1) }
   end

   def teardown
      @egid = nil
   end
end
