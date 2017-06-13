require 'ffi'
require 'test/unit'

class TestFFIMemoryLeak < Test::Unit::TestCase
  java_import java.lang.management.ManagementFactory
  def test_4625
    5.times { JRuby.gc }
    initial = ManagementFactory.memory_mx_bean.non_heap_memory_usage.used
    i = 0
    10_000_000.times do
      ptr = FFI::MemoryPointer.from_string('some random test string')
      ptr.free
      i += 1
    end
    5.times { JRuby.gc }
    memory = ManagementFactory.get_memory_mx_bean.get_non_heap_memory_usage.used
    assert (memory - initial) / initial < 0.10, "memory after allocations was more than 10% higher"
  end
end
