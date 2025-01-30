require 'test/unit'

class TestClassSubclasses < Test::Unit::TestCase
  def test_concurrent_modification
    c = Class.new
    c2 = Class.new(c)
    go = false

    thread_count = 100
    threads = thread_count.times.map {
      Thread.new {
        Thread.pass until go
        c3 = Class.new(c2)
        o = c3.new
        c.class_eval {
          def foo; end
        }
        class << o
          extend Enumerable
          prepend Comparable
        end
        c.class_eval {
          def foo; end
        }
        c3.class_eval {
          include Enumerable
          prepend Comparable
        }
        c.class_eval {
          def foo; end
        }
        :success
      }
    }

    go = true
    assert_equal(threads.map(&:value), [:success] * thread_count)
  end
end
