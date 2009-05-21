require 'test/unit'

class TestPack < Test::Unit::TestCase  
  def setup
    @char_array  = %w/alpha beta gamma/
    @int_array   = [-1, 0, 1, 128]
    @float_array = [-1.5, 0.0, 1.5, 128.5]
    @bignum1      = 2**63
  end

  def teardown
    @char_array  = nil
    @int_array   = nil
    @float_array = nil
    @bignum1     = nil
  end

  def test_pack_w
    assert_equal( "\005", [5].pack('w'))
    assert_equal( "\203t", [500].pack('w'))
    assert_equal( "\203\206P", [50000].pack('w'))
    assert_equal( "\222\320\227\344\000", [5000000000].pack('w'))
    assert_equal( "\222\320\227\344\001", [5000000001].pack('w'))
    assert_equal( "\272\357\232\025", [123456789].pack('w'))
  end
  
  def test_pack_M
    assert_equal("alpha=\n", %w/alpha beta gamma/.pack("M")) # ok
    assert_equal("-1=\n", [-1, 0, 1, 128].pack("M"))
    assert_equal("1=\n", [1].pack("M"))
    assert_equal("-1.5=\n", [-1.5, 0.0, 1.5, 128.5].pack("M"))
    assert_equal("9223372036854775808=\n", [9223372036854775808].pack("M"))
  end

  def endian(data, n=4)
    [1].pack('I') == [1].pack('N') ? data.gsub(/.{#{n}}/){ |s| s.reverse } : data
  end

  def test_pack_q
    assert_equal(endian("\000\000\000\000\000\000\000\000", 8), [0].pack("q"))
    assert_equal(endian("\001\000\000\000\000\000\000\000", 8), [1].pack("q"))
    assert_equal(endian("\377\377\377\377\377\377\377\377", 8), [-1].pack("q"))
    assert_equal(endian("\377\377\377\377\377\377\377\377", 8), @int_array.pack("q"))
    assert_equal(endian("\377\377\377\377\377\377\377\377", 8), @float_array.pack("q"))
    assert_equal(endian("\000\000\000\000\000\000\000\200", 8), [@bignum1].pack("q"))
  end

  def test_pack_q_expected_errors
    assert_raises(TypeError){ @char_array.pack("q") }
    assert_raises(RangeError){ [(2**128)].pack("q") }
  end

  def test_pack_Q
    assert_equal(endian("\000\000\000\000\000\000\000\000", 8), [0].pack("Q"))
    assert_equal(endian("\001\000\000\000\000\000\000\000", 8), [1].pack("Q"))
    assert_equal(endian("\377\377\377\377\377\377\377\377", 8), [-1].pack("Q"))
    assert_equal(endian("\377\377\377\377\377\377\377\377", 8), @int_array.pack("Q"))
    assert_equal(endian("\377\377\377\377\377\377\377\377", 8), @float_array.pack("Q"))
    assert_equal(endian("\000\000\000\000\000\000\000\200", 8), [@bignum1].pack("Q"))
  end

  def test_pack_Q_expected_errors
    assert_raises(TypeError){ @char_array.pack("Q") }
    assert_raises(RangeError){ [(2**128)].pack("Q") }
  end

  # JRUBY-2502
  def test_pack_m_u_regression
    assert_equal(
      "QUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJD\nQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJD\nQUJD\n",
      ["ABC"*31].pack('m'))

    assert_equal(
      "M04%!04%!04%!04%!04%!04%!04%!04%!04%!04%!04%!04%!04%!04%!04%!\n%04%!04$`\n",
      ["A"*50].pack('u'))
  end

  # JRUBY-3677
  def test_pack_N_star_regression
    arr = ["head",1,1,1]
    assert_equal(arr.pack("A4N3"), arr.pack("A4N*"))
  end


  # JRUBY-2502
  def test_pack_M_regression
    assert_equal("ABCDEF=\n", ['ABCDEF'].pack('M'))
  end
end
