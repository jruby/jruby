require 'test/unit'

class TestEval < Test::Unit::TestCase
  class ToStr; def to_str; ''; end; end
  class ToInt; def to_int; 1; end; end

  def test_args_coersion
    assert_raises(TypeError) { eval :foo }
    assert_raises(TypeError) { eval 1 }
    assert_nothing_raised { eval "" }
    assert_nothing_raised { eval ToStr.new }
    assert_raises(TypeError) { eval "", :foo }
    assert_raises(TypeError) { eval "", 1 }
    assert_nothing_raised { eval "", binding }
    assert_nothing_raised { eval "", proc{} }
    assert_raises(TypeError) { eval "", binding, :foo }
    assert_raises(TypeError) { eval "", binding, 1 }
    assert_nothing_raised { eval "", binding, 'foo' }
    assert_nothing_raised { eval "", binding, ToStr.new }
    assert_raises(TypeError) { eval "", binding, 'foo', 'foo' }
    assert_nothing_raised { eval "", binding, 'foo', 1 }
    assert_nothing_raised { eval "", binding, 'foo', :foo }
    assert_nothing_raised { eval "", binding, 'foo', ToInt.new }
  end
end
