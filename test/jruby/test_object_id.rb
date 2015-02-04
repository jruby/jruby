require 'test/unit'

class TestObjectId < Test::Unit::TestCase
  def test_basic_object_does_not_respond_to_object_id
    o = BasicObject.new

    o.__id__
    assert_raises(NoMethodError) { o.object_id }
  end

  def test_dup_object_gets_new_object_id
    o = Object.new
    o.object_id
    o.instance_variable_set(:@foo, "foo")

    assert_not_equal o.object_id, o.dup.object_id
  end
end
