# encoding: UTF-8
require 'test/unit'

class TestRegexp < Test::Unit::TestCase
  def test_match_m_p_method
    /backref/ =~ 'backref'
    # must match here, but not in a separate method, e.g., assert_send,
    # to check if $~ is affected or not.
    assert_equal(true, //.match?(""))
    assert_equal(true, /.../.match?(:abc))
    assert_equal(true, /b/.match?('abc'))
    assert_equal(true, /b/.match?('abc', 1))
    assert_equal(true, /../.match?('abc', 1))
    assert_equal(true, /../.match?('abc', -2))
    assert_equal(false, /../.match?('abc', -4))
    assert_equal(false, /../.match?('abc', 4))
    assert_equal(true, /../n.match?(("\u3042" + '\x'), 1))
    assert_equal(true, /\z/.match?(''))
    assert_equal(true, /\z/.match?('abc'))
    assert_equal(true, /R.../.match?('Ruby'))
    assert_equal(false, /R.../.match?('Ruby', 1))
    assert_equal(false, /P.../.match?('Ruby', 1))
    assert_equal('backref', $&)
  end
end