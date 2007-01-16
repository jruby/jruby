require 'test/unit'


class TestTrueClass < Test::Unit::TestCase

    # FIXME: This needs to be moved to a common location.
    def truth_table(method, *result)
      for a in [ false, true ]
        expected = result.shift
        assert_equal(expected, method.call(a))
        assert_equal(expected, method.call(a ? self : nil))
      end
    end

  def test_00sanity
    assert_equal(true,TRUE)
  end

  def test_and # '&'
    truth_table(true.method("&"), false, true)
  end

  def test_or # '|'
    truth_table(true.method("|"), true, true)
  end

  def test_carat # '^'
    truth_table(true.method("^"), true, false)
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
