##################################################
# tc_aset.rb
#
# Test suite for the Array#[]= instance method.
##################################################
require "test/unit"

class TC_Array_Aset_Instance < Test::Unit::TestCase
   def setup
      @empty = []
      @basic = [1,2,3,4,5]
   end
   
   def test_int
      assert_equal("foo", @empty[0] = "foo")
      assert_equal("bar", @empty[1] = "bar")
      assert_equal(nil, @empty[2] = nil)
      assert_equal(0, @empty[3] = 0)
      assert_equal(3, @empty[-1] = 3)
   end
   
   def teardown
      @empty = nil
      @basic = nil
   end
end