require 'test/unit'
require 'rubicon_testcase'

class TestTrueClass < RubiconTestCase

  def test_00sanity
    assert_equal(true,TRUE)
  end

  def test_and
    truth_table(true.method("&"), false, true)
  end

  def test_carat
    truth_table(true.method("^"), true, false)
  end

  def test_or
    truth_table(true.method("|"), true, true)
  end

  def test_to_s
    assert_equal("true", true.to_s)
    assert_equal("true", TRUE.to_s)
  end

  def test_type
    assert_equal(TrueClass, true.class)
    assert_equal(TrueClass, TRUE.class)
  end

end

