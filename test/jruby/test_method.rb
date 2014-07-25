require 'test/unit'

class TestMethod < Test::Unit::TestCase
  # JRUBY-3491: NullPointerException when calling #arity on Klass.method(:new)
  def test_jruby_3491
    assert_nothing_raised do
      String.method(:new).arity
    end
  end

  def test_function_break
    obj = Object.new
    def obj.broken_method
      break
    end
    assert_raise(LocalJumpError){obj.broken_method}
  end
end