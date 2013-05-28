require 'test/unit'
require 'bigdecimal'

class TestBigDecimal < Test::Unit::TestCase

  def test_bad_to_s_format_strings
    bd = BigDecimal.new("1")
    assert_equal("0.1E1", bd.to_s)
    assert_equal("+0.1E1", bd.to_s("+-2"))
    assert_equal("0.23", BigDecimal.new("0.23").to_s("F"))
  end

  def test_no_singleton_methods_on_bigdecimal
    num = BigDecimal.new("0.001")
    assert_raise(TypeError) { class << num ; def amethod ; end ; end }
    assert_raise(TypeError) { def num.amethod ; end }
  end
  
  def test_can_instantiate_big_decimal
    assert_nothing_raised {BigDecimal.new("4")}
    assert_nothing_raised {BigDecimal.new("3.14159")}
  end
  
  def test_can_implicitly_instantiate_big_decimal
    # JRUBY-153 issues
    assert_nothing_raised {BigDecimal("4")}
    assert_nothing_raised {BigDecimal("3.14159")}
  end

  if RUBY_VERSION =~ /1\.8/
    def test_reject_arguments_not_responding_to_to_str
      assert_raise(TypeError) { BigDecimal.new(4) }
      assert_raise(TypeError) { BigDecimal(4) }
      if RUBY_VERSION =~ /1\.8/
        assert_raise(TypeError) { BigDecimal.new(3.14159) }
        assert_raise(TypeError) { BigDecimal(3.14159) }
      end
    end
  end

  def test_alphabetic_args_return_zero
    assert_equal( BigDecimal("0.0"), BigDecimal("XXX"),
                  'Big Decimal objects instanitiated with a value that starts
                  with a letter should have a value of 0.0' )
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
    expected = BigDecimal('0.31415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679821480865132823066453462141417033006060218E1')
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
    
    assert_equal("1.0", BigDecimal.new('1.0').to_s('F'))
    assert_equal("0.0", BigDecimal.new('0.0').to_s)
    
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
    
  def test_big_decimal_power
    n = BigDecimal("10")
    assert_equal(n.power(0), BigDecimal("1"))
    assert_equal(n.power(1), n)
    assert_equal(n.power(2), BigDecimal("100"))
    assert_equal(n.power(-1), BigDecimal("0.1"))
    assert_raises(TypeError) { n.power(1.1) }
  end

  def test_big_decimal_mode
    # Accept valid arguments to #mode
    assert BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW)
    assert BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW,true)
    assert BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW,false)
    
    # Reject invalid arguments to #mode
    assert_raises(TypeError) { BigDecimal.mode(true) } # first argument must be a Fixnum
    assert_raises(TypeError) { BigDecimal.mode(BigDecimal::EXCEPTION_OVERFLOW, 1) } # second argument must be [true|false]
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
    assert_equal BigDecimal::EXCEPTION_NaN, BigDecimal.mode(1)
    
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
    assert_raises(TypeError) { BigDecimal.mode(BigDecimal::ROUND_MODE, 8) } # any Fixnum >= 8 should trigger this error, as the valid rounding modes are currently [0..6]
  end

  def test_marshaling
    f = 123.456
    bd = BigDecimal.new(f.to_s)
    bd_serialized = Marshal.dump(bd)
    assert_equal f, Marshal.restore(bd_serialized).to_f
  end
  
  #JRUBY-2272
  def test_marshal_regression
    assert_equal BigDecimal('0.0'), Marshal.load(Marshal.dump(BigDecimal.new('0.0')))
  end

  def test_large_bigdecimal_to_f
    pos_inf = BigDecimal.new("5E69999999").to_f
    assert pos_inf.infinite?
    assert pos_inf > 0
    assert BigDecimal.new("0E69999999").to_f < Float::EPSILON
    neg_inf = BigDecimal.new("-5E69999999").to_f
    assert neg_inf.infinite?
    assert neg_inf < 0
    assert BigDecimal.new("5E-69999999").to_f < Float::EPSILON
  end
  
  #JRUBY-3818
  def test_decimal_format
    require 'java'
    format = java.text.DecimalFormat.new("#,##0.00")
    locale_separator = java.text.DecimalFormatSymbols.new().getDecimalSeparator()
    value = java.math.BigDecimal.new("10")
    assert_equal "10" + locale_separator.chr + "00", format.format(value)
  end

  #JRUBY-5190
  def test_large_precisions 
    a = BigDecimal("1").div(BigDecimal("3"), 307)
    b = BigDecimal("1").div(BigDecimal("3"), 308)
    assert_equal a.to_f, b.to_f
  end

  if RUBY_VERSION =~ /1\.9/ || RUBY_VERSION =~ /2\.0/
    # GH-644, GH-648
    def test_div_by_float_precision_gh644
      a = BigDecimal.new(11023)/2.2046
      assert_equal 5_000, a.to_f
    end

    def test_div_by_float_precision_gh648
      b = BigDecimal.new(1.05, 10)/1.48
      assert (b.to_f - 0.7094594594594595) < Float::EPSILON
    end
  end
end
