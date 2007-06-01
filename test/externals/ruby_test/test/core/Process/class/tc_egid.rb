######################################################################
# tc_egid.rb
#
# Test case for the Process.egid and Process.egid= module methods.
# For now these tests are for UNIX platforms only.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Process_Egid_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @egid  = nil
      @group = Etc.getgrnam('nobody')
   end

   def test_egid_basic
      assert_respond_to(Process, :egid)
      assert_respond_to(Process, :egid=)
   end

   unless WINDOWS
      def test_egid
         assert_nothing_raised{ Process.egid }
         assert_kind_of(Fixnum, Process.egid)
      end

      # This test will only run if run as root
      if ROOT
         def test_egid_set
            assert_nothing_raised{ @egid = Process.egid }
            assert_nothing_raised{ Process.egid = @group.gid }
            assert_equal(@group.gid, Process.egid)
            assert_nothing_raised{ Process.egid = @egid }
            assert_equal(@egid, Process.egid)
         end
      end
   end

   def teardown
      @egid  = nil
      @group = nil
   end
end
