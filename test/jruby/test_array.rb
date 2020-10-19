require 'test/unit'

## NOTE: Most of the tests that were here have been moved to the RubySpec.

class TestArray < Test::Unit::TestCase
  ##### splat test #####
  class ATest
    def to_a; 1; end
  end

  def test_splatting
    proc { |a| assert_equal(1, a) }.call(*1)
    assert_raises(TypeError) { proc { |a| }.call(*ATest.new) }
  end

  def test_initialize_on_frozen_array
    assert_raises(FrozenError) {
      [1, 2, 3].freeze.instance_eval { initialize }
    }
  end

  def test_uniq_with_block_after_slice
    assert_equal [1], [1, 1, 1][1,2].uniq { |x| x }
    assert_equal [1], [1, 1, 1][1,2].uniq! { |x| x }
  end

  # JRUBY-4157
  def test_shared_ary_slice
    assert_equal [4,5,6], [1,2,3,4,5,6].slice(1,5).slice!(2,3)
  end
  
  # JRUBY-4206
  def test_map
    methods = %w{map map! collect collect!}
    methods.each { |method|
      assert_no_match(/Enumerable/, [].method(method).to_s)
    }
  end

  # GH-4021
  def test_range_to_a
    assert_equal [ '2202702806' ], ("2202702806".."2202702806").to_a
    str = '12345678900000'; assert (str..str).include?(str)
    str = '2202702806'; assert_equal true, (str..str).member?(str)
    str = '2202702806'; assert_equal false, (str..str).member?(str.to_i)
  end

  def test_collect_concurrency
    arr = []

    Thread.new do ; times = 0
      loop { arr << Time.now.to_f; break if (times += 1) == 1000 }
    end

    1000.times do
      begin
        arr.collect { |f| f.to_i }.size
        # expected not to raise a Java AIOoBE
      rescue ConcurrencyError => e
        puts "#{__method__} : #{e}" if $VERBOSE
      end
    end
  end

  # GH-5141
  def test_concat_self
    arr = [1]
    arr.concat(arr)
    arr.concat(arr)
    arr.concat(arr)
    assert_equal [1, 1, 1, 1, 1, 1, 1, 1], arr

    arr = [1, 2]
    arr.concat(arr)
    arr.concat(arr)
    assert_equal [1, 2, 1, 2, 1, 2, 1, 2], arr
  end

  def test_sort_4538
    # array sort test based on JRUBY-4538
    a = (1601..1980).to_a +
        ( 941..1600).to_a +
        ( 561.. 660).to_a +
        (   1.. 280).to_a +
        ( 661.. 940).to_a +
        (2261..2640).to_a +
        ( 281.. 560).to_a +
        (2921..3300).to_a +
        (1981..2260).to_a +
        (4901..5000).to_a +
        (3961..4220).to_a

    assert_equal(a.sort, a.sort.sort)
  end

  # GH-5483
  def test_transpose_and_delete
    arr = [[1, 2], [3, 4], [5, 6]]
    (res = arr.transpose).delete_at(1)
    assert_equal [[1, 3, 5]], res
    (res = arr.transpose).delete_at(2)
    assert_equal [[1, 3, 5], [2, 4, 6]], res
  end

  class MyArray < Array; end

  def test_array_instance_methods_on_subclass
    @arr = MyArray.new([1,2,3])
    @arr2 = MyArray.new([[1,2],[2,3],[3,3]])

    assert_equal(Array, @arr2.transpose.class)
    assert_equal(Array, @arr.compact.class)
    assert_equal(Array, @arr.reverse.class)
    assert_equal(MyArray, @arr2.flatten.class)
    assert_equal(MyArray, @arr.uniq.class)
    assert_equal(Array, @arr.sort.class)
    assert_equal(MyArray, @arr[1,2].class)
    assert_equal(MyArray, @arr[1..2].class)
    assert_equal(Array, @arr.to_a.class)
    assert_equal(MyArray, @arr.to_ary.class)
    assert_equal(MyArray, @arr.slice(1,2).class)
    assert_equal(MyArray, @arr.slice!(1,2).class)
    assert_equal(MyArray, (@arr*0).class)
    assert_equal(MyArray, (@arr*2).class)
    assert_equal(MyArray, @arr.replace([1,2,3]).class)
    assert_equal(Array, @arr.last(2).class)
    assert_equal(Array, @arr.first(2).class)
    assert_equal(Enumerator, @arr.collect.class)
    assert_equal(Array, @arr.collect{true}.class)
    assert_equal(Array, @arr.zip([1,2,3]).class)
    assert_equal(MyArray, @arr.dup.class)
  end

  LONGP = 9223372036854775807

  def test_aset_error # from MRI's TestArray which has test_aset_error excluded
    assert_raise(IndexError) { [0][-2] = 1 }
    assert_raise(IndexError) { [0][LONGP] = 2 }
    assert_raise(IndexError) { [0][(LONGP + 1) / 2 - 1] = 2 }
    #assert_raise(IndexError) { [0][LONGP..-1] = 2 }
    begin
      [0][LONGP..-1] = 2
    rescue StandardError # okay
    end

    a = [0]
    a[2] = 4
    assert_equal([0, nil, 4], a)
    assert_raise(ArgumentError) { [0][0, 0, 0] = 0 }
    assert_raise(ArgumentError) { [0].freeze[0, 0, 0] = 0 }
    assert_raise(TypeError) { [0][:foo] = 0 }
    assert_raise(FrozenError) { [0].freeze[:foo] = 0 }
  end

  class Foo1
    def initialize
      @ary = [1,2,3]
    end

    def ==(other)
      @ary == other
    end

    def to_ary
      @ary
    end
  end

  class Foo2
    def initialize
      @ary = [1,2,3]
    end

    def ==(other)
      @ary == other
    end
  end

  def test_delegated_array_equals
    a = Foo1.new
    assert_equal(a, a)
    assert(a == a)
  end

  def test_badly_delegated_array_equals
    a = Foo2.new
    assert_not_equal(a, a)
    assert(!(a == a))
  end

  # This is unspecified behavior, and has no tests in the ruby/spec or CRuby
  # suites. Since we are attempting to match CRuby behavior here, we will test
  # this in our own suite. See jruby/jruby#6371.
  def test_delete_if_with_modification
    rules = [1, 2, 3, 4, 5]
    iters = []
    rules.delete_if do |rule|
      iters << rule
      rules.insert(1, 2) if rule == 1
      true
    end

    assert_equal([1,2,2,3,4,5], iters)
    assert_equal([], rules)
  end

end
