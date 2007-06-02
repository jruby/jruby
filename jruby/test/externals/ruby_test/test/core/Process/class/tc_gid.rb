######################################################################
# tc_gid.rb
#
# Test case for the Process.gid and Process.gid= module methods.
#
# NOTE: The Process.gid= tests are only run if the test is run as the
# root user.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Process_Gid_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @nobody_gid = Admin.get_group('nobody').gid
      @login_gid  = Admin.get_user(Admin.get_login).gid
   end

   def test_gid_basic
      assert_respond_to(Process, :gid)
      assert_respond_to(Process, :gid=)
   end

   def test_gid
      if ROOT
         assert_equal(0, Process.gid)
      else
         assert_equal(@login_gid, Process.gid)
      end
   end

   if ROOT
      def test_gid=
         assert_nothing_raised{ Process.gid = @nobody_gid }
         assert_equal(@nobody_gid, Process.gid)
         assert_nothing_raised{ Process.gid = @login_gid }
         assert_equal(@login_gid, Process.gid)
      end
   end

   def test_gid_expected_errors
      assert_raises(ArgumentError){ Process.gid(0) }
      assert_raises(TypeError){ Process.gid = "bogus" }
   end

   def teardown
      @nobody_gid = nil
      @login_gid  = nil
   end
end
