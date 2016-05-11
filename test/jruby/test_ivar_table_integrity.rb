require 'test/unit'

class TestIvarTableIntegrity < Test::Unit::TestCase
  def test_ivar_table_integrity
    cls = Class.new do
      def initialize; @foo = nil; end
      attr_accessor :foo
      attr_accessor :bar
      def remove_foo; remove_instance_variable :@foo; end
    end

    obj = cls.new
    obj.foo = 1
    assert_equal 1, obj.foo
    obj.remove_foo
    obj.bar = 2
    assert_equal nil, obj.foo
    assert_equal 2, obj.bar
    obj.foo = 3
    assert_equal 3, obj.foo
    assert_equal 2, obj.bar
  end
end

