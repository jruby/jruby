######################################################################
# tc_setegid.rb
#
# Test case for the Process::Sys.setegid module method.
#
# Most of these tests will only run on Unix systems, and then only
# as root.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessSys_Setegid_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   unless WINDOWS
      def setup
         @nobody_gid = Etc.getgrnam('nobody').gid
         @login_gid  = Etc.getpwnam(Etc.getlogin).gid
      end
   end

   def test_setegid_basic
      assert_respond_to(Process::Sys, :setegid)
   end

   if ROOT && !WINDOWS
      def test_setegid
         assert_nothing_raised{ Process::Sys.setegid(@nobody_gid) }
         assert_equal(@nobody_gid, Process.egid)
         assert_nothing_raised{ Process::Sys.setegid(@login_gid) }
         assert_equal(@login_gid, Process.egid)
      end
   end

   def test_gid_expected_errors     
      if WINDOWS
         assert_raises(NotImplementedError){ Process::Sys.setegid('bogus') }
      else
         assert_raises(TypeError){ Process::Sys.setegid('bogus') }
      end
   end

   unless WINDOWS
      def teardown
         @nobody_gid = nil
         @login_gid  = nil
      end
   end
end
