######################################################################
# tc_euid.rb
#
# Test case for the Process.euid and Process.euid= module methods.
# For now these tests are for UNIX platforms only.
######################################################################
require 'test/unit'
require 'test/helper'
require 'etc' rescue nil

class TC_Process_Euid_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @euid = nil
      @user = Etc.getpwnam('nobody')
   end

   def test_euid_basic
      assert_respond_to(Process, :euid)
      assert_respond_to(Process, :euid=)
   end

   unless WINDOWS
      def test_euid
         assert_nothing_raised{ Process.euid }
         assert_kind_of(Fixnum, Process.euid)
      end

      # This test will only run if run as root
      if Process.euid == 0
         def test_euid_set
            assert_nothing_raised{ @euid = Process.euid }
            assert_nothing_raised{ Process.euid = @user.gid }
            assert_equal(@user.gid, Process.euid)
            assert_nothing_raised{ Process.euid = @euid }
            assert_equal(@euid, Process.euid)
         end
      end
   end

   def teardown
      @euid = nil
      @user = nil
   end
end
