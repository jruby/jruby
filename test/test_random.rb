require 'test/unit'

class TestRand < Test::Unit::TestCase
  
  def test_srand
    srand(123)
    a = [rand, rand, rand]
    srand(123)
    assert_equal(a, [rand, rand, rand])
  end
  
  def test_return_float_for_no_arg 
    r = rand
    assert(r.kind_of?(Float)) 
    assert(r < 1.0)
    assert(r >= 0.0)
  end

  def test_return_float_for_zero_arg
    r = rand(0)
    assert(r.kind_of?(Float)) 
    assert(r < 1.0)
    assert(r >= 0.0)
  end
  
  def test_one_arg
    100.times {
      r = rand(1)
      assert_equal(0, r)
    }
  end
  
  def test_rand_int_in_range
    do_range_test(2**31 - 1)
  end

  def test_rand_long_in_range
    do_range_test(2**31)
    do_range_test(2**64 - 1)
  end
  
  def test_rand_bignum_in_range
    do_range_test(2**64)
    do_range_test(2**100)
  end
  
  def test_negative_arg
    do_abs_value_arg_test(0)
    do_abs_value_arg_test(1)
    do_abs_value_arg_test(2**31 - 1)
    do_abs_value_arg_test(2**31)
    do_abs_value_arg_test(2**64 - 1)
    do_abs_value_arg_test(2**64)
    do_abs_value_arg_test(2**100)
  end

  def do_range_test(max)
    100.times {
      r = rand(max)
      assert(r < max)
      assert(r >= 0)
    }
  end
  
  def do_abs_value_arg_test(max)
    srand(42)
    r1 = rand(max) 
    srand(42)
    r2 = rand(-max)
    assert(r1 == r2)
  end
end
