######################################################################
# tc_getuid.rb
#
# Test case for the Process::Sys.getuid module method.
######################################################################
require 'test/unit'

class TC_ProcessSys_Getuid_ModuleMethod < Test::Unit::TestCase
   def setup
      @uid = Process.uid
   end

   def test_getuid_basic
      assert_respond_to(Process::Sys, :getuid)
      assert_nothing_raised{ Process::Sys.getuid }
   end

   def test_getuid
      assert_equal(@uid, Process::Sys.getuid)
   end

   def test_getuid_expected_errors
      assert_raises(ArgumentError){ Process::Sys.getuid(1) }
   end

   def teardown
      @uid = nil
   end
end
