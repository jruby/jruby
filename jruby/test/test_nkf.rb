require 'test/unit'
require 'nkf'

class NKFTest < Test::Unit::TestCase

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
    assert_equal(utf, conv)
    conv = NKF.nkf('-w', sjis)
    assert_equal(utf, conv)
  end
  
  def test_euc_utf
    euc = ["a4a2a4a4a4a6a4a8a4aa"].pack("H*")
    utf = ["e38182e38184e38186e38188e3818a"].pack("H*")
    conv = NKF.nkf('-wE', euc)
    assert_equal(utf, conv)
    conv = NKF.nkf('-w', euc)
    assert_equal(utf, conv)
    conv = NKF.nkf('-w8', euc)
    assert_equal(utf, conv)
  end
end
