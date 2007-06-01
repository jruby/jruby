######################################################################
# tc_eid.rb
#
# Test case for the Process::UID.eid method. The Process::UID.eid=
# method is part of the tc_grant_privilege test case because its an
# alias for the Process::UID.grant_privilege method.
#
# For now these tests are for Unix platforms only.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessUID_Eid_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @eid   = nil
      @group = Etc.getgrnam('nobody')
   end

   def test_eid_basic
      assert_respond_to(Process::UID, :eid)
   end

   unless WINDOWS
      def test_eid
         assert_nothing_raised{ Process::UID.eid }
         assert_kind_of(Fixnum, Process::UID.eid)
      end
   end

   def teardown
      @eid   = nil
      @group = nil
   end
end
