######################################################################
# tc_re_exchangeable.rb
#
# Test case for the Process::GID.re_exchangeable module method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessGID_ReExchangeable_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def test_re_exchangeable_basic
      assert_respond_to(Process::GID, :re_exchangeable?)
      assert_nothing_raised{ Process::GID.re_exchangeable? }
   end

   def test_re_exchangeable
      if WINDOWS
         assert_equal(false, Process::GID.re_exchangeable?)
      else
         assert_equal(true, Process::GID.re_exchangeable?)
      end
   end

   def test_re_exchangeable_expected_failures
      assert_raises(ArgumentError){ Process::GID.re_exchangeable?(1) }
      assert_raises(ArgumentError){ Process::GID.re_exchangeable?(1, 2) }
   end
end
