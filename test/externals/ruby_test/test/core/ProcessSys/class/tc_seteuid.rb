######################################################################
# tc_seteuid.rb
#
# Test case for the Process::Sys.seteuid module method.
#
# Most of these tests will only run on Unix systems, and then only
# as root.
#
# TODO: Figure out why these tests always fail.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessSys_Seteuid_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   unless WINDOWS
      def setup
         @nobody_uid = Etc.getpwnam('nobody').uid
         @login_uid  = Etc.getpwnam(Etc.getlogin).uid
      end
   end

   def test_seteuid_basic
      assert_respond_to(Process::Sys, :seteuid)
   end

   #if ROOT
   #   def test_seteuid
   #      assert_nothing_raised{ Process::Sys.seteuid(@nobody_uid) }
   #      assert_equal(@nobody_uid, Process.euid)
   #      assert_nothing_raised{ Process::Sys.seteuid(@login_uid) }
   #      assert_equal(@login_uid, Process.euid)
   #   end
   #end

   def test_uid_expected_errors
      if WINDOWS       
         assert_raises(NotImplementedError){ Process::Sys.seteuid(1) }
      else
         assert_raises(TypeError){ Process::Sys.seteuid('bogus') }
      end
   end

   unless WINDOWS
      def teardown
         @nobody_uid = nil
         @login_uid  = nil
      end
   end
end
