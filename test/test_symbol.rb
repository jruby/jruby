require 'test/unit'

# JRuby-specific symbol tests
class TestSymbol < Test::Unit::TestCase
  # JRUBY-2379
  def test_inspect_with_dollar_2
    assert_equal(":$ruby", :$ruby.inspect)
    assert_equal(":$_", :$_.inspect)
  end

  # JRUBY-4780
  def test_symbol_to_proc_inject
    assert_equal 15, [1,2,3,4,5].inject(&:+)
  end
end
