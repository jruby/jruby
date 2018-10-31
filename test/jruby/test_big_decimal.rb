require 'test/unit'
require 'bigdecimal'

class TestBigDecimal < Test::Unit::TestCase

  def test_to_s
    assert_equal("0.0", BigDecimal('0.0').to_s)
    assert_equal("0.11111111111e0", BigDecimal('0.11111111111').to_s)
    assert_equal("0.0", BigDecimal('0').to_s)
    assert_equal("0.1e-9", BigDecimal("1.0e-10").to_s)
  end

  def test_to_java
    # assert_equal java.lang.Long, 1000.to_java.class

    assert_equal java.math.BigDecimal, BigDecimal('1000.8').to_java.class
    assert_equal java.lang.Integer, BigDecimal('0.0').to_java(:int).class

    number = java.lang.Number
    assert_equal java.math.BigDecimal.new('8.0111'), BigDecimal('8.0111').to_java(number)
    assert_equal java.lang.Long, 1000.to_java(number).class
  end

  def test_no_singleton_methods_on_bigdecimal
    num = BigDecimal("0.001")
    assert_raise(TypeError) { class << num ; def amethod ; end ; end }
    assert_raise(TypeError) { def num.amethod ; end }
  end

  def test_can_instantiate_big_decimal
    assert_nothing_raised { BigDecimal("4") }
    assert_nothing_raised { BigDecimal("3.14159") }
    BigDecimal.new("1")
  end

  class X
    def to_str; "3.14159" end
  end

  def test_can_accept_arbitrary_objects_as_arguments
    # as log as the object has a #to_str method...
    x = X.new
    assert_nothing_raised { BigDecimal.new(x) }
    assert_nothing_raised { BigDecimal(x) }
  end

  def test_cmp
    begin
      BigDecimal('10') < "foo"
    rescue ArgumentError => e
      assert_equal 'comparison of BigDecimal with String failed', e.message
    else
      fail 'expected cmp to fail'
    end

    begin
      BigDecimal('10') >= nil
    rescue ArgumentError => e
      assert_equal 'comparison of BigDecimal with nil failed', e.message
    else
      fail 'expected cmp to fail'
    end
  end

  class MyNum
    def *(other)
      33
    end

    def /(other)
      99
    end

    def coerce(other)
      [MyNum.new, self]
    end
  end

  def test_coerce_div_mul
    require 'bigdecimal/util'

    assert_equal 33, BigDecimal(10) * MyNum.new
    assert_equal 99, 10.0 / MyNum.new
    assert_equal 99, 10.0.to_d / MyNum.new
  end

  def test_coerce_subtraction
    coercible = Object.new; def coercible.coerce(x); [x, BigDecimal("0")]; end
    assert_equal(1, BigDecimal("1") - coercible)
  end

  require "bigdecimal/newton"
  include Newton

  class Function
    def initialize()
      @zero = BigDecimal("0.0")
      @one  = BigDecimal("1.0")
      @two  = BigDecimal("2.0")
      @ten  = BigDecimal("10.0")
      @eps  = BigDecimal("1.0e-16")
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

  def test_newton_extension
    f = BigDecimal::limit(100)
    f = Function.new
    x = [f.zero,f.zero]      # Initial values
    n = nlsolve(f,x)
    expected = [BigDecimal('0.1000000000262923315461642086010446338567975310185638386446002778855192224707966221794469725479649528E1'),
                BigDecimal('0.1000000000262923315461642086010446338567975310185638386446002778855192224707966221794469725479649528E1')]
    assert_equal expected, x
  end

  require "bigdecimal/math.rb"
  include BigMath

  def test_math_extension
    expected = BigDecimal('0.31415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679821480865132823028638630883606928E1')
    # this test fails under C Ruby
    # ruby 1.8.6 (2007-03-13 patchlevel 0) [i686-darwin8.9.1]
    assert_equal expected, PI(100)

    zero= BigDecimal("0")
    one = BigDecimal("1")
    two = BigDecimal("2")
    three = BigDecimal("3")

    assert_equal one * 1, one
    assert_equal one / 1, one
    assert_equal one + 1, two
    assert_equal one - 1, zero

    assert_equal zero, one % 1
    assert_equal one, three % two
    assert_equal BigDecimal("0.2"), BigDecimal("2.2") % two
    assert_equal BigDecimal("0.003"), BigDecimal("15.993") % BigDecimal("15.99")

    assert_equal 1*one, one
    assert_equal 1/one, one
    assert_equal 1+one, BigDecimal("2")
    assert_equal 1-one, BigDecimal("0")

    assert_equal one * 1.0, 1.0
    assert_equal one / 1.0, 1.0
    assert_equal one + 1.0, 2.0
    assert_equal one - 1.0, 0.0

    assert_equal 1.0*one, 1.0
    assert_equal 1.0/one, 1.0
    assert_equal 1.0+one, 2.0
    assert_equal 1.0-one, 0.0

    assert_equal("1.0", BigDecimal('1.0').to_s('F'))
    assert_equal("0.0", BigDecimal('0.0').to_s)

    assert_equal(BigDecimal("2"), BigDecimal("1.5").round)
    assert_equal(BigDecimal("15"), BigDecimal("15").round)
    assert_equal(BigDecimal("20"), BigDecimal("15").round(-1))
    assert_equal(BigDecimal("0"), BigDecimal("15").round(-2))
    assert_equal(BigDecimal("-10"), BigDecimal("-15").round(-1, BigDecimal::ROUND_CEILING))
    assert_equal(BigDecimal("10"), BigDecimal("15").round(-1, BigDecimal::ROUND_HALF_DOWN))
    assert_equal(BigDecimal("20"), BigDecimal("25").round(-1, BigDecimal::ROUND_HALF_EVEN))
    assert_equal(BigDecimal("15.99"), BigDecimal("15.993").round(2))

    assert_equal(BigDecimal("1"), BigDecimal("1.8").round(0, BigDecimal::ROUND_DOWN))
    assert_equal(BigDecimal("2"), BigDecimal("1.2").round(0, BigDecimal::ROUND_UP))
    assert_equal(BigDecimal("-1"), BigDecimal("-1.5").round(0, BigDecimal::ROUND_CEILING))
    assert_equal(BigDecimal("-2"), BigDecimal("-1.5").round(0, BigDecimal::ROUND_FLOOR))
    assert_equal(BigDecimal("-2"), BigDecimal("-1.5").round(0, BigDecimal::ROUND_FLOOR))
    assert_equal(BigDecimal("1"), BigDecimal("1.5").round(0, BigDecimal::ROUND_HALF_DOWN))
    assert_equal(BigDecimal("2"), BigDecimal("1.5").round(0, BigDecimal::ROUND_HALF_EVEN))
    assert_equal(BigDecimal("2"), BigDecimal("2.5").round(0, BigDecimal::ROUND_HALF_EVEN))
  end

  def test_round_nan
    nan = BigDecimal('NaN')
    assert nan.round(0).nan?
    assert nan.round(2).nan?
  end

  def test_big_decimal_power
    require 'bigdecimal/math'

    n = BigDecimal("10")
    assert_equal(n.power(0), BigDecimal("1"))
    assert_equal(n.power(1), n)
    assert_equal(n.power(2), BigDecimal("100"))
    assert_equal(n.power(-1), BigDecimal("0.1"))

    n.power(1.1)

    begin
      n.power('1.1')
    rescue TypeError => e
      assert_equal 'wrong argument type String (expected scalar Numeric)', e.message
    else
      fail 'expected to raise TypeError'
    end

    assert_equal BigDecimal('0.1E2'), n.power(1.0)

    res = n.power(1.1)
    #assert_equal BigDecimal('0.125892541E2'), res
    # NOTE: we're not handling precision the same as MRI with pow
    assert_equal '0.125892541', res.to_s[0..10]
    assert_equal 'e2', res.to_s[-2..-1]

    res = 2 ** BigDecimal(1.2, 2)
    #assert_equal BigDecimal('0.229739671E1'), res
    # NOTE: we're not handling precision the same as MRI with pow
    assert_equal '0.22973967', res.to_s[0..9]
    assert_equal 'e1', res.to_s[-2..-1]

    res = BigDecimal(1.2, 2) ** 2.0
    assert_equal BigDecimal('0.144E1'), res

  end

  def teardown
    BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW, false) rescue nil
    BigDecimal.mode(BigDecimal::EXCEPTION_NaN, false) rescue nil
    BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, false) rescue nil
  end

  def test_big_decimal_mode
    # Accept valid arguments to #mode
    assert BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW)
    assert BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW, true)
    assert BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW, false)

    # Reject invalid arguments to #mode
    assert_raises(TypeError) { BigDecimal.mode(true) } # first argument must be a Fixnum
    assert_raises(ArgumentError) { BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW, 1) } # second argument must be [true|false]
    assert_raises(TypeError) { BigDecimal.mode(512) } # first argument must be == 256, or return non-zero when AND-ed with 255

    # exception mode defaults to 0
    assert_equal 0, BigDecimal.mode(1) # value of first argument doesn't matter when retrieving the current exception mode, as long as it's a Fixnum <= 255

    # set and clear a single exception mode
    assert_equal BigDecimal::EXCEPTION_INFINITY, BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, true)
    assert_equal 0, BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, false)
    assert_equal BigDecimal::EXCEPTION_NaN, BigDecimal.mode(BigDecimal::EXCEPTION_NaN, true)
    assert_equal 0, BigDecimal.mode(BigDecimal::EXCEPTION_NaN, false)

    # set a composition of exception modes separately, make sure the final result is the composited value
    BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, true)
    BigDecimal.mode(BigDecimal::EXCEPTION_NaN, true)
    assert_equal BigDecimal::EXCEPTION_INFINITY | BigDecimal::EXCEPTION_NaN, BigDecimal.mode(1)

    # reset the exception mode to 0 for the following tests
    BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, false)
    BigDecimal.mode(BigDecimal::EXCEPTION_NaN, false)

    # set a composition of exception modes with one call and retrieve it using the retrieval idiom
    # note: this is to check compatibility with MRI, which currently sets only the last mode
    # it checks for
    BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY | BigDecimal::EXCEPTION_NaN, true)
    assert_equal BigDecimal::EXCEPTION_INFINITY | BigDecimal::EXCEPTION_NaN, BigDecimal.mode(1)

    # rounding mode defaults to BigDecimal::ROUND_HALF_UP
    assert_equal BigDecimal::ROUND_HALF_UP, BigDecimal.mode(BigDecimal::ROUND_MODE)

    # make sure each setting complete replaces any previous setting
    [BigDecimal::ROUND_UP, BigDecimal::ROUND_DOWN, BigDecimal::ROUND_CEILING, BigDecimal::ROUND_FLOOR,
     BigDecimal::ROUND_HALF_UP, BigDecimal::ROUND_HALF_DOWN, BigDecimal::ROUND_HALF_EVEN].each do |mode|
    	assert_equal mode, BigDecimal.mode(BigDecimal::ROUND_MODE, mode)
    end

    # reset rounding mode to 0 for following tests
    BigDecimal.mode(BigDecimal::ROUND_MODE, BigDecimal::ROUND_HALF_UP)

    assert_raises(TypeError) { BigDecimal.mode(BigDecimal::ROUND_MODE, true) } # second argument must be a Fixnum
    assert_raises(ArgumentError) { BigDecimal.mode(BigDecimal::ROUND_MODE, 8) } # any Fixnum >= 8 should trigger this error, as the valid rounding modes are currently [0..6]
  end

  def test_marshaling
    f = 123.456
    bd = BigDecimal(f.to_s)
    bd_serialized = Marshal.dump(bd)
    assert_equal f, Marshal.restore(bd_serialized).to_f
  end

  #JRUBY-2272
  def test_marshal_regression
    assert_equal BigDecimal('0.0'), Marshal.load(Marshal.dump(BigDecimal('0.0')))
  end

  def test_large_bigdecimal_to_f
    pos_inf = BigDecimal("5E69999999").to_f
    assert pos_inf.infinite?
    assert pos_inf > 0
    assert_equal nil, BigDecimal("0E69999999").infinite?
    assert BigDecimal("0E69999999").to_f < Float::EPSILON
    neg_inf = BigDecimal("-5E69999999").to_f
    assert neg_inf.infinite?
    assert neg_inf < 0
    assert BigDecimal("5E-69999999").to_f < Float::EPSILON
  end

  def test_infinity
    assert_equal true, BigDecimal.new("0.0000000001").finite?

    assert_equal 1, BigDecimal("Infinity").infinite?
    assert_equal false, BigDecimal("-Infinity").finite?
    assert_equal false, BigDecimal("+Infinity").finite?

    assert_raises(TypeError) { BigDecimal(:"+Infinity") }
  end

  def test_large_precisions
    a = BigDecimal("1").div(BigDecimal("3"), 307)
    b = BigDecimal("1").div(BigDecimal("3"), 308)
    assert_equal a.to_f, b.to_f
  end

  def test_div_by_float_precision
    # GH-644
    a = BigDecimal(11023) / 2.2046
    assert_equal 5_000, a.to_f

    # GH-648
    b = BigDecimal(1.05, 10) / 1.48
    assert (b.to_f - 0.7094594594594595) < Float::EPSILON
  end

  def test_to_f # GH_2650
    assert_equal(BigDecimal.new("10.91231", 1).to_f, 10.91231)
    assert_equal(BigDecimal.new("10.9", 2).to_f, 10.9)
  end

  def test_new_
    num = BigDecimal("666666.22E4444")

    assert_equal num, BigDecimal("666_666.22E4444")
    assert_equal num, BigDecimal("666_666.22_E4_4_4_4")
    assert_equal num, BigDecimal("6_6_6__666_.2__2___E_444__4_")
  end

  def test_tail_junk # GH-3527
    b = BigDecimal("5-6")
    assert_equal BigDecimal('5'), b
    b = BigDecimal("100+42")
    assert_equal 100, b.to_i
  end

  def test_tail_junk2
    b = BigDecimal("+55555x6")
    assert_equal 55555, b
    b = BigDecimal("-10000.5,9")
    assert_equal(-10000.5, b)

    b = BigDecimal("+55555d-66E")
    assert_equal BigDecimal('55555e-66'), b
  end

  def test_tail_junk_invalid
    begin
      BigDecimal("42E")
      fail 'expected-to-raise'
    rescue ArgumentError => ex
      assert_equal 'invalid value for BigDecimal(): "42E"', ex.message
    end

    assert_raise(ArgumentError) { BigDecimal("2E+") }
    assert_raise(ArgumentError) { BigDecimal("1E-") }
    assert_raise(ArgumentError) { BigDecimal("1E+") }
    assert_raise(ArgumentError) { BigDecimal("0E") }
  end

  def test_div_returns_integer_by_default
    res = BigDecimal('10.111').div 3
    assert res.eql?(3)
    res = BigDecimal('1_000_000_000_000_000_000_000_000.99').div 1.to_r/3
    assert_equal 3_000_000_000_000_000_000_000_002, res
    assert_equal Integer, res.class
    res = BigDecimal('1_000_000_000_000_000_000_000_009.99').div 1_000.0
    assert res.eql?(1_000_000_000_000_000_000_000)
    res = BigDecimal('1_000_000_000_000_000_000_000_009.99').div 1_000.1
    assert_equal 999900009999000099990, res
  end

  def test_div_mult_precision
    small = BigDecimal("1E-99"); denom = BigDecimal('0.000000123')
    res = small / denom
    # MRI (2.5):
    # 0.8130081300813008130081300813008130081300813008130081300813008130081300813008130081300813008130081e-92
    puts "\n#{__method__} #{small} / #{denom} = #{res} (#{res.precs})" if $VERBOSE
    assert res.to_s.length > 40, "not enough precision: #{res}" # in JRuby 9.1 only 20
    assert_equal BigDecimal('0.81300813e-92'),  res.round(100)

    val1 = BigDecimal('1'); val2 = BigDecimal('3')
    res = val1 / val2
    puts "\n#{__method__} #{val1} / #{val2} = #{res} (#{res.precs})" if $VERBOSE
    assert 18 <= res.precs.first, "unexpected precision: #{res.precs.first}"
    assert res.precs.first <= 20, "unexpected precision: #{res.precs.first}"
    assert_equal '0.333333333333333333e0', res.to_s

    val1 = BigDecimal("1.00000000000000000001"); val2 = Rational(1, 3)
    res = val1 * val2
    puts "\n#{__method__} #{val1} * #{val2} = #{res} (#{res.precs})" if $VERBOSE
    assert res.to_s.start_with?('0.333333333333333333336666666666666666'), "invalid result: #{res.to_s}"
    # MRI (2.5):
    # 0.33333333333333333333666666666666666633333333333333333333e0

    # test_div from MRI's suite :
    assert_equal(BigDecimal('1486.868686869'), (BigDecimal('1472.0') / BigDecimal('0.99')).round(9))

    assert_equal(4.124045235, (BigDecimal('0.9932') / (700 * BigDecimal('0.344045') / BigDecimal('1000.0'))).round(9))
  end

  def test_zero_p # from MRI test_bigdecimal.rb
    # BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, false)
    # BigDecimal.mode(BigDecimal::EXCEPTION_NaN, false)

    assert_equal(true, BigDecimal("0").zero?)
    assert_equal(true, BigDecimal("-0").zero?)
    assert_equal(false, BigDecimal("1").zero?)
    #assert_equal(true, BigDecimal("0E200000000000000").zero?)
    assert_equal(false, BigDecimal("Infinity").zero?)
    assert_equal(false, BigDecimal("-Infinity").zero?)
    assert_equal(false, BigDecimal("NaN").zero?)
  end

  def test_power_of_three # from MRI test_bigdecimal.rb
    x = BigDecimal(3)
    assert_equal(81, x ** 4)
    #assert_equal(1.quo(81), x ** -4)
    assert_in_delta(1.0/81, x ** -4)
  end

  def test_div # from MRI test_bigdecimal.rb
    x = BigDecimal((2**100).to_s)
    assert_equal(BigDecimal((2**100 / 3).to_s), (x / 3).to_i)
    assert_equal(BigDecimal::SIGN_POSITIVE_ZERO, (BigDecimal("0") / 1).sign)
    assert_equal(BigDecimal::SIGN_NEGATIVE_ZERO, (BigDecimal("-0") / 1).sign)
    assert_equal(2, BigDecimal("2") / 1)
    assert_equal(-2, BigDecimal("2") / -1)

    #assert_equal(BigDecimal('1486.868686869'), BigDecimal('1472.0') / BigDecimal('0.99'), '[ruby-core:59365] [#9316]')

    #assert_equal(4.124045235, BigDecimal('0.9932') / (700 * BigDecimal('0.344045') / BigDecimal('1000.0')), '[#9305]')

    BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, false)
    assert_positive_zero(BigDecimal("1.0")  / BigDecimal("Infinity"))
    assert_negative_zero(BigDecimal("-1.0") / BigDecimal("Infinity"))
    assert_negative_zero(BigDecimal("1.0")  / BigDecimal("-Infinity"))
    assert_positive_zero(BigDecimal("-1.0") / BigDecimal("-Infinity"))

    # BigDecimal.mode(BigDecimal::EXCEPTION_INFINITY, true)
    # BigDecimal.mode(BigDecimal::EXCEPTION_ZERODIVIDE, false)
    # assert_raise_with_message(FloatDomainError, "Computation results to 'Infinity'") { BigDecimal("1") / 0 }
    # assert_raise_with_message(FloatDomainError, "Computation results to '-Infinity'") { BigDecimal("-1") / 0 }
  end

  def test_new # from MRI test_bigdecimal.rb
    assert_equal(1, BigDecimal("1"))
    assert_equal(1, BigDecimal("1", 1))
    assert_equal(1, BigDecimal(" 1 "))
    assert_equal(111, BigDecimal("1_1_1_"))
    assert_equal(10**(-1), BigDecimal("1E-1"), '#4825')

    #assert_raise(ArgumentError, /"_1_1_1"/) { BigDecimal("_1_1_1") }

    BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW, false)
    BigDecimal.mode(BigDecimal::EXCEPTION_NaN, false)
    assert_positive_infinite(BigDecimal("Infinity"))
    assert_negative_infinite(BigDecimal("-Infinity"))
    assert_nan(BigDecimal("NaN"))
    assert_positive_infinite(BigDecimal("1E1111111111111111111"))
  end

  def test_eqq_eql
    assert_equal false, BigDecimal('1000.0') == nil
    assert_equal true, BigDecimal('11000.0') != nil
    assert_equal true, BigDecimal('12000.0') != true
    assert_equal false, BigDecimal('1000.0').eql?(false)
    assert_equal false, BigDecimal('0.0000') === nil
    assert_raise(ArgumentError) { BigDecimal('1.001') >= true }
    assert_raise(ArgumentError) { BigDecimal('2.200') < false }
    assert_raise(ArgumentError) { BigDecimal('3.003') >=  nil }
  end

  private

  def assert_nan(x)
    assert(x.nan?, "Expected #{x.inspect} to be NaN")
  end

  def assert_positive_infinite(x)
    assert(x.infinite?, "Expected #{x.inspect} to be positive infinite")
    assert_operator(x, :>, 0)
  end

  def assert_negative_infinite(x)
    assert(x.infinite?, "Expected #{x.inspect} to be negative infinite")
    assert_operator(x, :<, 0)
  end

  def assert_positive_zero(x)
    assert_equal(BigDecimal::SIGN_POSITIVE_ZERO, x.sign, "Expected #{x.inspect} to be positive zero")
  end

  def assert_negative_zero(x)
    assert_equal(BigDecimal::SIGN_NEGATIVE_ZERO, x.sign, "Expected #{x.inspect} to be negative zero")
  end

end
