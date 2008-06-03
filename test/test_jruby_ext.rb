require 'test/unit'
require 'jruby/ext'

class TestJrubyExt < Test::Unit::TestCase
  # eval these to ensure they're interpreted
  eval "
  def foo1(a, b, c = 1, d = 2, *e, &f); end
  def foo2; end
  "
  define_method(:foo3) {}
  
  def test_method_args
    assert_equal([[:a, :b], [:c, :d], :e, :f], method(:foo1).args)
    assert_equal([[], [], nil, nil], method(:foo2).args)
    assert_raise(TypeError) { method(:foo3).args }
  end
end