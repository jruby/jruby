require 'test/unit'
require 'nkf'
require 'test/test_helper'

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

  def test_mime_encode
    if !IBM_JVM
      # IBM JDK does not appear to support all the same encodings; See JRUBY-3301.
      assert_equal("hello=", NKF.nkf("-MQ", "hello"))
      assert_equal("aGVsbG8gd29ybGQ=", NKF.nkf("-MB", "hello world"))
    end
  end
end
