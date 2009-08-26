require 'test/unit'

class TestRegex19 < Test::Unit::TestCase
  def test_to_s
    assert_equal /(?-mix:foo|bar)/, /#{Regexp.union("foo", "bar")}/
  end
end
