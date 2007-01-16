require 'test/unit'

# Note: conversion tests etc are in the language tests. This is
# simply for the class methods

class TestBignum < Test::Unit::TestCase

  def setup
    @big1 = 10**40 + 10**30 + 10**20 + 10**10 + 1
    @big2 = 2**80 + 2**40 + 2**20 + 2**10 + 1
  end

  # Local routine to check that a set of bits, and only a set of bits,
  # is set!
  # FIXME: This should probably move to a common file somewhere, yes?
  def checkBits(bits, num)
    0.upto(90)  { |n|
      expected = bits.include?(n) ? 1 : 0
      assert_equal(expected, num[n], "bit %d" % n)
    }
  end

  def fact(n)
    return 1 if n == 0
    f = 1
    while n>0
      f *= n
      n -= 1
    end
     f
  end

  def test_00_sanity
    assert_equal(10000000001000000000100000000010000000001, @big1)
    assert_equal(1208925819615728687383553, @big2)

    x = fact(40)
    assert_equal(x, x)
    assert_equal(x, fact(40))
    assert(x < x+2)
    assert(x > x-2)
    assert_equal(x, 815915283247897734345611269596115894272000000000)
    assert(x != 815915283247897734345611269596115894272000000001)
    assert_equal(x+1, 815915283247897734345611269596115894272000000001)
    assert_equal(x/fact(20), 335367096786357081410764800000)
    x = -x
    assert_equal(x, -815915283247897734345611269596115894272000000000)
    assert_equal(2-(2**32), -(2**32-2))
    assert_equal(2**32 - 5, (2**32-3)-2)

    a = 677330545177305025495135714080
    b = 14269972710765292560
    assert_equal(0, a % b)
    assert_equal(0, -a % b)

  end

  def test_UMINUS
    bm = -@big1
    assert_equal(0, @big1 + bm)
  end

  def test_AND # '&'
    checkBits([], @big1 & 0)
    checkBits([80, 40, 20, 10, 0], @big2 & -1)
    checkBits([80, 20,  0], @big2 & (2**80 + 2**20 + 1))
  end

  def test_AREF # '[]'
    checkBits( [80, 40, 20, 10, 0], @big2 )
  end

  def test_CMP # '<=>'
    assert_equal(1,  @big1 <=> @big2)
    assert_equal(-1, @big2 <=> @big1)
    assert_equal(0,  @big1 <=> @big1)
    assert_equal(1,  @big1 <=> 9999)
    assert_equal(-1, 9999  <=> @big1)
    assert_equal(-1, 1.23  <=> @big1)
    assert_equal(+1, @big2 <=> 1.23)
    # Negatives:
    assert_equal(-1, -@big1 <=> @big1)
    assert_equal(1,  @big1 <=> -@big1)
    assert_equal(-1,  -@big1 <=> -@big2)
  end

  def test_DIV # '/'
    big3 = 2**70 + 2**30 + 2**10 + 1
    assert_equal(big3, @big2 / 1024)
    assert_equal(1024, @big2 / big3)

    bm = -@big2
    assert_equal(-big3-1, bm / 1024)
    assert_equal(-1025, bm / big3)

    assert_equal(-big3-1, @big2 / -1024)
    assert_equal(-1025, @big2 / -big3)

    assert_equal(big3, bm / -1024)
    assert_equal(1024, bm / -big3)

    for i in 4000..4096
      n1 = 1 << i
      assert_equal((n1**2-1) / (n1+1), n1-1)
    end

    assert_equal(100000000000000000000, 10**40/10**20)
  end

  def test_LSHIFT # '<<'
    checkBits([81, 41, 21, 11, 1],  @big2 << 1)
    checkBits([90, 50, 30, 20, 10], @big2 << 10)
    checkBits([79, 39, 19,  9],     @big2 << -1)

    # there was a bug where the top digits were getting dropped
    num = 0x80000000
    num = 0x8000000000000000 unless num.is_a?(Bignum)
    if num.is_a?(Bignum)
      assert_equal(num*2, num << 1)
      assert_equal((num << 1)>>1, num)
    end

    for i in 1000..1014
      assert_equal(2**i, 1<<i)
    end
    
    n1 = 1<<1000
    for i in 1000..1014
      assert_equal(n1, 1 << i)
      n1 *= 2
    end
  end

  def test_MINUS # '-'
    checkBits([80, 40, 20], @big2 - 1025)
    assert_equal(1,      @big2 - (2**80 + 2**40 + 2**20 + 2**10))
    assert_equal(-@big1, 0-@big1)
    assert_equal(0,      @big1 - @big1)
  end

  def test_MUL # '*'
    big3 = 2**90 + 2**50 + 2**30 + 2**20 + 2**10
    assert_equal(big3, @big2 * 1024)
    assert_equal(big3, 1024 * @big2)

    bm = -@big2
    assert_equal(-big3, bm * 1024)
    assert_equal(-big3, 1024 * bm)

    assert_equal(-big3, @big2 * -1024)
    assert_equal(-big3, -1024 * @big2)

    assert_equal(big3, bm * -1024)
    assert_equal(big3, -1024 * bm)
  end

  def test_OR # '|'
    a = 0xffff0000ffff0000ffff0000
    b = 0xf0f0f0f0c3c3c3c3a5a5a5a5
    c = 0xfffff0f0ffffc3c3ffffa5a5

    assert_equal(c, a | b)
    assert_equal(c, b | a)

    assert_equal(a, a | 0)
  end

  def test_PLUS # '+'
    a = 10000000001000000000100000000010000000001
    b =  9999999999999999999999999999999999999999
    c = 20000000001000000000100000000010000000000
    d =           1000000000100000000010000000002

    assert_equal(a, a + 0)
    assert_equal(a, 0 + a)
    assert_equal(c, a + b)
    assert_equal(c, b + a)
    assert_equal(d, a + (-b))
    assert_equal(d, (-b) + a)

    assert_equal(10000000000000000000100000000000000000000, 10**40+10**20)
  end

  def test_POW # '**'
    a = 10000200001
    b = 100004000060000400001
    assert_equal(1, a**0)
    assert_equal(a, a**1)
    assert_equal(b, a**2)
    assert_equal(100001, a**(0.5))
    assert_equal(a.to_f, b**(0.5))
    assert((a**(-1) - 9.9998e-11).abs < 1e-16)
    assert((a**(-2) - 9.9996e-21).abs < 1e-26)
  end

  def test_REV # '~'
    a = ~(2**100)
    assert_equal(0, a[100])
    assert_equal(1, a[101])
    99.downto(0) {|i| assert_equal(1, a[i]) }

    a = ~a
    assert_equal(1, a[100])
    assert_equal(0, a[101])
    99.downto(0) {|i| assert_equal(0, a[i]) }
  end

  def test_RSHIFT # '>>'
    checkBits([79, 39, 19,  9],     @big2 >> 1)
    checkBits([70, 30, 10, 0],      @big2 >> 10)
    checkBits([81, 41, 21, 11, 1],  @big2 >> -1)

    n2 = n1 = 1 << 1000
    for i in 1..10
      n1 = n1 / 2
      n2 = n2 >> 1
      assert_equal(n1, n2)
    end
  end

  def test_EQUAL # '=='
    assert(81402749386839761113321 == 121**11)
    assert(!(81402749386839761113320 == 121**11))
  end

  def test_VERY_EQUAL # '==='
    assert(81402749386839761113321 === 121**11)
    assert(!(81402749386839761113320 === 121**11))
  end

  def test_eql? #
    assert(81402749386839761113321.eql?(121**11))
    assert(!( 81402749386839761113320.eql?(121**11) ))
  end

  def test_equal?
    a = 81402749386839761113321
    b = a
    c = 81402749386839761113321
    assert(a.equal?(b))
    assert(!(a.equal?(c)))
  end

  def test_XOR # '^'
    a = 0xffff0000ffff0000ffff0000
    b = 0xf0f0f0f0c3c3c3c3a5a5a5a5
    c = 0x0f0ff0f03c3cc3c35a5aa5a5
    d = 0xffff0000ffff0000ffff0001

    assert_equal(c, a ^ b)
    assert_equal(c, b ^ a)

    assert_equal(1, a ^ d)
    assert_equal(1, d ^ a)
  end

  def test_abs
    assert_equal(@big1, @big1.abs)
    bm = -@big1
    assert_equal(@big1, bm.abs)
  end

