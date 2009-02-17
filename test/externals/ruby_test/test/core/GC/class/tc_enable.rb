###############################################################################
# tc_enable.rb
#
# Test case for the GC.enable class method. Note that I do not include any
# of the GC tests in the 'test_core' task. Run the 'test_gc' task separately
# to see the results.
###############################################################################
require 'test/unit'

class TC_GC_Enable_ModuleMethod < Test::Unit::TestCase
   def test_enable_basic
      assert_respond_to(GC, :enable)
      assert_nothing_raised{ GC.enable }
   end

   def test_enable
#      assert_equal(true, GC.enable)
#      assert_equal(false, GC.enable)
   end

   def test_enable_expected_errors
      assert_raise(ArgumentError){ GC.enable(true) }
   end
end
