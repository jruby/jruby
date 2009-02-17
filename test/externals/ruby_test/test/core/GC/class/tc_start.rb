###############################################################################
# tc_start.rb
#
# Test case for the GC.enable class method. Note that I do not include any
# of the GC tests in the 'test_core' task. Run the 'test_gc' task separately
# to see the results.
###############################################################################
require 'test/unit'

class TC_GC_Start_ModuleMethod < Test::Unit::TestCase
   def test_start_basic
      assert_respond_to(GC, :enable)
      assert_nothing_raised{ GC.enable }
   end

   def test_start
      assert_nil(GC.start)
   end

   # Should GC.start raise an error if GC has been disabled?
   #
   def test_start_with_gc_disabled
      assert_nothing_raised{ GC.disable }
      assert_nothing_raised{ GC.start }
   end

   def test_start_expected_errors
      assert_raises(ArgumentError){ GC.start(true) }
   end
end
