require 'test/unit'
require 'java'

class TestInstantiatingInterfaces < Test::Unit::TestCase

  class NoRun
    include java.lang.Runnable
  end

  class Runner
    include java.lang.Runnable
    def run; 'run' end
  end

  def test_include_mixin
    mixin = NoRun.new
    assert_raises(NoMethodError) { mixin.run }
    Runner.new.run
  end

  def test_impl_proc
    foo = nil
    java.lang.Runnable.impl do
      foo = "ran"
    end.run
    assert_equal("ran", foo)

    cs = java.lang.CharSequence.impl(:charAt) do |sym, index|
      assert_equal(:charAt, sym)
      assert_equal(0, index)
    end
    assert_nothing_raised { cs.charAt(0) }
    assert_raises(NoMethodError) { cs.length }

    calls = []
    cs = java.lang.CharSequence.impl(:charAt, :length) do |sym, *args|
      calls << sym; calls << args
      sym == :length ? 2 : ' '
    end
    assert_equal ' ', cs.charAt(0)
    assert_equal ' ', cs.charAt(1)
    assert_equal 2, cs.length

    assert_equal [ :charAt, [0], :charAt, [1], :length, [] ], calls

    def cs.length; 3 end
    def cs.char_at; nil end

    assert_equal 3, cs.length
    assert_equal ' ', cs.charAt(2)
  end

  # --- arity 0 -----------------------------------------------------------------

  def test_proc_to_java_functional_interface
    proc = proc { @ran = true }

    runnable = proc.to_java(java.lang.Runnable)
    runnable.run

    assert_equal true, @ran
    assert_same proc, runnable.__ruby_object
  end

  def test_proc_to_java_supplier # arity 0, reference return -> __ruby_call(Class)
    supplier = proc { 'forty-two' }.to_java(java.util.function.Supplier)
    assert_equal 'forty-two', supplier.get
  end

  def test_proc_to_java_callable # arity 0, reference return, can throw -> __ruby_call(Class)
    callable = proc { 42 }.to_java(java.util.concurrent.Callable)
    assert_equal 42, callable.call
  end

  def test_proc_to_java_int_supplier # arity 0, primitive return -> __ruby_call(Class)
    int_supplier = proc { 7 }.to_java(java.util.function.IntSupplier)
    assert_equal 7, int_supplier.getAsInt
  end

  def test_proc_to_java_long_supplier # arity 0, primitive long return
    long_supplier = proc { 1 << 40 }.to_java(java.util.function.LongSupplier)
    assert_equal 1 << 40, long_supplier.getAsLong
  end

  def test_proc_to_java_double_supplier # arity 0, primitive double return
    dbl_supplier = proc { 3.5 }.to_java(java.util.function.DoubleSupplier)
    assert_equal 3.5, dbl_supplier.getAsDouble
  end

  def test_proc_to_java_boolean_supplier # arity 0, primitive boolean return
    bool_supplier = proc { true }.to_java(java.util.function.BooleanSupplier)
    assert_equal true, bool_supplier.getAsBoolean
  end

  # --- arity 1 -----------------------------------------------------------------

  def test_proc_to_java_primitive_functional_interface # int -> int
    doubler = proc { |value| value * 2 }

    operator = doubler.to_java(java.util.function.IntUnaryOperator)

    assert_equal 42, operator.applyAsInt(21)
  end

  def test_proc_to_java_consumer # arity 1, void -> __ruby_call(Class, Object)
    captured = []
    consumer = proc { |x| captured << x }.to_java(java.util.function.Consumer)
    consumer.accept('a')
    consumer.accept('b')
    assert_equal ['a', 'b'], captured
  end

  def test_proc_to_java_function # arity 1 reference -> reference
    upcase = proc { |s| s.upcase }.to_java(java.util.function.Function)
    assert_equal 'HELLO', upcase.apply('hello')
  end

  def test_proc_to_java_predicate # arity 1, boolean return
    even = proc { |n| n.even? }.to_java(java.util.function.Predicate)
    assert_equal true, even.test(4)
    assert_equal false, even.test(5)
  end

  def test_proc_to_java_unary_operator # arity 1 reference -> reference
    rev = proc { |s| s.reverse }.to_java(java.util.function.UnaryOperator)
    assert_equal 'olleh', rev.apply('hello')
  end

  def test_proc_to_java_to_int_function # arity 1 reference -> primitive int
    len = proc { |s| s.length }.to_java(java.util.function.ToIntFunction)
    assert_equal 5, len.applyAsInt('hello')
  end

  def test_proc_to_java_int_function # arity 1 primitive int -> reference
    str = proc { |i| "n=#{i}" }.to_java(java.util.function.IntFunction)
    assert_equal 'n=3', str.apply(3)
  end

  def test_proc_to_java_int_consumer # arity 1 primitive int -> void
    seen = []
    consumer = proc { |i| seen << i }.to_java(java.util.function.IntConsumer)
    consumer.accept(1); consumer.accept(2)
    assert_equal [1, 2], seen
  end

  def test_proc_to_java_int_predicate # arity 1 primitive int -> primitive boolean
    pos = proc { |i| i > 0 }.to_java(java.util.function.IntPredicate)
    assert_equal true,  pos.test(1)
    assert_equal false, pos.test(-1)
  end

  def test_proc_to_java_long_unary_operator # arity 1 primitive long -> primitive long
    plus_one = proc { |l| l + 1 }.to_java(java.util.function.LongUnaryOperator)
    assert_equal 1 << 40 | 1 + 1, plus_one.applyAsLong(1 << 40 | 1)
  end

  def test_proc_to_java_double_unary_operator # arity 1 primitive double -> primitive double
    half = proc { |d| d / 2.0 }.to_java(java.util.function.DoubleUnaryOperator)
    assert_equal 1.5, half.applyAsDouble(3.0)
  end

  def test_proc_to_java_long_consumer # arity 1 primitive long -> void
    seen = []
    consumer = proc { |l| seen << l }.to_java(java.util.function.LongConsumer)
    consumer.accept(1 << 40); consumer.accept(-1)
    assert_equal [1 << 40, -1], seen
  end

  def test_proc_to_java_double_consumer # arity 1 primitive double -> void
    seen = []
    consumer = proc { |d| seen << d }.to_java(java.util.function.DoubleConsumer)
    consumer.accept(1.5); consumer.accept(-0.25)
    assert_equal [1.5, -0.25], seen
  end

  def test_proc_to_java_long_function # arity 1 primitive long -> reference
    fn = proc { |l| "L=#{l}" }.to_java(java.util.function.LongFunction)
    assert_equal 'L=99', fn.apply(99)
  end

  def test_proc_to_java_double_function # arity 1 primitive double -> reference
    fn = proc { |d| "D=#{d}" }.to_java(java.util.function.DoubleFunction)
    assert_equal 'D=2.5', fn.apply(2.5)
  end

  def test_proc_to_java_long_predicate # arity 1 primitive long -> primitive boolean
    pos = proc { |l| l > 0 }.to_java(java.util.function.LongPredicate)
    assert_equal true,  pos.test(1)
    assert_equal false, pos.test(-1)
  end

  def test_proc_to_java_double_predicate # arity 1 primitive double -> primitive boolean
    pos = proc { |d| d > 0 }.to_java(java.util.function.DoublePredicate)
    assert_equal true,  pos.test(0.1)
    assert_equal false, pos.test(-0.1)
  end

  def test_proc_to_java_int_to_long_function # arity 1 primitive int -> primitive long
    fn = proc { |i| i * (1 << 32) }.to_java(java.util.function.IntToLongFunction)
    assert_equal 3 * (1 << 32), fn.applyAsLong(3)
  end

  def test_proc_to_java_int_to_double_function # arity 1 primitive int -> primitive double
    fn = proc { |i| i + 0.5 }.to_java(java.util.function.IntToDoubleFunction)
    assert_equal 3.5, fn.applyAsDouble(3)
  end

  def test_proc_to_java_long_to_int_function # arity 1 primitive long -> primitive int
    fn = proc { |l| (l & 0xFF).to_i }.to_java(java.util.function.LongToIntFunction)
    assert_equal 0xAB, fn.applyAsInt(0x7FFF_FFFF_FFFF_FFAB)
  end

  def test_proc_to_java_long_to_double_function # arity 1 primitive long -> primitive double
    fn = proc { |l| l.to_f / 2 }.to_java(java.util.function.LongToDoubleFunction)
    assert_equal 5.0, fn.applyAsDouble(10)
  end

  def test_proc_to_java_double_to_int_function # arity 1 primitive double -> primitive int
    fn = proc { |d| d.floor }.to_java(java.util.function.DoubleToIntFunction)
    assert_equal 3, fn.applyAsInt(3.7)
  end

  def test_proc_to_java_double_to_long_function # arity 1 primitive double -> primitive long
    fn = proc { |d| d.floor }.to_java(java.util.function.DoubleToLongFunction)
    assert_equal 3, fn.applyAsLong(3.7)
  end

  # --- arity 2 -----------------------------------------------------------------

  def test_proc_to_java_two_arg_functional_interface
    joiner = proc { |a, b| "#{a}+#{b}" }

    bi = joiner.to_java(java.util.function.BiFunction)

    assert_equal 'x+y', bi.apply('x', 'y')
  end

  def test_proc_to_java_bi_consumer # arity 2, void -> __ruby_call(Class, Object, Object)
    pairs = []
    bi = proc { |a, b| pairs << [a, b] }.to_java(java.util.function.BiConsumer)
    bi.accept(:k, 1); bi.accept(:k2, 2)
    assert_equal [[:k, 1], [:k2, 2]], pairs
  end

  def test_proc_to_java_bi_predicate # arity 2, boolean return
    pred = proc { |a, b| a == b }.to_java(java.util.function.BiPredicate)
    assert_equal true,  pred.test('x', 'x')
    assert_equal false, pred.test('x', 'y')
  end

  def test_proc_to_java_binary_operator # arity 2, generic same-type return
    sum = proc { |a, b| a + b }.to_java(java.util.function.BinaryOperator)
    assert_equal 'ab', sum.apply('a', 'b')
  end

  def test_proc_to_java_int_binary_operator # arity 2, primitive int
    add = proc { |a, b| a + b }.to_java(java.util.function.IntBinaryOperator)
    assert_equal 5, add.applyAsInt(2, 3)
  end

  def test_proc_to_java_double_binary_operator # arity 2, primitive double
    mul = proc { |a, b| a * b }.to_java(java.util.function.DoubleBinaryOperator)
    assert_equal 6.0, mul.applyAsDouble(2.0, 3.0)
  end

  def test_proc_to_java_long_binary_operator # arity 2, primitive long args -> primitive long
    add = proc { |a, b| a + b }.to_java(java.util.function.LongBinaryOperator)
    assert_equal (1 << 40) + 1, add.applyAsLong(1 << 40, 1)
  end

  def test_proc_to_java_to_int_bi_function # arity 2, reference args -> primitive int
    bi = proc { |s, prefix| s.start_with?(prefix) ? 1 : 0 }.to_java(java.util.function.ToIntBiFunction)
    assert_equal 1, bi.applyAsInt('hello world', 'hello')
    assert_equal 0, bi.applyAsInt('hello world', 'bye')
  end

  def test_proc_to_java_to_long_bi_function # arity 2, reference args -> primitive long
    bi = proc { |s, n| s.length + n }.to_java(java.util.function.ToLongBiFunction)
    assert_equal 8, bi.applyAsLong('hello', 3)
  end

  def test_proc_to_java_to_double_bi_function # arity 2, reference args -> primitive double
    bi = proc { |s, n| s.length * n }.to_java(java.util.function.ToDoubleBiFunction)
    assert_equal 12.5, bi.applyAsDouble('hello', 2.5)
  end

  def test_proc_to_java_obj_int_consumer # arity 2, (reference, primitive int) -> void
    seen = []
    consumer = proc { |s, i| seen << [s, i] }.to_java(java.util.function.ObjIntConsumer)
    consumer.accept('a', 1); consumer.accept('b', 2)
    assert_equal [['a', 1], ['b', 2]], seen
  end

  def test_proc_to_java_obj_long_consumer # arity 2, (reference, primitive long) -> void
    seen = []
    consumer = proc { |s, l| seen << [s, l] }.to_java(java.util.function.ObjLongConsumer)
    consumer.accept('x', 1 << 40)
    assert_equal [['x', 1 << 40]], seen
  end

  def test_proc_to_java_obj_double_consumer # arity 2, (reference, primitive double) -> void
    seen = []
    consumer = proc { |s, d| seen << [s, d] }.to_java(java.util.function.ObjDoubleConsumer)
    consumer.accept('pi', 3.14)
    assert_equal [['pi', 3.14]], seen
  end

  def test_proc_to_java_comparator # arity 2 -> primitive int
    cmp = proc { |a, b| a.length <=> b.length }.to_java(java.util.Comparator)
    list = java.util.ArrayList.new
    list.add('aaa'); list.add('a'); list.add('aa')
    java.util.Collections.sort(list, cmp)
    assert_equal ['a', 'aa', 'aaa'], list.to_a
  end

  def test_proc_to_java_void_return_ignores_block_value
    # block returns a non-nil value; Runnable#run is void so the value is discarded.
    runnable = proc { 42 }.to_java(java.lang.Runnable)
    assert_nil runnable.run
  end

  def test_proc_to_java_primitive_return_unboxing
    # block returns a Ruby Integer; the generated bridge must unbox to primitive int.
    op = proc { |s| s.length * 10 }.to_java(java.util.function.ToIntFunction)
    assert_equal 50, op.applyAsInt('hello')
  end

  def test_proc_to_java_internal__ruby_object
    p0 = proc { 1 }
    p1 = proc { |a| a }
    p2 = proc { |a, b| [a, b] }
    p3 = proc { |a, b, c| [a, b, c] }

    assert_same p0, p0.to_java(java.util.function.Supplier).__ruby_object
    assert_same p1, p1.to_java(java.util.function.Function).__ruby_object
    assert_same p2, p2.to_java(java.util.function.BiFunction).__ruby_object
    assert_same p3, p3.to_java(java.lang.reflect.InvocationHandler).__ruby_object
  end

  def test_lambda_to_java_functional_interface
    fn = lambda { |a, b| a + b }
    bi = fn.to_java(java.util.function.BiFunction)
    assert_equal 5, bi.apply(2, 3)
    assert_same fn, bi.__ruby_object
  end

  def test_proc_to_java_repeated_invocation_is_stable
    counter = java.util.concurrent.atomic.AtomicInteger.new(0)
    consumer = proc { |_| counter.incrementAndGet }.to_java(java.util.function.Consumer)
    100.times { consumer.accept('x') }
    assert_equal 100, counter.get
  end

end
