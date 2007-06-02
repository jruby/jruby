require 'test/unit'


class TestNilClass < Test::Unit::TestCase

    # FIXME: This should be moved to a common location.
    def truth_table(method, *result)
      for a in [ false, true ]
        expected = result.shift
        assert_equal(expected, method.call(a))
        assert_equal(expected, method.call(a ? self : nil))
      end
    end

  def test_and # '&'
    truth_table(nil.method("&"), false, false)
  end

  def sideEffect
    $global = 1
  end

  def test_or # '|'
    truth_table(nil.method("|"), false, true)
    $global = 0
    assert_equal(true,  nil | sideEffect)
    assert_equal(1, $global)
  end

  def test_carat # '^'
    truth_table(nil.method("^"), false, true)
    $global = 0
    assert_equal(true,  nil ^ sideEffect)
    assert_equal(1, $global)
  end

  def test_nil?
    assert(nil.nil?)
  end

  def test_to_a
    assert_equal([], nil.to_a)
  end

  def test_to_i
    assert_equal(0, nil.to_i)
  end

  def test_to_s
    assert_equal("", nil.to_s)
  end
  
  def test_to_f
    assert_equal(0.0, nil.to_f)
  end
end
