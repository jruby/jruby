require 'test/unit'

class TestObjectId < Test::Unit::TestCase
  def test_dup_object_gets_new_object_id
    o = Object.new
    o.__id__
    o.instance_variable_set(:@foo, "foo")

    assert_not_equal o.__id__, o.dup.__id__
  end
end
