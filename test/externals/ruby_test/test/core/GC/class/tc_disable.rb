###############################################################################
# tc_disable.rb
#
# Test case for the GC.disable class method. Note that I do not include any
# of the GC tests in the 'test_core' task. Run the 'test_gc' task separately
# to see the results.
###############################################################################
require 'test/unit'

class TC_GC_Disable_ModuleMethod < Test::Unit::TestCase
   def test_disable_basic
      assert_respond_to(GC, :disable)
      assert_nothing_raised{ GC.disable }
   end

   def test_disable
#      assert_equal(false, GC.disable)
#      assert_equal(true, GC.disable)
   end

   def test_disable_expected_errors
      assert_raise(ArgumentError){ GC.disable(true) }
   end
end
