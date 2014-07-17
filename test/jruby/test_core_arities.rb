require 'test/unit'

# Test that core class methods are correctly raising errors for incorrect call
# arities.
class TestCoreArities < Test::Unit::TestCase
  def assert_argerr(&b)
    assert_raises(ArgumentError, &b)
  end

  def test_fixnum
    assert_argerr { 1.+(1,2) }
    assert_argerr { 1.-(1,2) }
    assert_argerr { 1.<(1,2) }
  end

  def test_array
    assert_argerr { [].+(1,2) }
    assert_argerr { [].pop(1,2) }
    assert_argerr { [].nil?(1) }
    assert_argerr { [].==(1,2) }
    assert_argerr { [].<<(1,2) }
    assert_argerr { [].empty?(1) }
  end

  def test_bignum
    assert_argerr { (2**500).+(1,2) }
    assert_argerr { (2**500).-(1,2) }
    assert_argerr { (2**500).<(1,2) }
  end

  def test_string
    assert_argerr { "".+(1,2) }
    assert_argerr { "".<(1,2) }
    assert_argerr { "".nil?(1) }
    assert_argerr { "".==(1,2) }
    assert_argerr { "".>=(1,2) }
    assert_argerr { "".<<(1,2) }
    assert_argerr { "".empty?(1) }
  end
end
