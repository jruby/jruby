######################################################################
# tc_grant_privilege.rb
#
# Test case for the Process::GID.grant_privilege module method, along
# with the Process::GID#eid= alias. 
#
# For now these tests are for Unix platforms only.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessGID_GrantPrivilege_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @eid   = nil
      @group = Etc.getgrnam('nobody')
   end

   # The eid= alias wasn't defined properly until 1.8.7 
   def test_grant_privilege_basic
      assert_respond_to(Process::GID, :grant_privilege)
      assert_respond_to(Process::GID, :eid=) if RELEASE >= 7
   end

   unless WINDOWS
      if ROOT
         def test_grant_privilege
            assert_nothing_raised{ @eid = Process::GID.eid }
            assert_nothing_raised{ Process::GID.grant_privilege(@group.gid) }
            assert_equal(@group.gid, Process::GID.eid)
            assert_nothing_raised{ Process::GID.grant_privilege(@eid) }
            assert_equal(@eid, Process::GID.eid)
         end
      end
   end

   def teardown
      @eid   = nil
      @group = nil
   end
end
