require 'test/unit'
require 'rubicon_testcase'

class TestFalseClass < RubiconTestCase

  def test_00sanity
    assert_equal(false, FALSE)
  end

  def test_and
    truth_table(false.method("&"), false, false)
  end

  def test_carat
    truth_table(false.method("^"), false, true)
  end

  def test_or
    truth_table(false.method("|"), false, true)
  end

  def test_to_s
    assert_equal("false", false.to_s)
    assert_equal("false", FALSE.to_s)
  end

  def test_type
    assert_equal(FalseClass, false.class)
    assert_equal(FalseClass, FALSE.class)
  end

end
