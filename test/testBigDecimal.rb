require 'test/minirunit'
test_check "Test BigDecimal"

require 'bigdecimal'

# no singleton methods on bigdecimal
num = BigDecimal.new("0.001")
test_exception(TypeError) { class << num ; def amethod ; end ; end }
test_exception(TypeError) { def num.amethod ; end }

test_ok BigDecimal.new("4")
test_ok BigDecimal.new("3.14159")

# JRUBY-153 issues
# Implicit new
test_ok BigDecimal("4")
test_ok BigDecimal("3.14159")

# Reject arguments not responding to #to_str
test_exception(TypeError) { BigDecimal.new(4) }
test_exception(TypeError) { BigDecimal.new(3.14159) }
test_exception(TypeError) { BigDecimal(4) }
test_exception(TypeError) { BigDecimal(3.14159) }

# Accept valid arguments to #mode
test_ok BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW)
test_ok BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW,true)
test_ok BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW,false)

# Reject invalid arguments to #mode
test_exception(TypeError) { BigDecimal.mode(true) } # first argument must be a Fixnum
test_exception(TypeError) { BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW, 1) } # second argument must be [true|false]
test_exception(TypeError) { BigDecimal.mode(512) } # first argument must be == 256, or return non-zero when AND-ed with 255

# exception mode defaults to 0
test_equal 0, BigDecimal.mode(1) # value of first argument doesn't matter when retrieving the current exception mode, as long as it's a Fixnum <= 255

# set and clear a single exception mode
test_equal BigDecimal::EXCEPTION_INFINITY, BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, true)
test_equal 0, BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, false)
test_equal BigDecimal::EXCEPTION_NaN, BigDecimal.mode(BigDecimal::EXCEPTION_NaN, true)
test_equal 0, BigDecimal.mode(BigDecimal::EXCEPTION_NaN, false)

# set a composition of exception modes separately, make sure the final result is the composited value
BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, true)
BigDecimal.mode(BigDecimal::EXCEPTION_NaN, true)
test_equal BigDecimal::EXCEPTION_INFINITY | BigDecimal::EXCEPTION_NaN, BigDecimal.mode(1)

# reset the exception mode to 0 for the following tests
BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, false)
BigDecimal.mode(BigDecimal::EXCEPTION_NaN, false)

# set a composition of exception modes with one call and retrieve it using the retrieval idiom
# note: this is to check compatibility with MRI, which currently sets only the last mode
# it checks for
BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY | BigDecimal::EXCEPTION_NaN, true)
test_equal BigDecimal::EXCEPTION_NaN, BigDecimal.mode(1)

# rounding mode defaults to 0
test_equal 0, BigDecimal.mode(BigDecimal::ROUND_MODE)

# make sure each setting complete replaces any previous setting
[BigDecimal::ROUND_UP, BigDecimal::ROUND_DOWN, BigDecimal::ROUND_CEILING, BigDecimal::ROUND_FLOOR,
 BigDecimal::ROUND_HALF_UP, BigDecimal::ROUND_HALF_DOWN, BigDecimal::ROUND_HALF_EVEN].each do |mode|
	test_equal mode, BigDecimal.mode(BigDecimal::ROUND_MODE, mode)
end

# reset rounding mode to 0 for following tests
BigDecimal.mode(BigDecimal::ROUND_MODE, 0)

test_exception(TypeError) { BigDecimal.mode(BigDecimal::ROUND_MODE, true) } # second argument must be a Fixnum
test_exception(TypeError) { BigDecimal.mode(BigDecimal::ROUND_MODE, 7) } # any Fixnum >= 7 should trigger this error, as the valid rounding modes are currently [0..6]

test_equal BigDecimal("0.0"), BigDecimal("XXX")

class X
  def to_str; "3.14159" end
end

x = X.new
test_ok BigDecimal.new(x)
test_ok BigDecimal(x)

require "bigdecimal/newton"
include Newton

