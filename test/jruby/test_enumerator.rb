require 'test/unit'
require 'test/jruby/test_helper'

class TestEnumerator < Test::Unit::TestCase
  include TestHelper

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

    enum = a.map
    enum.next; enum.next
    exc = assert_raise(StopIteration) { enum.next }
    assert_equal([nil, nil], exc.result)
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

  # Enumerator's Java Support :

  IterationException = StopIteration # java.util.NoSuchElementException

  def test_java_iterator
    arr = [1, 2, 3]

    assert_equal 3, arr.each_with_index.size

    iter = arr.each_with_index.to_java
    assert iter.is_a?(java.util.Iterator)
    assert_equal([1, 0], iter.next)
    assert_equal([2, 1], iter.next)
    assert_equal([3, 2], iter.next)
    assert_raise(IterationException) { iter.next }

    iter = [ 1 ].each_with_index.to_java
    assert iter.hasNext; assert iter.hasNext; iter.next
    assert ! iter.hasNext
    assert_raise(IterationException) { iter.next }
    assert ! iter.hasNext

    enum = Enumerator.new do |out|
      a = 1 ; loop do
        raise StopIteration if a > 10
        out << a; (a = a * 2)
      end
    end
    iter = enum.to_java
    assert_equal nil, enum.size

    assert iter.hasNext; assert_equal 1, iter.next
    assert iter.hasNext; assert iter.hasNext; assert_equal 2, iter.next
    assert_equal 4, iter.next
    assert iter.hasNext; assert_equal 8, iter.next
    assert ! iter.hasNext
    assert_raise(IterationException) { iter.next }
    assert ! iter.hasNext
  end

  def test_java_iterator_array
    arr = [1, 2, 3]
    iter = arr.each.to_java
    assert iter.is_a?(java.util.Iterator)
    assert_equal(1, iter.next)
    assert_equal(2, iter.next)
    assert_equal(3, iter.next)
    ex = assert_raise(IterationException) { iter.next }
    #assert_equal(StopIteration, ex.cause.exception.class)
    #assert_equal(arr, ex.cause.exception.result)

    enum = arr.each
    assert_equal 3, enum.size

    iter = enum.to_java
    assert_equal 1, iter.next
    assert iter.hasNext; assert_equal 2, iter.next
    assert iter.hasNext; assert iter.hasNext; assert_equal 3, iter.next
    assert ! iter.hasNext; assert ! iter.hasNext
    assert_raise(IterationException) { iter.next }
    assert ! iter.hasNext; assert ! iter.hasNext
  end

  def test_java8_streaming_array
    arr = (1..200).to_a
    enum = arr.each
    stream = enum.to_java.stream
    # assert_equal 200, stream.count
    list = stream.limit(100).map { |el| el + 1000 }.collect(java.util.stream.Collectors.toList)
    assert_equal 1001, list[0]
    assert_equal 1100, list[99]
    assert_equal 100, list.count
  end

  def test_java8_streaming
    enum = Enumerator.new do |out|
      i = 1 ; loop do
        raise StopIteration if i > 100
        out << i * 10; (i += 1)
      end
    end
    enum2 = enum.dup

    stream = enum.to_java.stream
    list = stream.limit(100).map { |el| el + 1 }.collect(java.util.stream.Collectors.toList)
    assert_equal 11, list[0]
    assert_equal 1001, list[99]
    assert_equal 100, list.count

    stream = java.util.stream.StreamSupport.stream enum2.to_java.spliterator, false
    set = stream.map { |el| el % 99 }.collect(java.util.stream.Collectors.toSet)

    assert_equal 99, set.size
    assert set.include?(98)
    assert ! set.include?(99)
  end

  protected

  def assert_raise(klass = StandardError, &block)
    if klass.ancestors.include?(java.lang.Throwable)
      begin
        yield
      rescue java.lang.Throwable => ex
        return ex if ex.is_a?(klass)
        raise ex
      end
    end
    super
  end

end