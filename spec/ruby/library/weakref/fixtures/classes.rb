require 'weakref'

# From MRI test_weakref.rb
class WeakRefSpec
  def self.make_weakref(level = 10)
    if level > 0
      make_weakref(level - 1)
    else
      WeakRef.new(Object.new)
    end
  end

  def self.make_dead_weakref
    weaks = []
    weak = nil
    1000.times do
      weaks << make_weakref
    end

    1000.times do
      GC.start
      break if weak = weaks.find { |w| !w.weakref_alive? }
    end
    weak
  end

  # A WeakRef to obj that nothing references afterwards; the map holds it weakly as a
  # value so its collection can be observed from Ruby without keeping it alive.
  def self.put_unreferenced_weakref(map, obj)
    map[0] = WeakRef.new(obj)
    nil
  end

  # GC.start is only a hint on some implementations, so churn garbage between attempts
  # until the block holds (true) or the attempts run out (false).
  def self.collect_until(attempts = 1000)
    attempts.times do
      GC.start
      return true if yield
      @churn = Array.new(100) { "x" * 25_000 }
    end
    false
  end
end
