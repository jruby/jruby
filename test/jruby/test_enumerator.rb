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

  public

  def test_yielder
    ary = Enumerator.new { |y| y.yield([1]) }.to_a
    assert_equal [[1]], ary
    ary = Enumerator.new { |y| y.yield([1]) }.lazy.to_a
    assert_equal [[1]], ary
    ary = Enumerator.new { |y| y << [1] }.to_a
    assert_equal [[1]], ary

    yields = []
    y = Enumerator::Yielder.new { |args| yields << args }
    y << [1]
    assert_equal [[1]], yields

    assert_equal 42, Enumerator.new { |y| y << 42 }.first
    assert_equal [42], Enumerator.new { |y| y << [42] }.first
    assert_equal [[42]], Enumerator.new { |y| y << [42] }.first(1)

    assert_equal [], Enumerator.new { |y| y << [] }.first
    assert_equal [], Enumerator.new { |y| y.yield [] }.first
    assert_equal [1], Enumerator.new { |y| y.yield [1] }.first
  end

  def test_yield_map # GH-4108
    ary = Enumerator.new { |y| y.yield([1]) }.to_a
    assert_equal [[1]], ary
    ary = Enumerator.new { |y| y.yield([1]) }.map { |e| e }.to_a
    assert_equal [[1]], ary
    ary = Enumerator.new { |y| y.yield([1]) }.lazy.map { |e| e }.to_a
    # NOTE: this one seems still failing (enumerable.lazy.map works)
    # assert_equal [[1]], ary
  end

  def test_lazy_map_enumerable # GH-5044
    a = [1, 2, 3].map { |a| [a + 1] }.to_a
    assert_equal [[2], [3], [4]], a
    a = [1, 2, 3].lazy.map { |a| [a + 1] }.to_a
    assert_equal [[2], [3], [4]], a
    a = [[1], [2], [3]].lazy.map(&:itself).to_a
    assert_equal [[1], [2], [3]], a
  end

  def test_each_arg
    enum = Enumerator.new { |y, arg| y.yield([1]); y.yield(arg) }
    assert_equal [[1], :foo], enum.each(:foo).to_a
    assert_equal [[1], :foo], enum.each(:foo, :bar).to_a
    assert_equal [[1], []], enum.each([]).to_a
    assert_equal [[1], [2]], enum.each([2]).to_a
    assert_equal [[1], nil], enum.each.to_a
    enum = Enumerator.new { |y, *arg| y.yield([1]); y.yield(*arg) }
    assert_equal [[1], :foo], enum.each(:foo).to_a
    assert_equal [[1], []], enum.each([]).to_a
    assert_equal [[1], [2]], enum.each([2]).to_a
    assert_equal [[1], nil], enum.each.to_a
    assert_equal [[1], [:foo, :bar]], enum.each(:foo, :bar).to_a
  end

  def test_zip # from MRI suite -> JRuby disabled the test (due very last assert)
    @obj = Object.new
    class << @obj
      include Enumerable
      def each
        yield 1
        yield 2
        yield 3
        yield 1
        yield 2
        self
      end
    end

    assert_equal([[1,1],[2,2],[3,3],[1,1],[2,2]], @obj.zip(@obj))
    assert_equal([["a",1],["b",2],["c",3]], ["a", "b", "c"].zip(@obj))

    a = []
    result = @obj.zip([:a, :b, :c]) {|x,y| a << [x, y] }
    assert_nil result
    assert_equal([[1,:a],[2,:b],[3,:c],[1,nil],[2,nil]], a)

    a = []
    cond = ->((x, i), y) { a << [x, y, i] }
    @obj.each_with_index.zip([:a, :b, :c], &cond)
    assert_equal([[1,:a,0],[2,:b,1],[3,:c,2],[1,nil,3],[2,nil,4]], a)

    a = []
    @obj.zip({a: "A", b: "B", c: "C"}) {|x,y| a << [x, y] }
    assert_equal([[1,[:a,"A"]],[2,[:b,"B"]],[3,[:c,"C"]],[1,nil],[2,nil]], a)

    ary = Object.new
    def ary.to_a;   [1, 2]; end
    assert_raise(TypeError) {%w(a b).zip(ary)}
    def ary.each; [3, 4].each{|e|yield e}; end
    assert_equal([[1, 3], [2, 4], [3, nil], [1, nil], [2, nil]], @obj.zip(ary))
    def ary.to_ary; [5, 6]; end
    assert_equal([[1, 5], [2, 6], [3, nil], [1, nil], [2, nil]], @obj.zip(ary))
    # obj = eval("class C\u{1f5ff}; self; end").new
    # assert_raise_with_message(TypeError, /C\u{1f5ff}/) {(1..1).zip(obj)}
  end

  def test_take
    assert_equal [[]], [[]].to_enum.take(1)
    assert_equal [[]], [[]].to_enum.take(3)
    assert_equal [], [[]].to_enum.take(0)

    a = [[1], 2, [[3]], [4]]
    assert_equal [[1]], a.to_enum.take(1)
    assert_equal [[1], 2, [[3]]], a.to_enum.take(3)
    assert_equal a, a.to_enum.take(10)

    assert_equal [], [].to_enum.take(1)
  end

  def test_any
    assert_equal true, [[]].to_enum.any?
    assert_equal true, [[], []].to_enum.any?
    assert_equal false, [].to_enum.any?
    assert_equal true, [1].to_enum.any?
    assert_equal true, [[1], 2, [[3]], [4]].to_enum.any?
    
    assert_equal false, [].to_enum.any? {|x| x == []}
    assert_equal true, [[]].to_enum.any? {|x| x == []}
    assert_equal true, [1].to_enum.any? {|x| x == 1}
    assert_equal false, [1].to_enum.any? {|x| x == []}

    assert_equal true, [1].to_enum.any?(1)
    assert_equal false, [1].to_enum.any?(0)
    assert_equal true, [0, 1].to_enum.any?(1)
  end

end