######################################################################
# tc_re_exchange.rb
#
# Test case for the Process::UID.re_exchange module method. This test
# case is useful only if run as root, and only on Unix systems.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessUID_ReExchange_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      unless WINDOWS
         @uid  = Process.uid
         @euid = Process.euid
      end
   end

   def test_re_exchange_basic
      assert_respond_to(Process::UID, :re_exchange)
   end

   if ROOT && !WINDOWS
      def test_re_exchange
         assert_nothing_raised{ Process::UID.re_exchange }
         assert_equal(@uid, Process.euid)
         assert_equal(@euid, Process.uid)
      end

      def test_re_exchange_expected_failures
         assert_raises(ArgumentError){ Process::UID.re_exchange(1) }
         assert_raises(ArgumentError){ Process::UID.re_exchange(1, 2) }
      end
   end

   def teardown
      unless WINDOWS
         @uid  = nil
         @euid = nil
      end
   end
end
