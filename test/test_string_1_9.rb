require 'test/unit'

class TestString19 < Test::Unit::TestCase
  #JRUBY-4464
  def test_tr_bang
    assert_nil 'hello'.tr!('','yb')
  end

  def test_tr_s_bang
    assert_nil 'hello'.tr_s!('','yb')
  end
end
