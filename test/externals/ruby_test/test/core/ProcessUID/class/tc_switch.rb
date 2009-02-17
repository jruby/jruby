######################################################################
# tc_switch.rb
#
# Test case for the Process::UID.switch module method. Most of these
# tests only run on Unix systems, and then only as root.
#
# Note: I am not convinced that this method actually works, and it
# has unusual (bad) behavior when the euid is the same as the uid.
#
# TODO: Update this as more information becomes available.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessUID_Switch_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @uid  = Process.uid
      @euid = Process.euid

      if !WINDOWS && ROOT && @uid == @euid
         Process.euid = Etc.getpwnam('nobody').uid
         @euid = Process.euid
      end
   end

   def test_switch_basic
      assert_respond_to(Process::UID, :switch)
   end

   if ROOT && !WINDOWS
      def test_switch
         assert_nothing_raised{ Process::UID.switch }
         #assert_equal(@euid, Process.uid)
         #assert_equal(@uid, Process.euid)
      end

      #def test_switch_with_block
      #end
   end

   def teardown
      @uid  = nil
      @euid = nil
   end
end
