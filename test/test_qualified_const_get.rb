# FIXME: remove this file when the Ruby 2.0 tests are pulled into the JRuby source tree

if RUBY_VERSION >= "2.0"
  require "test/unit"

  class TestQualifiedConstGet < Test::Unit::TestCase
    module X
      module Y
      end

      Z = 123
    end

    def test_nested_get
      assert_equal Test::Unit::TestCase, Object.const_get("Test::Unit::TestCase")
      assert_equal X::Y, self.class.const_get("X::Y")
    end

    def test_nested_get_symbol
      assert_equal Test::Unit::TestCase, Object.const_get(:"Test::Unit::TestCase")
      assert_equal X::Y, self.class.const_get(:"X::Y")
    end

    def test_nested_get_const_missing
      classes = []
      klass = Class.new {
        define_singleton_method(:const_missing) { |name|
          classes << name
          klass
        }
      }
      klass.const_get("Foo::Bar::Baz")
      assert_equal [:Foo, :Bar, :Baz], classes
    end

    def test_nested_bad_class
      assert_raises(TypeError) do
        self.class.const_get([X, 'Z', 'Foo'].join('::'))
      end
    end
  end
end
