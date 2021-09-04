require 'test/unit'

class TestInclude < Test::Unit::TestCase
  module X ; end

  class Q
    def foo arg = []
      arg << :Q
    end
  end

  class Y < Q ; include X  end

  module A
    def foo arg = []
      arg << :A ; super
    end
  end

  module X ; include A  end

  module Z
    def foo arg = []
      arg << :Z ; super
    end
  end

  class Y ; include Z  end

  class Y ; include X  end

  def test_include_order
    pend '[:A, :Z, :Q] on JRuby GH-1938' if defined? JRUBY_VERSION
    assert_equal [:Z, :A, :Q], Y.new.foo
  end

  module M1 ; V = 123  end
  module M2 ; V = 456  end

  class Foo
    include M1

    def self.get; return V end
  end

  def test_including_module_busts_constant_caches
    assert_equal 123, Foo.get
    Foo.send(:include, M2)
    assert_equal 456, Foo.get
  end

  class Bar
    include M1, M2
  end

  def test_multi_include
    assert_equal 123, Bar::V
    assert_equal Bar, Bar.send(:include)
    assert_equal Bar, Bar.send(:include, Comparable, Enumerable)

    assert_equal Object, Object.include
    assert_equal Object, Object.include(*[])
  end

  # JRUBY-3036
  def test_included_does_not_hit_each_class
    ObjectSpace.each_object(Class) do |cls|
      if cls < Top
        assert cls.top
      end
    end
  end

end

module IncludedTestCaseModule; end

class Top
  def self.top; true; end
end

class Next < Top
  include IncludedTestCaseModule
end