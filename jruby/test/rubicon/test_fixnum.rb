require 'test/unit'


class TestFixnum < Test::Unit::TestCase

  # Local routine to check that a set of bits, and only a set of bits,
  # is set!
  # FIXME: This should probably move to a common file somewhere, yes?
  def checkBits(bits, num)
    0.upto(90)  { |n|
      expected = bits.include?(n) ? 1 : 0
      assert_equal(expected, num[n], "bit %d" % n)
    }
  end

  #
  # Check a float for approximate equality
  #
  # FIXME: nuke this or move it to a common location.
  def assert_flequal(exp, actual, msg=nil)
    assert_in_delta(exp, actual, exp == 0.0 ? 1e-7 : exp.abs/1e7, msg)
  end

  # assumes 8 bit byte, 1 bit flag, and 2's comp
  MAX = 2**(1.size*8 - 2) - 1
  MIN = -MAX - 1

  def test_s_induced_from
    assert_equal(1, Fixnum.induced_from(1))
    assert_equal(1, Fixnum.induced_from(1.0))
  end

  def test_UMINUS
    # This is actually a test of + !
    # [ -99, -1, 0, +1 , +99].each { |n| assert_equal(0, -n + n) }

    # But I suppose this might be a test of Array.. :-(
    a = [ -99, -1, 0, +1 , +99]
    b = a.reverse
    a.each_index { |n| assert_equal(b[n], -a[n]) }

  end

  def test_AND # '&'
    n1 = 0b0101
    n2 = 0b1100
    assert_equal(0,      n1 & 0)
    assert_equal(0b0100, n1 & n2)
    assert_equal(n1,     n1 & -1)
  end

  def test_AREF # '[]'
    checkBits([], 0)
    checkBits([15, 11, 7, 3], 0x8888)
  end

  def test_CMP # '<=>'
    assert_equal(0, MAX <=> MAX)
    assert_equal(0, MIN <=> MIN)
    assert_equal(0, 1   <=> 1)

    assert_equal(1, MAX <=> MIN)
    assert_equal(1, MAX <=> 0)
    assert_equal(1, MAX <=> 1)

    assert_equal(-1, MIN <=> MAX)
    assert_equal(-1, MIN <=> 0)
    assert_equal(-1, MIN <=> 1)
  end

  def test_DIV # '/'
    assert_equal(2, 6 / 3)
    assert_equal(2, 7 / 3)
    assert_equal(2, 8 / 3)
    assert_equal(3, 9 / 3)

    assert_equal(2, -6 / -3)
    assert_equal(2, -7 / -3)
    assert_equal(2, -8 / -3)
    assert_equal(3, -9 / -3)

    assert_equal(-2, -6 / 3)
    assert_equal(-3, -7 / 3)
    assert_equal(-3, -8 / 3)
    assert_equal(-3, -9 / 3)
    
    assert_equal(-2, 6 / -3)
    assert_equal(-3, 7 / -3)
    assert_equal(-3, 8 / -3)
    assert_equal(-3, 9 / -3)
    
    assert_equal(-2, MIN / MAX)
    assert_equal(-1,  MAX / MIN)
  end

  def test_EQUAL # '=='
    assert(0 == 0)
    assert(MIN == MIN)
    assert(MAX == MAX)
    assert(!(MIN == MAX))
    assert(!(1 == 0))
  end

  def test_GE # '>='
    assert(0 >= 0)
    assert(1 >= 0)
    assert(MAX >= 0)
    assert(0 >= MIN)

    assert(!(0 >= 1))
    assert(!(0 >= MAX))
    assert(!(MIN >= 0))
  end

  def test_GT # '>'
    assert(1 > 0)
    assert(MAX > 0)
    assert(0 > MIN)

    assert(!(0 > 0))
    assert(!(0 > 1))
    assert(!(0 > MAX))
    assert(!(MIN > 0))
  end

  def test_LE # '<='
    assert(0 <= 0)
    assert(0 <= 1)
    assert(0 <= MAX)
    assert(MIN <= 0)

    assert(!(1 <= 0))
    assert(!(MAX <= 0))
    assert(!(0 <= MIN))
  end

  def test_LSHIFT # '<<'
    assert_equal(0, 0 << 1)
    assert_equal(2, 1 << 1)
    assert_equal(8, 1 << 3)
    assert_equal(2**20, 1 << 20)

    assert_equal(-2, (-1) << 1)
    assert_equal(-8, (-1) << 3)

    a = 1 << (1.size*8 - 2)
    assert_instance_of(Bignum, a)

    a = (-1) << (1.size*8 - 1)
    assert_instance_of(Bignum, a)
  end

  def test_LT # '<'
    assert(0 < 1)
    assert(0 < MAX)
    assert(MIN < 0)

    assert(!(0 < 0))
    assert(!(1 < 0))
    assert(!(MAX < 0))
    assert(!(0 < MIN))
  end

  def test_MINUS # '-'
    assert_equal(0, 0-0)
    assert_equal(0, 1-1)
    assert_equal(0, -1 - (-1))
    
    assert_equal(77, 100 - 20 - 3)

    a = MIN - 1
    assert_instance_of(Bignum, a)
  end

  def test_MOD # '%'
    # See "The Higher Arithmetic" H. Davenport, Fifth Edition
    # Cambridge University press, (C)1962-1982
    # ISBN 0-521-28678-6
    # Page 41 Ch 2
    # a = b mod m is defined to mean that a - b is divisible by m.
    # so a = b mod m => (a - b) mod m = 0

    a = [  0,  1, -3,   3,  -1]
    b = [  0, 13, 13, -13, -13]
    m = [123,  4, -4,   4,  -4]
    a.each_index do
      |i|
      assert_equal(a[i], b[i] % m[i])
      assert_equal(0, (a[i]-b[i]) % m[i])
    end
  end

  def test_MUL # '*'
    assert_equal(0, 0*MAX)
    a = 1 * MAX
    assert_equal(MAX, a)
    assert_instance_of(Fixnum, a)
    a = 1 * MIN
    assert_equal(MIN, a)
    assert_instance_of(Fixnum, a)
    
    a = -1 * MAX
    assert_equal(-MAX, a)
    assert_instance_of(Fixnum, a)

    a = -1 * MIN
    assert_equal(-MIN, a)
    assert_instance_of(Bignum, a)

    assert_flequal(9.5, 19 * 0.5)
  end

  def test_OR # '|'
    n1 = 0b0101
    n2 = 0b1100
    assert_equal(n1,     n1 | 0)
    assert_equal(0b1101, n1 | n2)
    assert_equal(-1,     n1 | -1)
  end

  def test_PLUS # '+'
    assert_equal(MIN, 0 + MIN)
    assert_equal(MIN, MIN + 0)

    a = MIN + 1
    assert(a > MIN)
    assert_instance_of(Fixnum, a)

    a = MAX + 1
    assert(a > MAX)
    assert_instance_of(Bignum, a)
  end

  def test_POW # '**'
    assert_equal(0, 0**1)
    assert_equal(1, 0**0)
    assert_not_nil((0**-1).infinite?)

    assert_equal(1, 1**1)
    assert_equal(1, 1**0)
    assert_equal(1, 1**-1)

    assert_equal(81, 9**2)
    assert_equal(9,  81**0.5)
  end

  def test_REV # '~'
    n1 = -1
    n2 = 0b1100
    assert_equal(0, ~n1)
    assert_equal(-2, ~1)
    assert_equal(n2, ~(~n2))
  end

  def test_RSHIFT # '>>'
    assert_equal(0, 0 >> 1)
    assert_equal(0, 1 >> 1)
    assert_equal(1, 8 >> 3)
    assert_equal(1, 2**20 >> 20)

    assert_equal(-1, (-2) >> 1)
    assert_equal(-1, (-8) >> 3)
    assert_equal(-64, (-8) << 3)
  end

  def test_XOR # '^'
    n1 = 0b0101
    n2 = 0b1100
    assert_equal(0b1001, n1 ^ n2)
    assert_equal(n1,     n1 ^ 0)
    assert_equal(~n1,    n1 ^ -1)
  end

  def test_abs
    assert_equal(1, 1.abs)
    assert_equal(1, (-1).abs)
    assert_equal(0, 0.abs)

    a = MAX.abs
    assert_equal(a, MAX)
    assert_instance_of(Fixnum, a)

    a = MIN.abs
    assert_equal(a, MAX+1)
    assert_instance_of(Bignum, a)
  end

  def test_downto
    vals = [ 7,6,5,4 ]
    7.downto(4) {|i| assert_equal(vals.shift, i) }
    assert_equal(0, vals.length)

    vals = [ -4, -5, -6, -7 ]
    (-4).downto(-7) {|i| assert_equal(vals.shift, i) }
    assert_equal(0, vals.length)

    vals = [ 2, 1, 0, -1, -2 ]
    2.downto(-2) {|i| assert_equal(vals.shift, i) }
    assert_equal(0, vals.length)

    vals = [ ]
    -4.downto(-2) {|i| assert_equal(vals.shift, i) }
    assert_equal(0, vals.length)
  end

  def test_id2name
    a = :Wombat
    assert_instance_of(Symbol, a)
    ai = a.to_i
    assert_instance_of(Fixnum, ai)
    assert_equal("Wombat", ai.id2name)

    a = :<<
    assert_instance_of(Symbol, a)
    ai = a.to_i
    assert_instance_of(Fixnum, ai)
    assert_equal("<<", ai.id2name)
  end

  def test_next
    assert_equal(1, 0.next)
    assert_equal(0, (-1).next)
    assert(MIN.next > MIN)
    a = MAX.next
    assert_instance_of(Bignum, a)
  end

  def test_remainder
    assert_equal(1, 13.remainder(4))
    assert_equal(1, 13.remainder(-4))
    assert_equal(-1, (-13).remainder(4))
    assert_equal(-1, (-13).remainder(-4))
  end

  def test_size
    assert((1.size == 4) || (1.size == 8))
  end

  def test_step
    vals = [1, 4, 7, 10 ]
    1.step(10, 3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [1, 4, 7, 10 ]
    1.step(12, 3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [10, 7, 4, 1 ]
    10.step(1, -3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [10, 7, 4, 1 ]
    10.step(-1, -3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [ 1 ]
    1.step(1, 3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [ 1 ]
    1.step(1, -3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)

    vals = [  ]
    1.step(0, 3) { |i| assert_equal(i, vals.shift) }
    assert_equal(0, vals.length)
  end

  def test_succ
    assert_equal(1, 0.succ)
    assert_equal(0, (-1).succ)
    assert(MIN.succ > MIN)
    a = MAX.succ
    assert_instance_of(Bignum, a)
  end

  def test_times
    vals = [ 0, 1, 2, 3, 4 ]
    5.times {|i| assert_equal(vals.shift, i) }
    assert_equal(0, vals.length)

    vals = [ ]
    0.times {|i| assert_equal(vals.shift, i) }
    assert_equal(0, vals.length)
  end

  def test_to_f
    f = MAX.to_f
    assert_instance_of(Float, f)
    assert((f - MAX).abs < 0.5)
  end

  def test_to_i
    assert(MAX.to_i == MAX)
  end

  def test_to_s
    assert_equal("123", 123.to_s)
    assert_equal("-123", (-123).to_s)
  end

  def test_upto
    vals = [ 4,5,6,7 ]
    4.upto(7) {|i| assert_equal(vals.shift, i) }
    assert_equal(0, vals.length)

    vals = [ -7, -6, -5, -4 ]
    (-7).upto(-4) {|i| assert_equal(vals.shift, i) }
    assert_equal(0, vals.length)

    vals = [ -2, -1, 0, 1, 2 ]
    (-2).upto(2) {|i| assert_equal(vals.shift, i) }
    assert_equal(0, vals.length)

    vals = [ ]
    (-2).upto(-4) {|i| assert_equal(vals.shift, i) }
    assert_equal(0, vals.length)

  end

  def test_zero?
    assert(0.zero?)
    assert(!1.zero?)
  end

  def test_div_by_zero
    1 / 0
    fail("Fixnum divide by zero should raise ZeroDivisionError")
  rescue Exception => e
    assert(ZeroDivisionError === e)
  end

end
