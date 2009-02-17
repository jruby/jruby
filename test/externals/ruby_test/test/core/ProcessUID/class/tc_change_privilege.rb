######################################################################
# tc_change_privilege.rb
#
# Test case for the Process::UID.change_privilege module method.
#
# NOTE: Most tests will only run with root privileges, and then only
# on Unix systems.
#
# TODO: figure out why this method always fails, even when run as
# the superuser.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessUID_ChangePrivilege_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      unless WINDOWS
         @nobody_uid = Etc.getpwnam('nobody').uid
         @login_uid  = Etc.getpwnam(Etc.getlogin).uid
      end
   end

   unless WINDOWS
      def test_change_privilege_basic
         assert_respond_to(Process::UID, :change_privilege)
      end

      # This method appears to be broken, as it always returns an Errno::EPERM
      #if ROOT
      #   def test_change_privilege
      #      assert_nothing_raised{ Process::UID.change_privilege(@nobody_uid) }
      #      assert_equal(@nobody_uid, Process::UID.eid)
      #      assert_nothing_raised{ Process::UID.change_privilege(@login_uid) }
      #      assert_equal(@login_uid, Process::UID.eid)
      #   end
      #end
   end

   def test_uid_expected_errors
      if WINDOWS
         assert_raises(TypeError, NotImplementedError){ Process::UID.change_privilege("x") }
      else
         assert_raises(TypeError){ Process::UID.change_privilege("x") }
      end
   end

   def teardown
      unless WINDOWS
         @nobody_uid = nil
         @login_uid  = nil
      end
   end
end
