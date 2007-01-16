require 'test/unit'


class TestFalseClass < Test::Unit::TestCase

  # FIXME: This should probably be moved to a common file somewhere.
  def truth_table(method, *result)
    for a in [ false, true ]
      expected = result.shift
      assert_equal(expected, method.call(a))
      assert_equal(expected, method.call(a ? self : nil))
    end
  end

  def test_00sanity
    assert_equal(false, FALSE)
  end

  def test_and # '&'
    truth_table(false.method("&"), false, false)
  end

  def test_or # '|'
    truth_table(false.method("|"), false, true)
  end

  def test_carat # '^'
    truth_table(false.method("^"), false, true)
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
