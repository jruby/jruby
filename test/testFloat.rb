require 'test/minirunit'
test_check "Test Floating points:"

# I had to choose a value for testing float equality
# 1.0e-11 maybe too big but since the result depends
# on the actual machine precision it may be too small
# too...
def test_float_equal(a,b)
  test_ok(a-b < 1.0e-11)
end

test_float_equal(9.2 , 3.5 + 5.7)
test_float_equal(9.9, 3.0*3.3)
test_float_equal(1.19047619 , 2.5 / 2.1)
test_float_equal(39.0625, 2.5 ** 4)

test_equal("1.1", 1.1.to_s)
test_equal("1.0", 1.0.to_s)
test_equal("-1.0", -1.0.to_s)

test_equal(nil, 0.0.infinite?)
test_equal(-1, (-1.0/0.0).infinite?)
test_equal(1, (+1.0/0.0).infinite?)

# a few added tests for MRI compatibility now that we override relops
# from Comparable

class Smallest
  include Comparable
  def <=> (other)
    -1
  end
end

class Different
  include Comparable
  def <=> (other)
    nil
  end
end

s = Smallest.new
d = Different.new
test_equal(nil, 3.0 <=> s)
test_equal(nil, 3.0 <=> d)
test_exception(ArgumentError) { 3.0 < s }
test_exception(ArgumentError) { 3.0 < d }

test_ok(Float.induced_from(1))
test_ok(Float.induced_from(100**50))
test_exception(TypeError){Float.induced_from('')}

test_equal(1.0.to_s=="1.0",true)

Inf = 1/0.0
MInf = -1/0.0
NaN = 0/0.0

test_equal(10-Inf,MInf)
test_equal(10+Inf,Inf)
test_equal(10/Inf,0.0)
test_equal(Inf.class,Float)
test_ok(Inf.infinite?)
test_ok(0.0.finite?)

test_equal(1.2 <=> 1.1,1)
test_equal(1.1 <=> 1.2,-1)
test_equal(1.2 <=> 1.2,0)

test_equal(1.0.floor.class,Fixnum)
test_equal(1.0.ceil.class,Fixnum)
test_equal(1.0.round.class,Fixnum)
test_equal(1.0.truncate.class,Fixnum)

test_equal(1.0.eql?(1),false)

test_equal((1.0+100**50).class,Float)
test_equal((1.0-100**50).class,Float)
test_equal((1.0/100**50).class,Float)
test_equal((1.0*100**50).class,Float)
test_equal((1.0%100**50).class,Float)

test_equal((1.0+100).class,Float)
test_equal((1.0-100).class,Float)
test_equal((1.0/100).class,Float)
test_equal((1.0*100).class,Float)
test_equal((1.0%100).class,Float)


test_equal(1.0.div(1).class,Fixnum)
test_equal(1.0.modulo(2.0).class,Float)

test_exception(FloatDomainError){1.0.divmod(0.0)}
test_equal(Inf.to_s=="Infinity",true)
test_equal((-Inf).to_s=="-Infinity",true)
test_equal(NaN.to_s=="NaN",true)
test_equal(NaN.to_s=="NaN",true)
test_equal(NaN==NaN,false)

test_exception(TypeError) {5.0.dup}


# JRUBY-1658
require 'rational'

test_equal(1.2, 1.0 + Rational(1, 5))
test_equal(0.1, 1.0 * (10 ** -1))
test_equal(0.8, 1.0 - Rational(1, 5))
test_equal(5.0, 1.0 / Rational(1, 5))
test_equal(4.0, 2.0 ** Rational(2))
test_equal(1.0, 5.0 % Rational(2))
test_equal([2,1.0], 5.0.divmod(Rational(2)))

test_equal(-1, 1.0 <=> Rational(2))
test_equal(1, 1.0 <=> Rational(1, 5))
test_equal(0, 1.0 <=> Rational(1))

test_equal(true, 1.0 == Rational(1))
test_equal(false, 0.0 == Rational(1))

test_equal(true, -2.0 > Rational(-3))
test_equal(true, 2.0 < Rational(3))
test_equal(true, 2.0 <= Rational(3))
test_equal(true, 2.0 <= Rational(2))
test_equal(true, -2.0 >= Rational(-3))
test_equal(true, -3.0 >= Rational(-3))

class Floatable
    def to_f
        2.0
    end
end

test_equal(2.0.coerce(Floatable.new), [2.0, 2.0])

# JRUBY-2568:
require 'bigdecimal'
test_equal(0.4, 2.0 / BigDecimal.new('5.0'))
