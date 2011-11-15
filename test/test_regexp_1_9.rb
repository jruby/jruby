# encoding: UTF-8
require 'test/unit'

class TestRegex19 < Test::Unit::TestCase
  def test_to_s
    assert_equal(/(?-mix:foo|bar)/, /#{Regexp.union("foo", "bar")}/)
  end

  # JRUBY-5622
  def test_dstr_with_unicode_class
    letter = '\p{L}'
    atext = "[#{letter}]"
    assert_nothing_raised { /#{atext}/ }
  end

  # JRUBY-5924
  def test_matchdata_end_position_on_unicode_strings
    rails_scriptlet_re = /(?m-ix:<%(=+|-|\#|%)?(.*?)([-=])?%>([ \t]*\r?\n)?)/
    match = rails_scriptlet_re.match "<%= @user %> á"

    assert_equal 12, match.end(0)
  end

  def test_dregexp_with_escape_char
    re_quoted_string = %r{"\x80"}n
    assert_nothing_raised { %r{#{re_quoted_string}}n }
  end

  def test_matchdata_begin_position_on_unicode_strings
    rails_scriptlet_re = regexp = /(.)(.)(\d+)(\d)/m
    match = rails_scriptlet_re.match "TEXG»234"
    assert_equal 3, match.begin(0)
  end
end
