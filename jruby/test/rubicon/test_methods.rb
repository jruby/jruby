require 'test/unit'

class TestMethods < Test::Unit::TestCase

  def aaa(a, b=100, *rest)
    res = [a, b]
    res += rest if rest
    res
  end

  def testNotEnoughArguments
    assert_raise(ArgumentError) { aaa() }
    assert_raise(ArgumentError) { aaa }
  end

  def testArgumentPassing
    assert_equal([1, 100], aaa(1))
    assert_equal([1, 2], aaa(1, 2))
    assert_equal([1, 2, 3, 4], aaa(1, 2, 3, 4))
    assert_equal([1, 2, 3, 4], aaa(1, *[2, 3, 4]))
  end
end