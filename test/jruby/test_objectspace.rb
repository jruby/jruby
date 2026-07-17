require 'test/unit'

class TestObjectSpace < Test::Unit::TestCase

  def setup
    require 'jruby'
    @objectspace = JRuby.objectspace
    JRuby.objectspace = true
  end

  def teardown
    JRuby.objectspace = @objectspace
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

  def body(objectspace, results)
    JRuby.objectspace = objectspace
    obj1 = "lemon"
    obj2 = "apple"

    ObjectSpace.define_finalizer obj1, finalizer(results)
    ObjectSpace.define_finalizer obj2, finalizer(results)

    results << obj1.object_id

    ObjectSpace.undefine_finalizer obj2

    obj1 = nil
    obj2 = nil
  end

  # JRUBY-4839 GH #3028
  def test_finalization
    [true, false].each do |objectspace|
      results = []

      body(objectspace, results)

      t = Time.now
      (JRuby.gc; sleep 0.1) until (Time.now - t > 5) || results.length > 1
      assert_equal "finalizing #{results[0]}", results[1]
    end
  end

  # See rails/rails#22376.
  def test_each_object_singleton_class
    # disable objectspace; we want this to always work
    old_objectspace = JRuby.objectspace
    JRuby.objectspace = false

    a = Class.new
    b = Class.new(a)
    c = Class.new(a)
    d = Class.new(b)

    classes = ObjectSpace.each_object(a.singleton_class).to_a
    assert_equal(classes.sort_by(&:object_id), [a, b, c, d].sort_by(&:object_id))
  ensure
    JRuby.objectspace = old_objectspace
  end
end
