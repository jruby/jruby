require 'test/unit'
require 'rubicon_testcase'

class TestNilClass < RubiconTestCase

  def test_and
    truth_table(nil.method("&"), false, false)
  end

  def test_carat
    truth_table(nil.method("^"), false, true)
    $global = 0
    assert_equal(true,  nil ^ util_side_effect)
    assert_equal(1, $global)
  end

  def test_inspect
    # TODO: raise NotImplementedError, 'Need to write test_inspect'
  end

  def test_nil_eh
    assert(nil.nil?)
  end

  def test_or
    truth_table(nil.method("|"), false, true)
    $global = 0
    assert_equal(true,  nil | util_side_effect)
    assert_equal(1, $global)
  end

  def test_to_a
    assert_equal([], nil.to_a)
  end

  def test_to_f
    assert_equal(0.0, nil.to_f)
  end

  def test_to_i
    assert_equal(0, nil.to_i)
  end

  def test_to_s
    assert_equal("", nil.to_s)
  end
  
  def util_side_effect
    $global = 1
  end
end
