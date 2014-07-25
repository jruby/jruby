require 'test/unit'

class TestProcVisibility < Test::Unit::TestCase
  class TestVis
    public
    p1 = proc { private }
    p2 = proc { public }

    public
    def foo; end

    p1.call
    def foo; end

    p2.call
    def foo; end
  end

  def test_proc_can_not_change_visibility
    tv = TestVis.new
    assert_nothing_raised { tv.foo }
    assert_nothing_raised { tv.foo }
    assert_nothing_raised { tv.foo }
  end

  class TestVis2
    public
    def foo; end

    1.times { private }
    def foo; end

    1.times { public }
    def foo; end
  end

  def test_block_can_not_change_visibility
    tv = TestVis2.new
    assert_nothing_raised { tv.foo }
    assert_nothing_raised { tv.foo }
    assert_nothing_raised { tv.foo }
  end
end
