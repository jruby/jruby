require 'test/unit'

## NOTE: Most of the tests that were here have been moved to the RubySpec.

class TestArray < Test::Unit::TestCase
  ##### splat test #####
  class ATest
    def to_a; 1; end
  end

  def test_splatting
    proc { |a| assert_equal(1, a) }.call(*1)
    assert_raises(TypeError) { proc { |a| }.call(*ATest.new) }
  end

  def test_initialize_on_frozen_array
    assert_raises(TypeError) {  
      [1, 2, 3].freeze.instance_eval { initialize }
    }
  end

  # JRUBY-4157
  def test_shared_ary_slice
    assert_equal [4,5,6], [1,2,3,4,5,6].slice(1,5).slice!(2,3)
  end
end
