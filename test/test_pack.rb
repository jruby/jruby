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

  unless RUBY_VERSION =~ /1\.9/
    def test_pack_q_expected_errors
      assert_raises(TypeError){ @char_array.pack("q") }
      assert_raises(RangeError){ [(2**128)].pack("q") }
    end
  end

  def test_pack_Q
    assert_equal(endian("\000\000\000\000\000\000\000\000", 8), [0].pack("Q"))
    assert_equal(endian("\001\000\000\000\000\000\000\000", 8), [1].pack("Q"))
    assert_equal(endian("\377\377\377\377\377\377\377\377", 8), [-1].pack("Q"))
    assert_equal(endian("\377\377\377\377\377\377\377\377", 8), @int_array.pack("Q"))
    assert_equal(endian("\377\377\377\377\377\377\377\377", 8), @float_array.pack("Q"))
    assert_equal(endian("\000\000\000\000\000\000\000\200", 8), [@bignum1].pack("Q"))
  end

  unless RUBY_VERSION =~ /1\.9/
    def test_pack_Q_expected_errors
      assert_raises(TypeError){ @char_array.pack("Q") }
      assert_raises(RangeError){ [(2**128)].pack("Q") }
    end
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

  def test_pack_m_0_RFC4648
    tobe = "QUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJD\nQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJDQUJD\nQUJD\n"
    if RUBY_VERSION >= "1.9"
      tobe.gsub!(/\n/, '')
    end
    assert_equal(tobe, ["ABC"*31].pack('m0'))
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

  # JRuby-4647
  def test_unpack_M
    assert_equal(["foo\r\n\r\r\r1\r2\r\r\r\rvvs"], "foo\r\n\r\r\r1\r=\r\n2\r\r\r\rvvs".unpack("M"))
    assert_equal(["foo\r\n"], "foo\r\n".unpack("M"))
  end

  # JRUBY-4967
  def test_pack_CC
    assert_raises(ArgumentError) { [0].pack('CC') }
  end

  unless RUBY_VERSION =~ /1\.9/
    def test_unpack_at_on_substring
      assert_equal([?c], 'abcdef'[1..-1].unpack('@1c'))
      assert_equal([?f], 'abcdef'[1..-1].unpack('x1@*c'))
    end
  end
end
