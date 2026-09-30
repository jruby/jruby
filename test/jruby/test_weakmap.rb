require 'test/unit'
require 'java'
require 'weakref'

# ObjectSpace::WeakMap holds keys and values weakly. Integer, Float, Symbol and nil keys stay
# and compare by value, as on CRuby, where such immediates are never collected.
class TestWeakMap < Test::Unit::TestCase
  LIVE = Object.new
  INTEGERS = [0, 1, -1, 42, -42, 2**30 + 1, -(2**30 + 1), 2**62 - 1, -(2**62)]
  FLOATS = [1.5, -1.5, 0.0, 100.25, 3.0]

  def test_key_is_collected
    map = ObjectSpace::WeakMap.new
    ref = add_unreachable_key(map, LIVE)
    assert gc_until { ref.refersTo(nil) && map.size == 0 }, "key stayed alive: size=#{map.size}"
    assert_equal [], map.keys
    map.each { |k, v| flunk "#{k.inspect} => #{v.inspect} survived its key" }
  end

  def test_weakref_instance_is_collected
    ref = make_unreferenced_weakref(LIVE)
    assert gc_until { ref.refersTo(nil) }, "WeakRef instance stayed alive"
  end

  def test_immediate_keys_stay
    map = ObjectSpace::WeakMap.new
    keys = [LIVE.object_id, 2.5, :sym, nil]
    keys.each { |k| map[k] = LIVE }
    lookups = -> { [LIVE.object_id, 2.5 + 0, :sym, nil] } # equal values, fresh number objects
    sentinel = dropped_sentinel
    collected = gc_until do
      lookups.call.each { |k| assert_same LIVE, map[k], "key #{k.inspect} lost before a collection was seen" }
      sentinel.refersTo(nil)
    end
    assert collected, "sentinel stayed alive: no collection observed"
    lookups.call.each { |k| assert_same LIVE, map[k], "key #{k.inspect} lost after a collection" }
    assert_equal keys.size, map.size
  end

  def test_integer_keys
    map = ObjectSpace::WeakMap.new
    INTEGERS.each { |i| map[i] = LIVE }
    INTEGERS.each { |i| assert_same LIVE, map[i + 0], "key #{i}" }
    assert_equal INTEGERS.size, map.size
  end

  def test_equal_integers_computed_apart_share_an_entry
    map = ObjectSpace::WeakMap.new
    map[large_integer] = LIVE
    assert_same LIVE, map[large_integer]
    assert map.key?(large_integer)
    assert_equal 1, map.size
  end

  def test_float_keys
    map = ObjectSpace::WeakMap.new
    FLOATS.each { |f| map[f] = LIVE }
    FLOATS.each { |f| assert_same LIVE, map[f + 0.0], "key #{f}" }
    assert_equal FLOATS.size, map.size
  end

  def test_equal_floats_computed_apart_share_an_entry
    map = ObjectSpace::WeakMap.new
    map[0.5 + 1.0] = LIVE
    assert_same LIVE, map[3.0 / 2]
    assert_equal 1, map.size
  end

  def test_integer_and_float_of_equal_value_are_distinct_keys
    map = ObjectSpace::WeakMap.new
    map[1] = "integer"
    map[1.0] = "float"
    assert_equal "integer", map[1]
    assert_equal "float", map[1.0]
    assert_equal 2, map.size
  end

  def test_bignum_keys_compare_by_identity
    map = ObjectSpace::WeakMap.new
    big = bignum
    map[big] = LIVE
    assert_same LIVE, map[big]
    assert_nil map[bignum]
    assert_equal 1, map.size
  end

  def test_numeric_key_replacement
    map = ObjectSpace::WeakMap.new
    map[7] = "old"
    assert_equal "new", (map[7] = "new")
    assert_equal "new", map[7]
    assert_equal 1, map.size
  end

  def test_numeric_key_delete
    map = ObjectSpace::WeakMap.new
    map[7] = "seven"
    assert_equal "seven", map.delete(7)
    assert_nil map.delete(7)
    assert_equal [:missing, 7], map.delete(7) { |k| [:missing, k] }
    assert_false map.key?(7)
    assert_equal 0, map.size
  end

  def test_numeric_key_membership
    map = ObjectSpace::WeakMap.new
    map[7] = LIVE
    map[2.5] = LIVE
    [:key?, :include?, :member?].each do |m|
      assert map.send(m, 7), m.to_s
      assert map.send(m, 2.5), m.to_s
      assert_false map.send(m, 8), m.to_s
      assert_false map.send(m, 3.5), m.to_s
    end
  end

  def test_numeric_keys_listed_with_object_keys
    map = ObjectSpace::WeakMap.new
    obj = Object.new
    map[7] = "seven"
    map[2.5] = "float"
    map[obj] = "object"
    assert_equal 3, map.size
    assert_equal 3, map.keys.size
    assert map.keys.include?(7) && map.keys.include?(2.5) && map.keys.include?(obj)
    assert_equal %w[float object seven], map.values.sort
    pairs = {}
    map.each { |k, v| pairs[k] = v }
    assert_equal({ 7 => "seven", 2.5 => "float", obj => "object" }, pairs)
  end

  def test_numeric_key_entry_dies_with_its_value
    map = ObjectSpace::WeakMap.new
    ref = put_dropped_value(map, 7)
    assert gc_until { ref.refersTo(nil) && !map.key?(7) }, "value stayed alive: key?=#{map.key?(7)}"
    assert_nil map[7]
    assert_equal 0, map.size
  end

  def test_object_keys_go_while_numeric_keys_stay
    map = ObjectSpace::WeakMap.new
    map[7] = LIVE
    map[2.5] = LIVE
    ref = add_unreachable_key(map, LIVE)
    assert_equal 3, map.size
    assert gc_until { ref.refersTo(nil) && map.size == 2 }, "object key stayed alive: size=#{map.size}"
    assert_same LIVE, map[7]
    assert_same LIVE, map[2.5]
  end

  private

  # separate frames so no interpreter temporary keeps the key, the value or the WeakRef alive
  def add_unreachable_key(map, value)
    key = Object.new
    map[key] = value
    java.lang.ref.WeakReference.new(key)
  end

  def put_dropped_value(map, key)
    value = Object.new
    map[key] = value
    java.lang.ref.WeakReference.new(value)
  end

  def make_unreferenced_weakref(obj)
    java.lang.ref.WeakReference.new(WeakRef.new(obj))
  end

  # an object nothing holds strongly; once it is cleared a collection has certainly run
  def dropped_sentinel
    java.lang.ref.WeakReference.new(Object.new)
  end

  # fresh Integer objects on each call: one outside the small-value cache, one Bignum
  def large_integer
    2**30 + 1
  end

  def bignum
    2**70
  end

  def gc_until(rounds = 10)
    rounds.times do
      java.lang.System.gc
      sleep 0.01
      return true if yield
    end
    false
  end
end
