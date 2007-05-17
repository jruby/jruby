require 'test/unit'

class TestPack < Test::Unit::TestCase

  def test_pack_A
    a = %w(cat wombat x yy)
    assert_equal "catwomx  yy ",          a.pack("A3A3A3A3")
    assert_equal "cat",                   a.pack("A*")
    assert_equal "cwx  yy ",              a.pack("A3@1A3@2A3A3")
  end

  def test_pack_a
    a = %w(cat wombat x yy)
    assert_equal "catwomx\000\000yy\000", a.pack("a3a3a3a3")
    assert_equal "cat",                   a.pack("a*")
    assert_equal "ca",                    a.pack("a2")
    assert_equal "cat\000\000",           a.pack("a5")
  end

  def test_pack_B
    assert_equal "\x61",     ["01100001"].pack("B8")
    assert_equal "\x61",     ["01100001"].pack("B*")
    assert_equal "\x61",     ["0110000100110111"].pack("B8")
    assert_equal "\x61\x37", ["0110000100110111"].pack("B16")
    assert_equal "\x61\x37", ["01100001", "00110111"].pack("B8B8")
    assert_equal "\x60",     ["01100001"].pack("B4")
    assert_equal "\x40",     ["01100001"].pack("B2")
  end

  def test_pack_b
    assert_equal "\x86",     ["01100001"].pack("b8")
    assert_equal "\x86",     ["01100001"].pack("b*")
    assert_equal "\x86",     ["0110000100110111"].pack("b8")
    assert_equal "\x86\xec", ["0110000100110111"].pack("b16")
    assert_equal "\x86\xec", ["01100001", "00110111"].pack("b8b8")
    assert_equal "\x06",     ["01100001"].pack("b4")
    assert_equal "\x02",     ["01100001"].pack("b2")
  end

  def test_pack_C
    assert_equal "ABC",      [ 65, 66, 67 ].pack("C3")
    assert_equal "\377BC",   [ -1, 66, 67 ].pack("C*")
  end

  def test_pack_c
    assert_equal "ABC",      [ 65, 66, 67 ].pack("c3")
    assert_equal "\377BC",   [ -1, 66, 67 ].pack("c*")
  end

  def test_pack_H
    assert_equal "AB\n\x10",  ["4142", "0a", "12"].pack("H4H2H1")
    assert_equal "AB\n\x02",  ["1424", "a0", "21"].pack("h4h2h1")
  end

  def test_pack_M
    assert_equal("abc=02def=\ncat=\n=01=\n", 
                 ["abc\002def", "cat", "\001"].pack("M9M3M4"))
  end

  def test_pack_m
    assert_equal "aGVsbG8K\n",  ["hello\n"].pack("m")
  end

  def test_pack_u
    assert_equal ",:&5L;&\\*:&5L;&\\*\n",  ["hello\nhello\n"].pack("u")
  end

  def test_pack_U
    assert_equal "\xc2\xa9B\xe2\x89\xa0", [0xa9, 0x42, 0x2260].pack("U*")
  end

  def test_pack_ugly
    format = "c2x5CCxsdils_l_a6";
    # Need the expression in here to force ary[5] to be numeric.  This avoids
    # test2 failing because ary2 goes str->numeric->str and ary does not.
    ary = [1, -100, 127, 128, 32767, 987.654321098/100.0,
           12345, 123456, -32767, -123456, "abcdef"]
    x    = ary.pack(format)
    ary2 = x.unpack(format)

    assert_equal ary.length, ary2.length
    assert_equal ary.join(':'), ary2.join(':')
    assert_not_nil(x =~ /def/)
  end

  def test_unpack_A
    assert_equal ["cat", "wom", "x", "yy"], "catwomx  yy ".unpack("A3A3A3A3")

    assert_equal ["cat"], "cat  \000\000".unpack("A*")
    assert_equal ["cwx", "wx", "x", "yy"], "cwx  yy ".unpack("A3@1A3@2A3A3")
  end

  def test_unpack_a
    assert_equal ["cat", "wom", "x\000\000", "yy\000"],
                 "catwomx\000\000yy\000".unpack("a3a3a3a3")
    assert_equal ["cat \000\000"], "cat \000\000".unpack("a*")
    assert_equal ["ca"], "catdog".unpack("a2")
    assert_equal ["cat\000\000"], "cat\000\000\000\000\000dog".unpack("a5")
  end

  def test_unpack_B
    assert_equal ["01100001"], "\x61".unpack("B8")
    assert_equal ["01100001"], "\x61".unpack("B*")
    assert_equal ["0110000100110111"], "\x61\x37".unpack("B16")
    assert_equal ["01100001", "00110111"], "\x61\x37".unpack("B8B8")
    assert_equal ["0110"], "\x60".unpack("B4")

    assert_equal ["01"], "\x40".unpack("B2")
  end

  def test_unpack_b
    assert_equal ["01100001"], "\x86".unpack("b8")
    assert_equal ["01100001"], "\x86".unpack("b*")

    assert_equal ["0110000100110111"], "\x86\xec".unpack("b16")
    assert_equal ["01100001", "00110111"], "\x86\xec".unpack("b8b8")

    assert_equal ["0110"], "\x06".unpack("b4")
    assert_equal ["01"], "\x02".unpack("b2")
  end

  def test_unpack_C
    assert_equal [ 65, 66, 67 ],  "ABC".unpack("C3")
    assert_equal [ 255, 66, 67 ], "\377BC".unpack("C*")
  end

  def test_unpack_c
    assert_equal [ 65, 66, 67 ],  "ABC".unpack("c3")
    assert_equal [ -1, 66, 67 ],  "\377BC".unpack("c*")
  end
    
  def test_unpack_H
    assert_equal ["4142", "0a", "1"], "AB\n\x10".unpack("H4H2H1")
  end

  def test_unpack_h
    assert_equal ["1424", "a0", "2"], "AB\n\x02".unpack("h4h2h1")
  end

  def test_unpack_M
    assert_equal ["abc\002defcat\001", "", ""],
                 "abc=02def=\ncat=\n=01=\n".unpack("M9M3M4")
  end

  def test_unpack_m
    assert_equal ["hello\n"], "aGVsbG8K\n".unpack("m")
  end

  def test_unpack_u
    assert_equal ["hello\nhello\n"], ",:&5L;&\\*:&5L;&\\*\n".unpack("u")
  end

  def test_unpack_U
    assert_equal [0xa9, 0x42, 0x2260], "\xc2\xa9B\xe2\x89\xa0".unpack("U*")
  end

end