#  def test_coerce
#    fail("untested")
#  end

  def test_divmod
    div = {1         =>      [@big1, 0], 
           1001      =>      [9990009991008991009090909090919080919,     82],
           100001    =>      [99999000019999800002999970000399996,        5],
           100000001 =>      [99999999010000009900999900990100,    99009901],
           10000000000001 => [1000000000099900000009990010,      9990009991] }

    div.each do |n, r| 
      res = @big1.divmod(n)
      assert_equal(@big1, n*res[0] + res[1])
      assert(res[1] < n)
      assert_equal(r, res, n.to_s)
    end

    b = 10**80
    a = b * 9 + 7
    assert_equal(7, a.modulo(b))
    assert_equal(-b + 7, a.modulo(-b))
    assert_equal(b + -7, (-a).modulo(b))
    assert_equal(-7, (-a).modulo(-b))
    assert_equal(7, a.remainder(b))
    assert_equal(7, a.remainder(-b))
    assert_equal(-7, (-a).remainder(b))
    assert_equal(-7, (-a).remainder(-b))
  end

  def test_hash
    a = 10000000001000000000100000000010000000001
    assert_equal(a.hash, @big1.hash)
  end

  def test_MOD # '%'
    b = 10**40
    a = b * 9 + 7

    assert_equal(7,   a % b)
    assert_equal(7-b, a % (-b))
    assert_equal(b-7, (-a) % b)
    assert_equal(-7,  (-a) % (-b))
  end

  def test_remainder
    b = 10**80
    a = b * 9 + 7

    assert_equal(7,  a.remainder(b))
    assert_equal(7,  a.remainder(-b))
    assert_equal(-7, (-a).remainder(b))
    assert_equal(-7, (-a).remainder(-b))
  end

  def test_size
    32.upto(100) { |n|
      s = (2**n).size
      assert(s >= (2**(n-1)).size)
      assert(s >= n/8)
    }
  end

  def test_to_f
    f = @big1.to_f
    assert(f.class == Float)
    assert(f > 1e40)
    assert(f < (1e40 + 1e31))
  end

  def test_to_i
    assert_equal(@big1, @big1.to_i)
  end

  def test_to_s
    assert_equal("10000000001000000000100000000010000000001", @big1.to_s)
    assert_equal("-10000000001000000000100000000010000000001", (-@big1).to_s)

  end

  def test_zero?
    assert(!@big1.zero?)
  end

#  def test_s_induced_from
#    fail("untested")
#  end

  def test_div_by_zero
    100000000000000000000000000000000000000 / 0
    fail("Bignum divide by zero should raise ZeroDivisionError")
  rescue Exception => e
    assert(ZeroDivisionError === e)
  end
end
