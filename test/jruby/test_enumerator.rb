require 'test/unit'

class TestEnumerator < Test::Unit::TestCase

  def test_stop_result_array
    a = [1, 2]
    enum = a.each
    assert_equal(1, enum.next)
    assert_equal(2, enum.next)
    exc = assert_raise(StopIteration) { enum.next }
    assert_equal(a, exc.result)

    exc = assert_raise(StopIteration) { enum.next }
    assert_equal(a, exc.result)
    #assert_equal('iteration reached an end', exc.message)

    #enum = a.map
    #enum.next; enum.next
    #exc = assert_raise(StopIteration) { enum.next }
    #assert_equal([nil, nil], exc.result)
    #assert_equal('iteration reached an end', exc.message)
  end

  def test_stop_result_obj
    o = Object.new
    def o.each
      yield
      yield 1
      yield 1, 2
    end
    enum = o.to_enum
    assert_equal(nil, enum.next)
    assert_equal(1, enum.next)
    assert_equal([1,2], enum.next)
    exc = assert_raise(StopIteration) { enum.next }
    assert_equal(nil, exc.result)
    assert_equal('iteration reached an end', exc.message)

    o = Object.new
    def o.each
      yield 1
      yield 2
      100
    end
    enum = o.to_enum
    assert_equal(1, enum.next)
    assert_equal(2, enum.next)
    exc = assert_raise(StopIteration) { enum.next }
    assert_equal(100, exc.result)
    assert_equal('iteration reached an end', exc.message)
  end

  def test_stop_result_explicit
    fib = Enumerator.new do |y|
      a = b = 1
      loop do
        if a < 10 # [1, 1, 2, 3, 5, 8, 13]
          y << a
          a, b = b, a + b
        else
          raise StopIteration
        end
      end
      :done
    end
    assert_equal [1, 1, 2, 3, 5, 8], fib.take(8)
    fib.next
    fib.next
    fib.next
    fib.next
    fib.next
    fib.next
    exc = assert_raise(StopIteration) { fib.next }
    assert_equal(:done, exc.result)
  end

end