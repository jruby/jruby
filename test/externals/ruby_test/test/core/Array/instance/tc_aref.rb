#############################################
# tc_aref.rb
#
# Test suite for Array#[]
#############################################
require "test/unit"

class TC_Array_Aref_Instance < Test::Unit::TestCase
   def setup
      @empty = []
      @basic = [1,2,3]
      @multi = [1, "foo", /^$/]
   end
   
   def test_int
      assert_nil(@empty[0])
      assert_nil(@empty[-1])
      assert_nil(@empty[1])
      
      assert_equal(1, @basic[0])
      assert_equal(3, @basic[2])
      assert_equal(3, @basic[-1])
      assert_nil(@basic[-5])
      assert_nil(@basic[99])
   end
   
   def test_expected_errors
      assert_raises(TypeError){ @empty[nil] }
      assert_raises(TypeError){ @empty["foo"] }
      assert_raises(TypeError){ @basic["1".to_sym, "2".to_sym] }
      assert_raises(TypeError){ @basic["1".to_sym, "2"] }
      assert_raises(TypeError){ @basic["1".to_sym, 2] }
      assert_raises(TypeError){ @basic["1".to_sym] }
      assert_raises(ArgumentError){ @basic[] }
   end
   
   def test_length
      assert_equal(["foo", /^$/], @multi[1,2])
      assert_equal([1, "foo"], @multi[0,2])
      assert_equal([1, "foo"], @multi[-3,2])     
      assert_nil(@multi[-5,2])
   end
   
   # There is some inconsistent behavior with negative indexes that are out
   # of range.
   def test_range
      assert_equal([], @empty[0..1])
      assert_nil(@empty[-2..-1]) # ???
      assert_nil(@empty[-1..-2]) # ???
   
      assert_equal([1,2], @basic[0..1])
      assert_equal([1,2,3], @basic[0..2])
      assert_equal([1,2,3], @basic[0..3])
      assert_equal([1,2,3], @basic[-3..-1])
   end
   
   def teardown
      @empty = nil
      @basic = nil
      @multi = nil
   end
end
