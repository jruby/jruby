require 'test/unit'
require 'stringio'
require 'iconv'

class TestIconv < Test::Unit::TestCase
  def test_euc2sjis
    euc = ["a4a2a4a4a4a6a4a8a4aa"].pack("H*")
    sjis = ["82a082a282a482a682a8"].pack("H*")
    iconv = Iconv.new('SHIFT_JIS', 'EUC-JP')
    str = iconv.iconv(euc)
    str << iconv.iconv(nil)
    assert_equal( sjis, str )
  end

  def test_close
    euc = ["a4a2a4a4a4a6a4a8a4aa"].pack("H*")
    sjis = ["82a082a282a482a682a8"].pack("H*")
    iconv = Iconv.new('Shift_JIS', 'EUC-JP')
    output = ""
    begin
      output += iconv.iconv(euc)
      output += iconv.iconv(nil)
    ensure
      assert_respond_to(iconv, :close)
      assert_equal("", iconv.close)
      assert_equal(sjis, output)
    end
  end

  def test_open_with_block
    euc = ["a4a2a4a4a4a6a4a8a4aa"].pack("H*")
    sjis = ["82a082a282a482a682a8"].pack("H*")
    assert_respond_to(Iconv, :open)
    iconv = Iconv.open('SHIFT_JIS', 'EUC-JP')
    str = iconv.iconv(euc)
    str << iconv.iconv(nil)
    assert_equal( sjis, str )
  end

  def test_open_without_block
    input = StringIO.new
    input << ["a4a2a4a4a4a6a4a8a4aa"].pack("H*")
    input << "\n"
    input << ["a4a2a4a4a4a6a4a8a4aa"].pack("H*")
    input << "\n"
    output = ""
    Iconv.open("Shift_JIS", "EUC-JP") do |cd|
      input.rewind
      input.each do |s|
        output << cd.iconv(s)
      end
      output << cd.iconv(nil)
    end
    sjis = ""
    sjis << ["82a082a282a482a682a8"].pack("H*")
    sjis << "\n"
    sjis << ["82a082a282a482a682a8"].pack("H*")
    sjis << "\n"
    assert_equal(sjis, output)
  end

  def test_ignore_option
    euc = ["a4a2a4a4a4a6a4a8a4aa"].pack("H*")
    sjis = ["82a082a282a482a682a8"].pack("H*")
    iconv = Iconv.new('SHIFT_JIS', 'EUC-JP//ignore')
    str = iconv.iconv(euc)
    str << iconv.iconv(nil)
    assert_equal( sjis, str )

    iconv = Iconv.new('SHIFT_JIS//IGNORE', 'EUC-JP//ignore')
    str = iconv.iconv(euc)
    str << iconv.iconv(nil)
    assert_equal( sjis, str )
  end

  def test_translit_option
    euc = ["a4a2a4a4a4a6a4a8a4aa"].pack("H*")
    sjis = ["82a082a282a482a682a8"].pack("H*")
    iconv = Iconv.new('SHIFT_JIS', 'EUC-JP//ignore')
    str = iconv.iconv(euc)
    str << iconv.iconv(nil)
    assert_equal( sjis, str )

    iconv = Iconv.new('SHIFT_JIS//TRANSLIT', 'EUC-JP//translit//ignore')
    str = iconv.iconv(euc)
    str << iconv.iconv(nil)
    assert_equal( sjis, str )
  end
  
  def test_unknown_encoding
  	assert_raise(Iconv::InvalidEncoding) { Iconv.iconv("utf-8", "X-UKNOWN", "heh") }
  end
  
  def test_string_offset
    # it should respect the internal offset of substrings
    iconv = Iconv.new("utf-16be", "utf-8")
    orig1 = "test"
    orig2 = "###test###"[3, 4]
    str1 = iconv.iconv(orig1)
    str2 = iconv.iconv(orig2)
    assert_equal( str1, str2 )
  end
  
  def test_iconv_range
    iconv = Iconv.new("us-ascii", "us-ascii")
    assert_equal( "", iconv.iconv("", 0, 0) )
    assert_equal( "", iconv.iconv("", 0, 2) )
    assert_equal( "", iconv.iconv("", 3, 4) )
    assert_equal( "", iconv.iconv("hello", 3, 0) )
    assert_equal( "o", iconv.iconv("hello", -1) )
    assert_equal( "o", iconv.iconv("hello", -1, -1) )
    assert_equal( "", iconv.iconv("hello", -1, -2) )
    assert_equal( "lo", iconv.iconv("hello", -2, -1) )
    assert_equal( "l", iconv.iconv("hello", -2, -2) )
    assert_equal( "ll", iconv.iconv("hello", -3, 4) )
    assert_equal( "he", iconv.iconv("hello", 0, -4) )
    assert_equal( "ell", iconv.iconv("hello", -4, -2) )
    assert_equal( "", iconv.iconv("hello", -4, 0) )
    assert_equal( "", iconv.iconv("hello", -2, -4) )
    assert_equal( "ell", iconv.iconv("hello", 1, -2) )
    assert_equal( "hello", iconv.iconv("hello", -5, 100) )
    assert_equal( "", iconv.iconv("hello", -6, 100) )
  end
end
