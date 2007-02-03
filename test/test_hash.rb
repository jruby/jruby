require 'test/unit'

class TestHash < Test::Unit::TestCase
  def test_clone_copies_default_initializer
    hash = Hash.new { |h, k| h[k] = 0 }
    clone = hash.clone
    assert_equal 0, clone[:test]
  end
end
