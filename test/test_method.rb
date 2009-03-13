require 'test/unit'

class TestMethod < Test::Unit::TestCase
  # JRUBY-3491: NullPointerException when calling #arity on Klass.method(:new)
  def test_jruby_3491
    assert_nothing_raised do
      String.method(:new).arity
    end
  end
end