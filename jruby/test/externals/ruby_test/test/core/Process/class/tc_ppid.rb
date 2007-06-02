######################################################################
# tc_ppid.rb
#
# Test case for the Process.ppid module method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Process_Ppid_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   # The test for MS Windows is the current behavior, but it could
   # be made to return a legitimate value.
   #
   def test_ppid
      assert_respond_to(Process, :ppid)
      assert_kind_of(Fixnum, Process.ppid)
      assert_equal(0, Process.ppid) if WINDOWS
   end
end
