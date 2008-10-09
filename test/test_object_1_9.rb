require 'test/unit'

class TestObject19 < Test::Unit::TestCase
  def test_tap
    value = nil
    1.tap { |v| value = v }
    assert_equal 1, value

    [].tap { |v| value = v }
    assert_equal [], value

    assert_equal 1, 1.tap { }

    obj = Object.new

    assert_equal obj, obj.tap { }

    assert_equal "str", "str".tap { value = "foo" }
    assert_equal "foo", value

    assert_raises(LocalJumpError) do 
      "str".tap
    end
  end
end