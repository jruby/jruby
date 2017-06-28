require 'test/unit'

class TestReifiedVariables < Test::Unit::TestCase
  def test_superclass_subclass
    foo_class = eval "
      module GH3950
        class Foo
          attr_accessor :name

          def initialize(name)
            self.name = name
          end

          def full_name
            name.to_s + 'baz'
          end
        end
      end
      GH3950::Foo
    "

    # Force it to reify and fully cache
    1000.times {
      foo_class.new('foo').full_name
    }
    assert_equal("foobaz", foo_class.new('foo').full_name)

    bar_class = eval "
      module GH3950
        class Bar < Foo
          def initialize(name)
            @name = name
          end
        end
      end
      GH3950::Bar
    "

    # Force it to reify and full cache
    1000.times {
      bar_class.new('foo').full_name
    }

    assert_equal("foobaz", bar_class.new('foo').full_name)
  end
end