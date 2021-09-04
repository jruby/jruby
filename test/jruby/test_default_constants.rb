require 'test/unit'

class TestDefaultConstants < Test::Unit::TestCase
  def test_values
    assert_equal('jruby', RUBY_ENGINE)
  end
end
