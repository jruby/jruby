require 'test/unit'

class TestStruct < Test::Unit::TestCase
  def test_new_struct
    c = Struct.new(:c) do
      def foo; 1; end
    end
    assert_equal c.new.foo, 1
  end
end
