###############################################################################
# tc_garbage_collect.rb
#
# Test case for the GC#garbage_collect instance method. 
###############################################################################
require 'test/unit'

class TC_GC_GarbageCollect_InstanceMethod < Test::Unit::TestCase
   include GC

   def test_garbage_collect
      assert_respond_to(self, :garbage_collect)
      assert_nothing_raised{ self.garbage_collect }
      assert_nil(self.garbage_collect)
   end
end
