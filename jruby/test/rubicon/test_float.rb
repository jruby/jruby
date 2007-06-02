require 'test/unit'


class TestFloat < Test::Unit::TestCase

  #
  # Check a float for approximate equality
  #
  # FIXME: nuke this or move it to a common location.
  def assert_flequal(exp, actual, msg=nil)
    assert_in_delta(exp, actual, exp == 0.0 ? 1e-7 : exp.abs/1e7, msg)
  end

  BIG = 3.1415926e43
  SML = 3.18309891613572e-44   # = 1/BIG

  ###########################################################################

  def test_UMINUS
    [ -99.0, -1.0, 0.0, +1.0 , +99.0].each { |n| assert_flequal(0.0, -n + n) }
  end

  def test_CMP # '<=>'
    assert_flequal(0, BIG <=> BIG)
    assert_flequal(0, SML <=> SML)
    assert_flequal(0, 1.0 <=> 1.0)

    assert_flequal(1, BIG <=> SML)
    assert_flequal(1, BIG <=> 0.0)
    assert_flequal(1, BIG <=> 1.0)
    assert_flequal(1, SML <=> 0.0)

    assert_flequal(-1, SML <=> BIG)
    assert_flequal(-1, SML <=> 0.1)
    assert_flequal(-1, SML <=> 1)

    # and negatives:
    assert_flequal(1, 1.0 <=> -1.0)
    assert_flequal(-1, -1.0 <=> 1.0)
  end

  def test_DIV # '/'
    assert_flequal(0.0, 0.0/3.0)
    assert_flequal(1.0, 3.0/3.0)
    assert_flequal(1.5, 4.5/3.0)
    assert_flequal(0.333333333, 1.0/3.0)

    assert_flequal(0.0, -0.0/3.0)
    assert_flequal(-1.0, -3.0/3.0)
    assert_flequal(-1.5, -4.5/3.0)
    assert_flequal(-0.333333333, -1.0/3.0)

    assert_flequal(0.0,  0.0/-3.0)
    assert_flequal(-1.0, 3.0/-3.0)
    assert_flequal(-1.5, 4.5/-3.0)
    assert_flequal(-0.333333333, 1.0/-3.0)

    assert_flequal(0.0, -0.0/-3.0)
    assert_flequal(1.0, -3.0/-3.0)
    assert_flequal(1.5, -4.5/-3.0)
    assert_flequal(0.333333333, -1.0/-3.0)

    
  end

  def test_nan?
    a = 1.0/0.0
    b = 0.0/0.0
    assert(!a.nan?)
    assert(b.nan?)
    assert(!99.0.nan?)
  end

  def test_infinite?
    a = 1.0/0.0
    b = -1.0/0.0

    assert_equal(1, a.infinite?)
    assert_equal(-1, b.infinite?)
    assert(!99.0.infinite?)
  end

  def test_finite?
    a = 1.0/0.0
    b = 0.0/0.0
    assert(!a.finite?)
    assert(!b.finite?)
    assert(99.0.finite?)
  end

  def test_EQUAL # '=='
    assert(0.0 ==  0.0)
    assert(BIG == BIG)
    assert(SML == SML)
    assert(-1.0 == -1.0)
    assert(!(0.0 == 1e-6))
  end

  def test_GE # '>='
    assert(0.0 >= 0.0)
    assert(1.0 >= 0.0)
    assert(BIG >= 0.0)
    assert(SML >= 0.0)
    assert(0.0 >= -1.0)
    assert(1.0 >= -1.0)
    assert(-1.0 >= -2.0)

    assert(!(0.0 >= 1.0))
    assert(!(0.0 >= BIG))
    assert(!(SML >= BIG))
  end

  def test_GT # '>'
    assert(1.0 > 0.0)
    assert(BIG > 0.0)
    assert(SML > 0.0)
    assert(0.0 > -1.0)
    assert(1.0 > -1.0)
    assert(-1.0 > -2.0)

    assert(!(0.0 > 0.0))
    assert(!(0.0 > 1.0))
    assert(!(0.0 > BIG))
    assert(!(SML > BIG))
  end

  def test_LE # '<='
    assert(0.0 <= 0.0)
    assert(0.0 <= 1.0)
    assert(0.0 <= BIG)
    assert(0.0 <= SML)
    assert(-1.0 <= 0.0)
    assert(-2.0 <= -1.0)

    assert(!(1.0 <= 0.0))
    assert(!(BIG <= 0.0))
    assert(!(SML <= 0.0))
  end

  def test_LT # '<'
    assert(0.0 < 1.0)
    assert(0.0 < BIG)
    assert(0.0 < SML)
    assert(-1.0 < 0.0)
    assert(-2.0 < -1.0)

    assert(!(0.0 < 0.0))
    assert(!(1.0 < 0.0))
    assert(!(BIG < 0.0))
    assert(!(SML < 0.0))
  end

  def test_MINUS # '-'
    assert_flequal(0.0, 0.0-0.0)
    assert_flequal(0.0, 1.0-1.0)
    assert_flequal(0.0, -1.0 - (-1.0))
    assert_flequal(-2.0, -1.0 - (1.0))
    assert_flequal(2.0, 1.0 - (-1.0))
    
    assert_flequal(77, 100.0 - 20.0 - 3.0)
  end

  def test_MOD # '%'
    assert_flequal(0.0,  0.0%123.0)

    assert_flequal(1.0,  13.0%4.0)
    assert_flequal(-3.0, 13.0%(-4.0))
    assert_flequal(3.0,  (-13.0)%4.0)
    assert_flequal(-1.0, (-13.0)%(-4.0))

    assert_flequal(1.5,  13.5%4.0)
    assert_flequal(-2.5, 13.5%(-4.0))
    assert_flequal(2.5,  (-13.5)%4.0)
    assert_flequal(-1.5, (-13.5)%(-4.0))

    assert_flequal(0.4,  13.4 % 1)
  end

  def test_MUL # '*'
    assert_flequal(0.0, 0.0*BIG)
    assert_flequal(BIG, 1.0*BIG)
    assert_flequal(SML, 1.0*SML)
    assert_flequal(1.0, BIG*SML)
    assert_flequal(-1.0, -1.0 *  1.0)
    assert_flequal( 1.0, -1.0 * -1.0)
  end

  def test_PLUS # '+'
    assert_flequal(0.0, 0.0 + 0.0)
    assert_flequal(1.0, 0.0 + 1.0)
    assert_flequal(1.0, 1.0 + 0.0)
    assert_flequal(0.0, 1.0 + (-1.0))
    assert_flequal(-1.0, 0.0 + (-1.0))
    assert_flequal(-2.0, -1.0 + (-1.0))
    assert_flequal(0.0, -1.0 + (1.0))

    assert_flequal(3.0, 0.0 + 3)
  end

  def test_POW # '**'
    assert_flequal(1e40, 10**40)
    assert_flequal(1e-40, 10**-40)
    assert_not_nil((10.0**10.0**10.0).infinite?)
    assert_flequal(0.0, 10.0**-(1000.0))
    assert_flequal(1.4142135623731, 2.0**0.5)
  end

  def test_abs
    assert_flequal(1.0, 1.0.abs)
    assert_flequal(1.0, (-1.0).abs)
  end

  def test_ceil
    assert_flequal(2.0,  (1.2).ceil)
    assert_flequal(2.0,  (2.0).ceil)
    assert_flequal(-1.0, (-1.2).ceil)
    assert_flequal(-2.0, (-2.0).ceil)
    assert_flequal(3,      2.6.ceil)
    assert_flequal(-2,    -2.6.ceil)
  end

  def test_divmod
    vals = { 
      [  13.0,  4.0 ] => [3.0,   1.0],
      [  13.0, -4.0 ] => [-4.0, -3.0],
      [ -13.0,  4.0 ] => [-4.0,  3.0],
      [ -13.0, -4.0 ] => [3.0,  -1.0],
    }

    vals.each { |ip, op|
      res = ip[0].divmod(ip[1])
      assert_flequal(op[0], res[0], "#{ip.join('.divmod ')}")
      assert_flequal(op[1], res[1], "#{ip.join('.divmod ')}")
    }

  end

  def test_floor
    assert_flequal(1.0,  (1.2).floor)
    assert_flequal(2.0,  (2.0).floor)
    assert_flequal(-2.0, (-1.2).floor)
    assert_flequal(2,    2.6.floor)
    assert_flequal(-3,  -2.6.floor)
  end

  def test_remainder
    assert_flequal(0.0,  0.0.remainder(123.0))

    assert_flequal(1.0,  13.0.remainder(4.0))
    assert_flequal(1.0,  13.0.remainder(-4.0))
    assert_flequal(-1.0, (-13.0).remainder(4.0))
    assert_flequal(-1.0, (-13.0).remainder(-4.0))

    assert_flequal(1.5,  13.5.remainder(4.0))
    assert_flequal(1.5,  13.5.remainder(-4.0))
    assert_flequal(-1.5,  (-13.5).remainder(4.0))
    assert_flequal(-1.5,  (-13.5).remainder(-4.0))
  end

  def test_round
    assert_instance_of(Fixnum, 1.5.round)
    assert_instance_of(Bignum, 1.5e20.round)
    assert_equal(1,   1.49.round)
    assert_equal(2,   1.5.round)
    assert_equal(-1, -1.49.round)
    assert_equal(-2, -1.5.round)
    assert_equal(3,   2.6.round)
  end

  def test_to_f
    a = BIG.to_f
    assert_equal(a, BIG)
    assert_equal(a.__id__, BIG.__id__)
  end

  def test_to_i
    assert_instance_of(Fixnum, 1.23.to_i)
    assert_equal(1,   1.4.to_i)
    assert_equal(1,   1.9.to_i)
    assert_equal(-1, -1.4.to_i)
    assert_equal(-1, -1.9.to_i)
  end

  def test_to_s
    assert_equal("Infinity",   (1.0/0.0).to_s)
    assert_equal("-Infinity",  (-1.0/0.0).to_s)
    assert_equal("NaN",        (0.0/0.0).to_s)

    assert_equal("1.23456",    1.23456.to_s)
    assert_equal("-1.23456",  -1.23456.to_s)
    assert_flequal(1.23e+45,   Float(1.23e+45.to_s))
    assert_flequal(1.23e-45,   Float(1.23e-45.to_s))
  end
  
  def test_truncate
    assert_flequal(2, 2.6.truncate)
    assert_flequal(-2, -2.6.truncate)
    assert_flequal(-2, -2.4.truncate)
  end

  def test_zero?
    assert(0.0.zero?)
    assert(!0.1.zero?)
  end

  def test_s_induced_from
    assert_flequal(1.0, Float.induced_from(1))
    assert_flequal(1.0, Float.induced_from(1.0))
  end

end
