######################################################################
# tc_grant_privilege.rb
#
# Test case for the Process::UID.grant_privilege module method, along
# with the Process::UID#eid= alias. 
#
# For now these tests are for Unix platforms only.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessUID_GrantPrivilege_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @eid   = nil
      @group = Etc.getgrnam('nobody')
   end

   # The eid= alias wasn't defined properly until 1.8.7 
   def test_grant_privilege_basic
      assert_respond_to(Process::UID, :grant_privilege)
      assert_respond_to(Process::UID, :eid=) if RELEASE >= 7
   end

   unless WINDOWS
      if ROOT
         def test_grant_privilege
            assert_nothing_raised{ @eid = Process::UID.eid }
            assert_nothing_raised{ Process::UID.grant_privilege(@group.gid) }
            assert_equal(@group.gid, Process::UID.eid)
            assert_nothing_raised{ Process::UID.grant_privilege(@eid) }
            assert_equal(@eid, Process::UID.eid)
         end
      end
   end

   def teardown
      @eid   = nil
      @group = nil
   end
end
