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
  end

  def test_pack_Q
    assert_equal(endian("\000\000\000\000\000\000\000\000", 8), [0].pack("Q"))
    assert_equal(endian("\001\000\000\000\000\000\000\000", 8), [1].pack("Q"))
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

  # same as MRI's test_pack_infection
  # except for 'P' and 'p' formats (aren't implemented)
  def test_pack_infection
    tainted_array_string = ["123456"]
    tainted_array_string.first.taint
    ['a', 'A', 'Z', 'B', 'b', 'H', 'h', 'u', 'M', 'm'].each do |f|
      assert_predicate(tainted_array_string.pack(f), :tainted?)
    end
  end

end
