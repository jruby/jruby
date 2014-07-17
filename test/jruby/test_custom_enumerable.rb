require 'test/unit'

class TestCustomEnumerable < Test::Unit::TestCase
  class CustomEnumerable
    include Enumerable
    def each; yield 1; yield 2; yield 3; end
  end

  # JRUBY-4161
  def test_entries
    assert_equal [1,2,3], CustomEnumerable.new.entries
    assert_equal [1,2,3], CustomEnumerable.new.entries(*[])
  end
end
