######################################################################
# tc_times.rb
#
# Test case for the Process.times module method.
######################################################################
require 'test/unit'

class TC_Process_Times_ModuleMethod < Test::Unit::TestCase
   def test_times
      assert_respond_to(Process, :times)
      assert_kind_of(Struct::Tms, Process.times)
   end

   def test_times_expected_errors
      assert_raises(ArgumentError){ Process.times(0) }
   end
end
