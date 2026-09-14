require 'test/unit'
require 'java'
require 'weakref'

# ObjectSpace::WeakMap holds keys and values weakly; only immediates stay pinned as keys.
class TestWeakMap < Test::Unit::TestCase
  LIVE = Object.new

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
    map[LIVE.object_id] = LIVE
    map[:sym] = LIVE
    map[nil] = LIVE
    3.times { java.lang.System.gc }
    assert_same LIVE, map[LIVE.object_id]
    assert_equal 3, map.size
    assert map.key?(:sym) && map.key?(nil)
  end

  private

  # separate frames so no interpreter temporary keeps the key or the WeakRef alive
  def add_unreachable_key(map, value)
    key = Object.new
    map[key] = value
    java.lang.ref.WeakReference.new(key)
  end

  def make_unreferenced_weakref(obj)
    java.lang.ref.WeakReference.new(WeakRef.new(obj))
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
