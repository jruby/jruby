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

  # JRUBY-5141
  def test_not_equal
    o = Object.new
    def o.==(rhs)
      true # always true
    end
    assert_equal(true, (o == o)) # always true
    assert_equal(true, (o == 1)) # always true
    assert_equal(false, (o != o)) # always false
    assert_equal(false, (o != 1)) # always false
  end
end
