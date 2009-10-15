require 'test/unit'

class TestDelegatedArrayEquals < Test::Unit::TestCase
  class Foo
     def initialize
      @ary = [1,2,3]
    end
  
    def ==(other)
      @ary == other
    end
  
    def to_ary
      @ary
    end
  end
  
  class Foo2
     def initialize
      @ary = [1,2,3]
    end
  
    def ==(other)
      @ary == other
    end
  end

  def test_delegated_array_equals
    a = Foo.new
    assert_equal(a, a)
    assert(a == a)
  end

  def test_badly_delegated_array_equals
    a = Foo2.new
    assert_not_equal(a, a)
    assert(!(a == a))
  end
end
