# encoding: UTF-8
require 'test/unit'

class TestRegex19 < Test::Unit::TestCase
  def test_to_s
    assert_equal /(?-mix:foo|bar)/, /#{Regexp.union("foo", "bar")}/
  end

  # JRUBY-5622
  def test_dstr_with_unicode_class
    letter = '\p{L}'
    atext = "[#{letter}]"
    assert_nothing_raised { /#{atext}/ }
  end
end
