######################################################################
# tc_geteuid.rb
#
# Test case for the Process::Sys.geteuid module method.
######################################################################
require 'test/unit'

class TC_ProcessSys_Geteuid_ModuleMethod < Test::Unit::TestCase
   def setup
      @euid = Process.euid
   end

   def test_geteuid_basic
      assert_respond_to(Process::Sys, :geteuid)
      assert_nothing_raised{ Process::Sys.geteuid }
   end

   def test_geteuid
      assert_equal(@euid, Process::Sys.geteuid)
   end

   def test_geteuid_expected_errors
      assert_raises(ArgumentError){ Process::Sys.geteuid(1) }
   end

   def teardown
      @euid = nil
   end
end
