######################################################################
# tc_pid.rb
#
# Test case for the Process.pid module method. It also tests the $$
# special variable.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_Process_Pid_ModuleMethod < Test::Unit::TestCase
   def test_pid
      assert_respond_to(Process, :pid)
      assert_kind_of(Fixnum, Process.pid)
   end

   def test_pid_global
      assert_kind_of(Fixnum, $$)
      assert_equal(Process.pid, $$)
   end
end
