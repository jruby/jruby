require 'test/unit'

class TestBignum < Test::Unit::TestCase
  # others tested in FixnumBignumAutoconversion test
  def setup
    @big = 12 ** 56
    @big2 = 341 ** 43
    # Inf = 1/0.0
  end
  
  def test_bignum_exceptions_behave_as_expected
    assert_raise(ArgumentError){@big.to_s(-1)}
    assert_raise(ArgumentError){@big.to_s(37)}
    assert_raise(TypeError){@big.coerce(0.0)}
    assert_raise(FloatDomainError){@big.divmod(0.0)}
    assert_raise(ZeroDivisionError){@big.divmod(0)}
    assert_raise(ZeroDivisionError){@big.remainder(0)}
    assert_raise(ZeroDivisionError){@big.modulo(0)}
  end
  
  def test_sufficiently_large_number_should_be_bignum
    assert(@big.class,Bignum)
  end
  
  def test_math_operations_on_bignum_with_float_should_produce_float
    assert((100**50/2.0).class,Float)
    assert((100**50+2.0).class,Float)
    assert((100**50-2.0).class,Float)
    assert((100**50*2.0).class,Float)
    assert((100**50%2.0).class,Float)
  end
  
  def test_bignum_should_respond_to_array_operator
    assert_equal(@big2[0],1)
    assert_equal(@big2[1],0)
    assert_equal(@big[-1],0)
    assert_equal(-(@big2)[0],-1)
    assert_equal(-(@big2)[1],0)
  end

  def test_bignum_aref_with_bignum_arg_no_exception
    assert_equal(0, (2**64)[2**32])
  end
  # more to come
  
end