class Function
  def initialize()
    @zero = BigDecimal::new("0.0")
    @one  = BigDecimal::new("1.0")
    @two  = BigDecimal::new("2.0")
    @ten  = BigDecimal::new("10.0")
    @eps  = BigDecimal::new("1.0e-16")
  end
  def zero;@zero;end
  def one ;@one ;end
  def two ;@two ;end
  def ten ;@ten ;end
  def eps ;@eps ;end
  def values(x) # <= defines functions solved
    f = []
    f1 = x[0]*x[0] + x[1]*x[1] - @two # f1 = x**2 + y**2 - 2 => 0
    f2 = x[0] - x[1]                  # f2 = x    - y        => 0
    f <<= f1
    f <<= f2
    f
  end
end
f = BigDecimal::limit(100)
f = Function.new
x = [f.zero,f.zero]      # Initial values
n = nlsolve(f,x)
expected = [BigDecimal('0.1000000000262923315461642086010446338567975310185638386446002778855192224707966221794469725479649528E1'),
            BigDecimal('0.1000000000262923315461642086010446338567975310185638386446002778855192224707966221794469725479649528E1')]
test_equal expected, x

require "bigdecimal/math.rb"
include BigMath
expected = BigDecimal('0.3141592653589793238462643383279502884197169399375105820974944592307816406286208998628034825342117067E1')
test_equal expected, PI(1000)

one = BigDecimal("1")

test_equal one * 1, one
test_equal one / 1, one
test_equal one + 1, BigDecimal("2")
test_equal one - 1, BigDecimal("0")

test_equal 1*one, one
test_equal 1/one, one
test_equal 1+one, BigDecimal("2")
test_equal 1-one, BigDecimal("0")

test_equal one * 1.0, 1.0
test_equal one / 1.0, 1.0
test_equal one + 1.0, 2.0
test_equal one - 1.0, 0.0

test_equal 1.0*one, 1.0
test_equal 1.0/one, 1.0
test_equal 1.0+one, 2.0
test_equal 1.0-one, 0.0

test_equal("1.0", BigDecimal.new('1.0').to_s('F'))
test_equal("0.0", BigDecimal.new('0.0').to_s)

test_equal(BigDecimal("2"), BigDecimal("1.5").round)
test_equal(BigDecimal("15"), BigDecimal("15").round)
test_equal(BigDecimal("20"), BigDecimal("15").round(-1))
test_equal(BigDecimal("0"), BigDecimal("15").round(-2))
test_equal(BigDecimal("-10"), BigDecimal("-15").round(-1, BigDecimal::ROUND_CEILING))
test_equal(BigDecimal("10"), BigDecimal("15").round(-1, BigDecimal::ROUND_HALF_DOWN))
test_equal(BigDecimal("20"), BigDecimal("25").round(-1, BigDecimal::ROUND_HALF_EVEN))
test_equal(BigDecimal("15.99"), BigDecimal("15.993").round(2))

test_equal(BigDecimal("1"), BigDecimal("1.8").round(0, BigDecimal::ROUND_DOWN))
test_equal(BigDecimal("2"), BigDecimal("1.2").round(0, BigDecimal::ROUND_UP))
test_equal(BigDecimal("-1"), BigDecimal("-1.5").round(0, BigDecimal::ROUND_CEILING))
test_equal(BigDecimal("-2"), BigDecimal("-1.5").round(0, BigDecimal::ROUND_FLOOR))
test_equal(BigDecimal("-2"), BigDecimal("-1.5").round(0, BigDecimal::ROUND_FLOOR))
test_equal(BigDecimal("1"), BigDecimal("1.5").round(0, BigDecimal::ROUND_HALF_DOWN))
test_equal(BigDecimal("2"), BigDecimal("1.5").round(0, BigDecimal::ROUND_HALF_EVEN))
test_equal(BigDecimal("2"), BigDecimal("2.5").round(0, BigDecimal::ROUND_HALF_EVEN))

# test BigDecimal.power()
n = BigDecimal("10")
test_equal(n.power(0), BigDecimal("1"))
test_equal(n.power(1), n)
test_equal(n.power(2), BigDecimal("100"))
test_equal(n.power(-1), BigDecimal("0.1"))
test_exception(TypeError) { n.power(1.1) }
