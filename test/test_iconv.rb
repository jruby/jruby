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
end
