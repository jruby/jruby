# coding: UTF-8
require 'test/unit'

class TestSprintf < Test::Unit::TestCase

  # Test Case based on https://github.com/jruby/jruby/issues/5556
  def test_sprintf_one
    n = 97.65625
    s = format("%f %f %.1f", n, n.round(1), n).to_s
    assert(s === "97.656250 97.700000 97.7", "Test failed, value of #{s} did not match '97.656250 97.700000 97.7'.")
  end
  
  def test_sprintf_two
    n = 1234567892.0
    s = format("%01.0f", n).to_s
    assert(s === "1234567892", "Test failed, value of #{s} did not match '1234567892'.")
  end

  def test_round_half_even
    assert_equal("5.00", sprintf("%.2f",5.005))
    assert_equal("5.01", sprintf("%.2f",5.0059))
    assert_equal("5.01", sprintf("%.2f",5.0051))
    assert_equal("5.00", sprintf("%.2f",5.0050))
    assert_equal("5.01", sprintf("%.2f",5.00501))
    assert_equal("5.000", sprintf("%.3f",5.0005))

    assert_equal("5.02", sprintf("%.2f",5.015))
    assert_equal("5.02", sprintf("%.2f",5.025))

    assert_equal("97.66A", "%.2fA" % 97.6562)
    assert_equal("29.56B", "%.2fB" % 29.5562)

    assert_equal "28.554", sprintf("%.3f", 28.5535)
    assert_equal "97.7X", sprintf("%.1fX", 97.65625)

    assert_equal("28.554", "%.3f" % 28.5535)
    assert_equal("97.7X", "%.1fX" % 97.65625)
  end

  def test_string
    assert_equal("š0", sprintf("%s", "š0"))
    assert_equal("", sprintf("%.0s", "foš"))
    assert_equal("š", sprintf("%.2s", "š"))
    assert_equal("0", sprintf("%.2s", "0"))

    assert_equal("fo", sprintf("%.2s", "foooo!"))

    assert_equal("解0", sprintf("%.2s", "解0析"))
    assert_equal("解Šu", sprintf("%.3s", "解Šuš"))
  end

  def test_nil_arg
    begin
      sprintf("%b", nil)
      fail("expected to raise")
    rescue TypeError => e
      assert_equal("can't convert nil into Integer", e.message)
    end
    assert_raises(TypeError) { "%b" % nil }
    assert_raises(TypeError) { "%i" % nil }
    assert_raises(TypeError) { "%x" % nil }
    assert_equal('', "%s" % nil)
  end

  def test_binary # from MRI's TestSprintf which has test_binary excluded
    assert_equal("0", sprintf("%b", 0))
    assert_equal("1", sprintf("%b", 1))
    assert_equal("10", sprintf("%b", 2))
    assert_equal("..1", sprintf("%b", -1))

    assert_equal("   0", sprintf("%4b", 0))
    assert_equal("   1", sprintf("%4b", 1))
    assert_equal("  10", sprintf("%4b", 2))
    assert_equal(" ..1", sprintf("%4b", -1))

    assert_equal("0000", sprintf("%04b", 0))
    assert_equal("0001", sprintf("%04b", 1))
    assert_equal("0010", sprintf("%04b", 2))
    assert_equal("..11", sprintf("%04b", -1))

    assert_equal("0000", sprintf("%.4b", 0))
    assert_equal("0001", sprintf("%.4b", 1))
    assert_equal("0010", sprintf("%.4b", 2))
    assert_equal("..11", sprintf("%.4b", -1))

    assert_equal("  0000", sprintf("%6.4b", 0))
    assert_equal("  0001", sprintf("%6.4b", 1))
    assert_equal("  0010", sprintf("%6.4b", 2))
    assert_equal("  ..11", sprintf("%6.4b", -1))

    assert_equal("   0", sprintf("%#4b", 0))
    assert_equal(" 0b1", sprintf("%#4b", 1))
    assert_equal("0b10", sprintf("%#4b", 2))
    assert_equal("0b..1", sprintf("%#4b", -1))

    assert_equal("0000", sprintf("%#04b", 0))
    assert_equal("0b01", sprintf("%#04b", 1))
    assert_equal("0b10", sprintf("%#04b", 2))
    #assert_equal("0b..1", sprintf("%#04b", -1))

    assert_equal("0000", sprintf("%#.4b", 0))
    assert_equal("0b0001", sprintf("%#.4b", 1))
    assert_equal("0b0010", sprintf("%#.4b", 2))
    assert_equal("0b..11", sprintf("%#.4b", -1))

    assert_equal("  0000", sprintf("%#6.4b", 0))
    assert_equal("0b0001", sprintf("%#6.4b", 1))
    assert_equal("0b0010", sprintf("%#6.4b", 2))
    assert_equal("0b..11", sprintf("%#6.4b", -1))

    assert_equal("+0", sprintf("%+b", 0))
    assert_equal("+1", sprintf("%+b", 1))
    assert_equal("+10", sprintf("%+b", 2))
    assert_equal("-1", sprintf("%+b", -1))

    assert_equal("  +0", sprintf("%+4b", 0))
    assert_equal("  +1", sprintf("%+4b", 1))
    assert_equal(" +10", sprintf("%+4b", 2))
    assert_equal("  -1", sprintf("%+4b", -1))

    assert_equal("+000", sprintf("%+04b", 0))
    assert_equal("+001", sprintf("%+04b", 1))
    assert_equal("+010", sprintf("%+04b", 2))
    assert_equal("-001", sprintf("%+04b", -1))

    assert_equal("+0000", sprintf("%+.4b", 0))
    assert_equal("+0001", sprintf("%+.4b", 1))
    assert_equal("+0010", sprintf("%+.4b", 2))
    assert_equal("-0001", sprintf("%+.4b", -1))

    assert_equal(" +0000", sprintf("%+6.4b", 0))
    assert_equal(" +0001", sprintf("%+6.4b", 1))
    assert_equal(" +0010", sprintf("%+6.4b", 2))
    assert_equal(" -0001", sprintf("%+6.4b", -1))
  end

end

