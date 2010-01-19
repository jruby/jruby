require 'test/unit'

class TestString19 < Test::Unit::TestCase
  #JRUBY-4464
  def test_tr_bang
    assert_nil 'hello'.tr!('','yb')
  end

  def test_tr_s_bang
    assert_nil 'hello'.tr_s!('','yb')
  end

  # JRUBY-4424
  def test_convert_to_fixnum
    assert_converted_to_fixnum(0.0, 0, 0, "_9")
    assert_converted_to_fixnum(19.0, 25, 19, "1_9")
    assert_converted_to_fixnum(19.0, 25, 19, "19_")
    assert_converted_to_fixnum(19.0, 1, 1, "1__9")
    assert_converted_to_fixnum(1.9, 1, 1, "1_._9")
    assert_converted_to_fixnum(1.9, 1, 1, "1_.9")
    assert_converted_to_fixnum(1.9, 1, 1, "1._9")
  end

  def assert_converted_to_fixnum(float_expected, hex_expected, int_expected, actual)
    assert_equal(float_expected, actual.to_f)
    assert_equal(hex_expected, actual.hex)
    assert_equal(int_expected, actual.to_i)
  end

  #JRUBY-4463
  def test_concat
    assert_raise(RangeError) {'test' << -1}
    assert_raise(RangeError) {'test' << -2}
    assert_raise(RangeError) {'test' << -3}
    assert_raise(RangeError) {'test' << -2**64}
  end
end
