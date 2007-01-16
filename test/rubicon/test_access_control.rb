require 'test/unit'


class TestAccessControl < Test::Unit::TestCase

  module M1
    def m1private
      1
    end
    def m1protected
      2
    end
    def m1public
      3
    end
    private :m1private
    protected :m1protected
  end

  module M2
    def m2private
      1
    end
    def m2protected
      2
    end
    def m2public
      3
    end
    private :m2private
    protected :m2protected
    module_function :m2private, :m2protected, :m2public
  end

  class C1
    def c1private
      1
    end
    def c1protected
      2
    end
    def c1public
      3
    end
    private :c1private
    protected :c1protected
  end
   
  class C2 < C1
    def c2private
      c1private
    end
    def c2protected
      c1protected
    end
    def c2public
      c1public
    end
  end

  class C3 < C1
    def c3private
      self.c1private
    end
    def c3protected
      self.c1protected
    end
    def c3public
      self.c1public
    end
  end

  class C4
    include M1
  end

  class C5
    include M2
  end

  # --------------------------------------------------------

  def test_class_access

    # access via subclass
    c2 = C2.new
    assert_raise(NoMethodError) { c2.c1private }
    assert_raise(NoMethodError) { c2.c1protected }
    assert_equal(3, c2.c1public)
    assert_equal(1, c2.c2private)
    assert_equal(2, c2.c2protected)
    assert_equal(3, c2.c2public)

    # access via subclass with explicit 'self'
    c3 = C3.new
    assert_raise(NoMethodError) { c3.c3private }
    assert_equal(2, c3.c3protected)
    assert_equal(3, c3.c3public)
  end

  # --------------------------------------------------------

  def test_included_module
    c4 = C4.new
    assert_raise(NoMethodError) { c4.m1private }
    assert_raise(NoMethodError) { c4.m1protected }
    assert_equal(3, c4.m1public)
  end

  # --------------------------------------------------------

  def test_module_functions
    assert_equal(1, M2.m2private)
    assert_equal(2, M2.m2protected)
    assert_equal(3, M2.m2public)
    c5 = C5.new
    assert_raise(NoMethodError) { c5.m2private }
    assert_raise(NoMethodError) { c5.m2protected }
    assert_raise(NoMethodError) { c5.m2public }
  end
end
