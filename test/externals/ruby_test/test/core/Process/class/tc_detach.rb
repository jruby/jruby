######################################################################
# tc_detach.rb
#
# Test case for the Process.detach method. This test case is for
# UNIX platforms only.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Process_Detach_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def setup
      @pid = nil
   end

   if WINDOWS
      def test_stub
         # Stub to keep Test::Unit from whining about no tests
      end
   else
      def test_detach_basic
         assert_respond_to(Process, :detach)
      end

#      def test_detach
#         assert_nothing_raised{ @pid = fork }
#         assert_nothing_raised{ Process.detach(@pid) } if @pid
#      end
   end

   def teardown
      @pid = nil
   end
end
