###############################################################################
# tc_aset.rb
#
# Test suite for the Array#[]= instance method.
#
# Note that I find the rules for the second and third forms of Array#[]= to
# be incredibly byzantine in general.
###############################################################################
require 'test/unit'

class TC_Array_Aset_InstanceMethod < Test::Unit::TestCase
   def setup
      @empty = []
      @basic = [1, 2, 3, 4, 5]
   end

   def test_aset_basic
      assert_respond_to(@basic, :[]=)
      assert_nothing_raised{ @basic[0] = 1 }
      assert_kind_of(String, @basic[1] = 'a')
   end
   
   def test_aset_with_integer
      assert_nothing_raised{ @empty[0] = 'foo' }
      assert_nothing_raised{ @empty[1] = [1,2,3] }
      assert_nothing_raised{ @empty[2] = {1,2} }
      assert_nothing_raised{ @empty[3] = 0 }

      assert_equal('foo', @empty[0])
      assert_equal([1,2,3], @empty[1])
      assert_equal({1,2}, @empty[2])
      assert_equal(0, @empty[3])
   end

   def test_aset_with_float
      assert_nothing_raised{ @empty[0.5] = 'foo' }
      assert_nothing_raised{ @empty[1.1] = [1,2,3] }
      assert_nothing_raised{ @empty[2.9] = {1,2} }
      assert_nothing_raised{ @empty[3.5] = 0 }

      assert_equal('foo', @empty[0])
      assert_equal([1,2,3], @empty[1])
      assert_equal({1,2}, @empty[2])
      assert_equal(0, @empty[3])
   end

   def test_aset_with_start_and_length_array
      assert_nothing_raised{ @basic[0, 3] = ['a', 'b', 'c'] }
      assert_equal(['a', 'b', 'c', 4, 5], @basic)

      assert_nothing_raised{ @basic[0, 10] = ['x', 'y', 'z'] }
      assert_equal(['x', 'y', 'z'], @basic)

      assert_nothing_raised{ @basic[2, 2] = *[1, 2, 3] }
      assert_equal(['x', 'y', 1, 2, 3], @basic)

      assert_nothing_raised{ @basic[-1, 1] = ['d', 'e', 'f']  }
      assert_equal(['x', 'y', 1, 2, 'd', 'e', 'f'], @basic)
   end

   def test_aset_with_float_start_and_length_array
      assert_nothing_raised{ @basic[0.5, 3.1] = ['a', 'b', 'c'] }
      assert_equal(['a', 'b', 'c', 4, 5], @basic)

      assert_nothing_raised{ @basic[0.0, 10.9] = ['x', 'y', 'z'] }
      assert_equal(['x', 'y', 'z'], @basic)

      assert_nothing_raised{ @basic[2.2, 2.9] = *[1, 2, 3] }
      assert_equal(['x', 'y', 1, 2, 3], @basic)

      assert_nothing_raised{ @basic[-1.5, 1.4] = ['d', 'e', 'f']  }
      assert_equal(['x', 'y', 1, 2, 'd', 'e', 'f'], @basic)
   end

   def test_aset_with_start_and_length_non_array
      assert_nothing_raised{ @basic[0, 2] = 'a' }
      assert_equal(['a', 3, 4, 5], @basic)

      assert_nothing_raised{ @basic[1, 0] = 'x' }
      assert_equal(['a', 'x', 3, 4, 5], @basic)

      assert_nothing_raised{ @basic[7, 0] = 'foo' }
      assert_equal(['a', 'x', 3, 4, 5, nil, nil, 'foo'], @basic)

      assert_nothing_raised{ @basic[-2, 2] = 9 }
      assert_equal(['a', 'x', 3, 4, 5, nil, 9], @basic)
   end

   def test_aset_with_range
      assert_nothing_raised{ @basic[0..3] = ['a', 'b', 'c'] }
      assert_equal(['a', 'b', 'c', 5], @basic)

      assert_nothing_raised{ @basic[0..10] = ['x', 'y', 'z'] }
      assert_equal(['x', 'y', 'z'], @basic)

      assert_nothing_raised{ @basic[2..2] = *[1, 2, 3] }
      assert_equal(['x', 'y', 1, 2, 3], @basic)

      assert_nothing_raised{ @basic[-1..1] = ['d', 'e', 'f']  }
      assert_equal(['x', 'y', 1, 2, 'd', 'e', 'f', 3], @basic)
   end

   def test_aset_return_value
      assert_equal('foo', @empty[0] = 'foo')
      assert_equal('bar', @empty[1] = 'bar')
      assert_equal(nil, @empty[2] = nil)
      assert_equal(0, @empty[3] = 0)
      assert_equal(3, @empty[-1] = 3)
   end

   def test_aset_edge_cases
      assert_nothing_raised{ @basic[0, 10] = 8 }
      assert_equal([8], @basic)
      assert_nothing_raised{ @empty[0, 12] = [8] }
      assert_equal([8], @empty)
   end

   def test_aset_expected_errors
      assert_raise(IndexError){ @empty[-1, 0] = 10 }
      assert_raise(IndexError){ @basic[-6, 0] = 10 }
      assert_raise(TypeError){ @basic['a', 0] = 10 }
      assert_raise(TypeError){ @basic['1'.to_sym, 0] = 10 }
      assert_raise(TypeError){ @basic[0, '1'.to_sym] = 10 }
   end

   def teardown
      @empty = nil
      @basic = nil
   end
end
