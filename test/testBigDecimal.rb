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

