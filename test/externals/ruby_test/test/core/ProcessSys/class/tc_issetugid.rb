######################################################################
# tc_issetugid.rb
#
# Test case for the Process::Sys.issetugid module method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessSys_Issetugid_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def test_issetugid_basic
      assert_respond_to(Process::Sys, :issetugid)
   end

   # TODO: I need a way to test for cases where true is expected.
   unless WINDOWS
      def test_issetugid
         assert_equal(false, Process::Sys.issetugid)
      end
   end

   def test_issetugid_expected_errors
      if WINDOWS
         assert_raises(NotImplementedError){ Process::Sys.issetugid }
      else
         assert_raises(ArgumentError){ Process::Sys.issetugid(1) }
      end
   end
end
