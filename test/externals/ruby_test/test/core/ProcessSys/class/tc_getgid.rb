######################################################################
# tc_getgid.rb
#
# Test case for the Process::Sys.getgid module method.
######################################################################
require 'test/unit'

class TC_ProcessSys_Getgid_ModuleMethod < Test::Unit::TestCase
   def setup
      @gid = Process.gid
   end

   def test_getgid_basic
      assert_respond_to(Process::Sys, :getgid)
      assert_nothing_raised{ Process::Sys.getgid }
   end

   def test_getgid
      assert_equal(@gid, Process::Sys.getgid)
   end

   def test_getgid_expected_errors
      assert_raises(ArgumentError){ Process::Sys.getgid(1) }
   end

   def teardown
      @gid = nil
   end
end
