require 'test/minirunit'
test_check "Test bignums:"

# others tested in FixnumBignumAutoconversion test

Inf = 1/0.0
big = 12 ** 56
big2 = 341 ** 43
test_equal(big.class,Bignum)

test_exception(ArgumentError){big.to_s(-1)}
test_exception(ArgumentError){big.to_s(37)}
test_exception(TypeError){big.coerce(0.0)}

test_equal((100**50/2.0).class,Float)
test_equal((100**50+2.0).class,Float)
test_equal((100**50-2.0).class,Float)
test_equal((100**50*2.0).class,Float)
test_equal((100**50%2.0).class,Float)

test_exception(FloatDomainError){big.divmod(0.0)}
test_exception(ZeroDivisionError){big.divmod(0)}
test_exception(ZeroDivisionError){big.remainder(0)}
test_exception(ZeroDivisionError){big.modulo(0)}

test_equal(big2[0],1)
test_equal(big2[1],0)
test_equal(big[-1],0)
test_equal(-big2[0],-1)
test_equal(-big2[1],0)

# more to come
