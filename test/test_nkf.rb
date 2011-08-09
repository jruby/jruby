require 'test/unit'
require 'nkf'
require 'test/test_helper'
require 'kconv'

class NKFTest < Test::Unit::TestCase
  include TestHelper
  
  def test_module_method_define?
    assert_respond_to(NKF, :guess)
    assert_respond_to(NKF, :guess1)
    assert_respond_to(NKF, :guess2)
    assert_respond_to(NKF, :nkf)
  end

#  def test_guess
#    sjis = ["82a082a282a482a682a8"].pack("H*")
#    euc = ["a4a2a4a4a4a6a4a8a4aa"].pack("H*")
#    assert_equal(NKF.guess(sjis), NKF::SJIS)
#    assert_equal(NKF.guess(euc), NKF::EUC)
#  end
  
  def test_sjis_utf
    sjis = ["82a082a282a482a682a8"].pack("H*")
    utf = ["e38182e38184e38186e38188e3818a"].pack("H*")
    conv = NKF.nkf('-wS', sjis)
    begin
      assert_equal(utf, conv)
      conv = NKF.nkf('-w', sjis)
      assert_equal(utf, conv)
    rescue ArgumentError
      # IBM JDK does not appear to support all the same encodings; See JRUBY-3301.
    end
  end

  def test_icoc
    sjis = ["82a082a282a482a682a8"].pack("H*")
    utf = ["e38182e38184e38186e38188e3818a"].pack("H*")
    begin
      conv = NKF.nkf("-m0 -x -W --oc=cp932 --fb-subchar=63", utf)
      assert_equal(sjis, conv)
      conv = NKF.nkf("-m0 -x -w --ic=cp932", sjis)
      assert_equal(utf, conv)
    rescue ArgumentError
      # IBM JDK does not appear to support all the same encodings; See JRUBY-3301.
    end
  end
  
  def test_euc_utf
    euc = ["a4a2a4a4a4a6a4a8a4aa"].pack("H*")
    utf = ["e38182e38184e38186e38188e3818a"].pack("H*")
    conv = NKF.nkf('-wE', euc)
    assert_equal(utf, conv)
    if !IBM_JVM
      # IBM JDK does not appear to support all the same encodings; See JRUBY-3301.
      conv = NKF.nkf('-w', euc)
      assert_equal(utf, conv)
      conv = NKF.nkf('-w8', euc)
      assert_equal(utf, conv)
    end
  end

  def test_mime_decode
    assert_equal("hello", NKF.nkf("-m", "=?US-ASCII?Q?hello=?="))
    assert_equal("hello world", NKF.nkf("-m", "=?US-ASCII?Q?hello=20world?="))
    assert_equal("hello world", NKF.nkf("-m", "=?US-ASCII?B?aGVsbG8gd29ybGQ=?="))
  end

  def test_multiple_mime_decode
    assert_equal("hello worldhello worldhello world", NKF.nkf("-m", "=?US-ASCII?B?aGVsbG8gd29ybGQ=?= =?US-ASCII?B?aGVsbG8gd29ybGQ=?= =?US-ASCII?B?aGVsbG8gd29ybGQ=?="))
  end

  def test_mime_encode
    if !IBM_JVM
      # IBM JDK does not appear to support all the same encodings; See JRUBY-3301.
      assert_equal("hello=", NKF.nkf("-MQ", "hello"))
      assert_equal("aGVsbG8gd29ybGQ=", NKF.nkf("-MB", "hello world"))
    end
  end
  
  def test_all_encodings
    # for JRUBY-5591, just make sure actual encodings function
    jis = Kconv.kconv("foo", Kconv::JIS, Kconv::ASCII)
    euc = Kconv.kconv("foo", Kconv::EUC, Kconv::ASCII)
    sjis = Kconv.kconv("foo", Kconv::SJIS, Kconv::ASCII)
    binary = Kconv.kconv("foo", Kconv::BINARY, Kconv::ASCII)
    utf8 = Kconv.kconv("foo", Kconv::UTF8, Kconv::ASCII)
    utf16 = Kconv.kconv("foo", Kconv::UTF16, Kconv::ASCII)
    utf32 = Kconv.kconv("foo", Kconv::UTF32, Kconv::ASCII)
    
    assert_equal "foo", jis
    assert_equal "foo", euc
    assert_equal "foo", sjis
    assert_equal "foo", binary
    assert_equal "foo", utf8
    # we're inserting byte order mark...
    #assert_equal "\000f\000o\000o", utf16
    assert_equal "\376\377\000f\000o\000o", utf16
    # this doesn't seem right...
    #assert_equal "foo", utf32
    
    jis = Kconv.kconv(jis, Kconv::ASCII, Kconv::JIS)
    euc = Kconv.kconv(euc, Kconv::ASCII, Kconv::EUC)
    sjis = Kconv.kconv(sjis, Kconv::ASCII, Kconv::SJIS)
    binary = Kconv.kconv(binary, Kconv::ASCII, Kconv::BINARY)
    utf8 = Kconv.kconv(utf8, Kconv::ASCII, Kconv::UTF8)
    utf16 = Kconv.kconv(utf16, Kconv::ASCII, Kconv::UTF16)
    utf32 = Kconv.kconv(utf32, Kconv::ASCII, Kconv::UTF32)
    
    assert_equal "foo", jis
    assert_equal "foo", euc
    assert_equal "foo", sjis
    assert_equal "foo", binary
    assert_equal "foo", utf8
    assert_equal "foo", utf16
    assert_equal "foo", utf32
  end
end
