require 'jruby'
require 'test/unit'

class TestObjectSpace < Test::Unit::TestCase
  def setup
    @objectspace = JRuby.objectspace
    JRuby.objectspace = true
  end
  
  def teardown
    JRuby.objectspace = @objectspace
  end
  
  def test_id2ref
    # Normal objects
    o1 = "hey"
    o2 = "ho"
    id1 = o1.object_id
    id2 = o2.object_id
    assert_equal(o1, ObjectSpace._id2ref(id1))
    assert_equal(o2, ObjectSpace._id2ref(id2))

    # Fixnums
    o1 = 17
    o2 = 100001
    id1 = o1.object_id
    id2 = o2.object_id
    assert_equal(o1, ObjectSpace._id2ref(id1))
    assert_equal(o2, ObjectSpace._id2ref(id2))

    assert_equal(1, 0.object_id)
    assert_equal(3, 1.object_id)
    assert_equal(201, 100.object_id)
    assert_equal(-1, -1.object_id)
    assert_equal(-19, -10.object_id)

    assert_equal(0, false.object_id)
    assert_equal(2, true.object_id)
    assert_equal(4, nil.object_id)

    assert_equal(false, ObjectSpace._id2ref(0))
    assert_equal(true, ObjectSpace._id2ref(2))
    assert_equal(nil, ObjectSpace._id2ref(4))
  end
  
  def test_jruby_objectspace
    JRuby.objectspace = false
    assert_equal(false, JRuby.objectspace)
    JRuby.objectspace = true
    assert_equal(true, JRuby.objectspace)
  end
  
  # JRUBY-4330
  def test_object_id_same_with_objectspace
    obj = "wahoo"
    id_with_objectspace = obj.object_id
    JRuby.objectspace = false
    id_without_objectspace = obj.object_id
    assert_equal(id_with_objectspace, id_without_objectspace)
  end

  def finalizer(results)
    proc do |i|
      results << "finalizing #{i}"
    end
  end

  # JRUBY-4839
  def test_finalization
    [true, false].each do |objectspace|
      JRuby.objectspace = objectspace
      obj1 = "lemon"
      obj2 = "apple"
      results = []

      ObjectSpace.define_finalizer obj1, finalizer(results)
      ObjectSpace.define_finalizer obj2, finalizer(results)

      obj1_id = obj1.object_id

      ObjectSpace.undefine_finalizer obj2

      obj1 = nil
      obj2 = nil

      t = Time.now
      (JRuby.gc; sleep 0.1) until (Time.now - t > 5) || results.length > 0
      assert_equal ["finalizing #{obj1_id}"], results
    end
  end
end
