######################################################################
# tc_re_exchange.rb
#
# Test case for the Process::GID.re_exchange module method. This test
# case is useful only if run as root, and only on Unix systems.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessGID_ReExchange_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      unless WINDOWS
         @gid  = Process.gid
         @egid = Process.egid
      end
   end

   def test_re_exchange_basic
      assert_respond_to(Process::GID, :re_exchange)
   end

   if ROOT && !WINDOWS
      def test_re_exchange
         assert_nothing_raised{ Process::GID.re_exchange }
         assert_equal(@gid, Process.egid)
         assert_equal(@egid, Process.gid)
      end

      def test_re_exchange_expected_failures
         assert_raises(ArgumentError){ Process::GID.re_exchange(1) }
         assert_raises(ArgumentError){ Process::GID.re_exchange(1, 2) }
      end
   end

   def teardown
      unless WINDOWS
         @gid  = nil
         @egid = nil
      end
   end
end
