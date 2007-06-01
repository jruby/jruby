######################################################################
# tc_sid_available.rb
#
# Test case for the Process::UID.sid_available module method.
######################################################################
require 'test/unit'
require 'test/helper'

class TC_ProcessUID_SidAvailable_ModuleMethod < Test::Unit::TestCase
   include Test::Helper

   def test_sid_available_basic
      assert_respond_to(Process::UID, :sid_available?)
      assert_nothing_raised{ Process::UID.sid_available? }
   end

   def test_sid_available
      if WINDOWS
         assert_equal(false, Process::UID.sid_available?)
      else
         assert_equal(true, Process::UID.sid_available?)
      end
   end

   def test_sid_available_expected_failures
      assert_raises(ArgumentError){ Process::UID.sid_available?(1) }
      assert_raises(ArgumentError){ Process::UID.sid_available?(1, 2) }
   end
end
