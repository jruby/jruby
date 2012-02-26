require 'test/unit'


class TestEnumerable < Test::Unit::TestCase
  IS19 = RUBY_VERSION =~ /1\.9/

  #--------------------
  # a simple class with an 'each' method
  #
  class EachDefiner

    include Enumerable

    attr_reader :arr

    def initialize(arr)
      @arr = arr
    end

    def each
      i = 0
      loop do
        break if i == @arr.size
        yield @arr[i]
        i += 1
      end
    end

  end

  def E(*elements)
    EachDefiner.new(elements)
  end

  #--------------------

  def setup
    @a = E( 2, 4, 6, 8, 10 )

    @e_strs = E("333", "22", "666666", "1", "55555", "1010101010")
    @e_ints = E( 333,   22,   666666,   1,   55555,   1010101010)
  end

  #
  # These tests all rely on EachDefiner#each
  def test_00_sanity
    count = 0
    @a.each { |a| count += 1 }
    assert_equal(@a.arr.size, count)

    tmp = []
    @a.each { |a| tmp << a }
    assert_equal(@a.arr, tmp)
  end

    def test_all?
      assert_equal(true,  E().all?)
      assert_equal(true,  E('a','b','c').all?)
      assert_equal(true,  E(0, "x", true).all?)
      assert_equal(false, E(nil).all?)
      assert_equal(false, E(nil, nil).all?)
      assert_equal(false, E(false).all?)
      assert_equal(false, E(false, false).all?)
      assert_equal(false, E(0, "x", false, true).all?)
    end
    def test_any?
      assert_equal(true,  E(true).any?)
      assert_equal(true,  E('a','b','c').any?)
      assert_equal(true,  E(false, 0, nil).any?)
      assert_equal(false, E().any?)
      assert_equal(false, E(false, nil).any?)
    end

  def test_collect
    generic_test_collect(:collect)
  end

  def generic_test_collect(method)
    # #collect and #map are aliases, so we only need one function
    # that tests both methods

    assert_equal([2,4,6,8,10],          @a.send(method)) unless IS19
    assert_equal([],                    E().send(method) {|a| a })
    assert_equal([1,3,5,7,9],           @a.send(method) {|a| a-1 })
    assert_equal([1,1,1,1,1],           @a.send(method) {|a| 1 })
    assert_equal([[1],[3],[5],[7],[9]], @a.send(method) {|a| [a-1] })
  end

  def test_detect
    generic_test_detect(:detect)
  end

  def generic_test_detect(method)
    # #detect and #find are aliases, so we only need one function
    # that tests both methods

    # Test with and without a proc argument.  With such an argument
    # the proc is called if no match is found, and the value returned
    # from the proc is returned instead of nil.
    fail_count = 0
    fail_proc = lambda { fail_count += 1 ; "not found" }
    fail_proc_value = fail_proc.call

    for args, fail_value in [
        [ [],           nil             ],
        [ [fail_proc],  fail_proc_value ],
      ]
      
      assert_equal(fail_value,  E().send(method, *args) {|a| true  })
      assert_equal(fail_value,  @a.send(method, *args) {|a| false })
      assert_equal(2,           @a.send(method, *args) {|a| a > 1 })
      assert_equal(6,           @a.send(method, *args) {|a| a > 5 })
      assert_equal(10,          @a.send(method, *args) {|a| a > 9 })
      assert_equal(fail_value,  @a.send(method, *args) {|a| a > 10 })
    end

    # Make sure that the "proc" is only called once, and only if no
    # match is found.
    fail_count = 0
    assert_equal(fail_proc_value, @a.send(method, fail_proc) {|a| false })
    assert_equal(1, fail_count)

    fail_count = 0
    assert_equal(2, @a.send(method, fail_proc) {|a| true })
    assert_equal(0, fail_count)
  end

  def test_each_with_index
    acc = []
    obj = E()
    res = obj.each_with_index {|a,i| acc << [a,i]}
    assert_equal([], acc)
    assert_same(obj, res)

    acc = []
    res = @a.each_with_index {|a,i| acc << [a,i]}
    assert_equal([[2,0],[4,1],[6,2],[8,3],[10,4]], acc)
    assert_same(@a, res)
  end

  def test_entries
    generic_test_entries(:entries)
  end

  def generic_test_entries(method)
    assert_equal([],      E().send(method))
    assert_equal([3],     E(3).send(method))
    assert_equal(@a.arr,  @a.send(method))
    assert_equal([3,4,5], E(3,4,5).send(method))
    assert_equal([false,3,nil,4,5,nil], E(false,3,nil,4,5,nil).send(method))
  end

  def test_find
    generic_test_detect(:find)
  end

  def test_find_all
    generic_test_find_all(:find_all)
  end

  def generic_test_find_all(method)
    # #find_all and #select are aliases, so we only need one function
    # that tests both methods

    assert_equal([],     E().send(method) {|a| true})
    assert_equal([],     @a.send(method) {|a| false})
    assert_equal([2, 4], @a.send(method) {|a| a < 6})
    assert_equal([4],    @a.send(method) {|a| a == 4})
    assert_equal(@a.arr, @a.send(method) {|a| true})
  end

  def test_grep
    assert_equal([],     E().grep(1))
    assert_equal([4,6],  @a.grep(3..7))
    assert_equal([5,7],  @a.grep(3..7) {|a| a+1})
    assert_equal([self], E(self, 1).grep(TestEnumerable))
  end

  def test_include?
    generic_test_include?(:include?)
  end

  def generic_test_include?(method)
    # #include? and #member? are aliases, so we only need one function
    # that tests both methods

    assert( ! E().send(method, 1))
    assert( ! E().send(method, nil))
    assert(   E(2,4,6,8,10).send(method, 2))
    assert( ! E(2,4,6,8,10).send(method, 3))
    assert(   E(2,4,6,8,10).send(method, 4))
    assert(   E(nil).send(method, nil))

    # equality is tested with ==
    assert(   E(2,4,6,8,10).send(method, 2.0))
    assert(   E(2,4,[6,8],10).send(method, [6, 8]))
    assert(   E(2,4,[6,8],10).send(method, [6.0, 8.0]))
  end

    def test_inject
      # with inject argument
      assert_equal(1,     E().inject(1) {|acc,x| 999 } )
      assert_equal(999,   E(2).inject(1) {|acc,x| 999 } )
      assert_equal(1,     E(2).inject(1) {|acc,x| acc } )
      assert_equal(2,     E(2).inject(1) {|acc,x| x } )

      assert_equal(110,   E(1,2,3,4).inject(100) {|acc,x| acc + x } )
      assert_equal(2400,  E(1,2,3,4).inject(100) {|acc,x| acc * x } )

      assert_equal("cbaz", E('a','b','c').inject("z") {|result, i| i+result} )

      # no inject argument
      assert_equal(2,     E(2).inject {|acc,x| 999 } )
      assert_equal(2,     E(2).inject {|acc,x| acc } )
      assert_equal(2,     E(2).inject {|acc,x| x } )

      assert_equal(10,    E(1,2,3,4).inject {|acc,x| acc + x } )
      assert_equal(24,    E(1,2,3,4).inject {|acc,x| acc * x } )

      assert_equal("cba", E('a','b','c').inject {|result, i| i+result} )
      assert_equal(60, E(3, 4, 5).inject {|result, i| result*i} )
      assert_equal([1, 2, 'a', 'b'], E([1], 2, 'a','b').inject{|r,i| r<<i} )

      # error cases
      assert_equal(nil,     E().inject {|acc,x| 999 } )
      e = assert_raise(TypeError) do 
        E(4.2, "NO", 0).inject{|r, i| r*i} 
      end
      assert_match(/String.*coerced.*Float/, e.message)
    end

  def test_map
    generic_test_collect(:map)
  end

  def test_max
    # basic tests
    assert_equal(55,    E(55).max)

    assert_equal(99,    E(11,99).max)
    assert_equal(99,    E(99,11).max)
    assert_equal(33,    E(2, 33, 4, 11).max)

    assert_equal(5,   E(1,2,3,4,5).max)
    assert_equal(5,   E(5,4,3,2,1).max)
    assert_equal(5,   E(1,4,3,5,2).max)
    assert_equal(5,   E(5,5,5,5,5).max)

    assert_equal("tt",    E("aa","tt").max)
    assert_equal("tt",    E("tt","aa").max)
    assert_equal("4",     E("2","33","4","11").max)

    assert_equal("666666",   @e_strs.max)
    assert_equal(1010101010, @e_ints.max)

    # error cases
    assert_equal(nil, E().max)
    if IS19
      assert_raise(ArgumentError) { E(Object.new, Object.new).max }
    else
      assert_raise(NoMethodError) { E(Object.new, Object.new).max }
    end
    assert_raise(NoMethodError, ArgumentError) { E(11,"22").max }
    
    # with a block
    assert_equal("4",      E("2","33","4","11").max {|a,b| a <=> b })
    assert_equal(33,       E( 2 , 33 , 4 , 11 ).max {|a,b| a <=> b })

    assert_equal("11",     E("2","33","4","11").max {|a,b| b <=> a })
    assert_equal(2,        E( 2 , 33 , 4 , 11 ).max {|a,b| b <=> a })

    assert_equal("1010101010", @e_strs.max {|a,b| a.length <=> b.length })

    assert_equal("666666",     @e_strs.max {|a,b| a <=> b })
    assert_equal("1010101010", @e_strs.max {|a,b| a.to_i <=> b.to_i })

    assert_equal(1010101010,   @e_ints.max {|a,b| a <=> b })
    assert_equal(666666,       @e_ints.max {|a,b| a.to_s <=> b.to_s })

    result = nil
    [1,2].max {|*a| result = a; -1}
    assert_equal([2,1], result)
  end

  def test_member?
    generic_test_include?(:member?)
  end

  def test_min
    # basic tests
    assert_equal(55,    E(55).min)

    assert_equal(11,    E(11,99).min)
    assert_equal(11,    E(99,11).min)
    assert_equal(2,     E(2, 33, 4, 11).min)

    assert_equal(1,   E(1,2,3,4,5).min)
    assert_equal(1,   E(5,4,3,2,1).min)
    assert_equal(1,   E(4,1,3,5,2).min)
    assert_equal(5,   E(5,5,5,5,5).min)

    assert_equal("aa",    E("aa","tt").min)
    assert_equal("aa",    E("tt","aa").min)
    assert_equal("11",    E("2","33","4","11").min)

    assert_equal("1",     @e_strs.min)
    assert_equal(1,       @e_ints.min)

    # error cases
    assert_equal(nil, E().min)
    if IS19
      assert_raise(ArgumentError) { E(Object.new, Object.new).min }
    else
      assert_raise(NoMethodError) { E(Object.new, Object.new).min }
    end
    assert_raise(NoMethodError, ArgumentError) { E(11,"22").min }
    
    # with a block
    assert_equal("11",     E("2","33","4","11").min {|a,b| a <=> b })
    assert_equal(2,        E( 2 , 33 , 4 , 11 ).min {|a,b| a <=> b })

    assert_equal("4",      E("2","33","4","11").min {|a,b| b <=> a })
    assert_equal(33,       E( 2 , 33 , 4 , 11 ).min {|a,b| b <=> a })

    assert_equal("1",      @e_strs.min {|a,b| a.length <=> b.length })

    assert_equal("1",      @e_strs.min {|a,b| a <=> b })
    assert_equal("1",      @e_strs.min {|a,b| a.to_i <=> b.to_i })

    assert_equal(1,        @e_ints.min {|a,b| a <=> b })
    assert_equal(1,        @e_ints.min {|a,b| a.to_s <=> b.to_s })
  end

    def test_partition
      assert_equal([[0,2,4], [1,3,5]],  E(0,1,2,3,4,5).partition {|i| i%2==0})
      assert_equal([[0,1,2,3,4,5], []], E(0,1,2,3,4,5).partition {|i| true})
      assert_equal([[], [0,1,2,3,4,5]], E(0,1,2,3,4,5).partition {|i| false})
    end

  def test_reject
    assert_equal([],            E().reject {|a| true})
    assert_equal([],            E().reject {|a| false})
    assert_equal(@a.arr,        @a.reject {|a| false})
    assert_equal([6, 8, 10],    @a.reject {|a| a < 6})
    assert_equal([2, 6, 8, 10], @a.reject {|a| a == 4})
    assert_equal([],            @a.reject {|a| true})
  end

  def test_select
    generic_test_find_all(:select)
  end

  def test_sort
    assert_equal([],            E().sort)
    assert_equal([1],           E(1).sort)
    assert_equal([1,2],         E(2,1).sort)
    assert_equal([1,2],         E(1,2).sort)
    assert_equal([1,1,1],       E(1,1,1).sort)
    assert_equal([1,2,3,3,4,5], E(3,2,5,3,4,1).sort)

    assert_equal(@a.arr,         @a.sort)

    assert_equal("ant bat cat dog", E('cat','ant','bat','dog').sort.join(" "))

    # with block
    assert_equal(@a.arr,         @a.sort {|a,b| a - b })
    assert_equal(@a.arr.reverse, @a.sort {|a,b| b <=> a})
  end

    def test_sort_by
      rank = {'a'=>3, 'b'=>1, 'c'=>2}
      assert_equal(%w(b c a), %w(a b c).sort_by{|i| rank[i]})
    end

  def test_to_a
    generic_test_entries(:to_a)
  end

    # Helper method to "test_zip".
    # Here we test "zip" with and without a block, using
    # the "test-vectors" specified in "test_zip".
    #
    def zip_tests(expected, object, *args)
      assert_equal(expected, object.zip(*args))
      
      acc = []
      res = object.zip(*args) {|x| acc << x }
      assert_equal(nil, res)
      assert_equal(expected, acc)
    end

    def test_zip
      zip_tests([%w(a 1), %w(b 2), %w(c 3), %w(d 4)],
                E('a','b','c','d'), %w(1 2 3 4))
      zip_tests([%w(a 1), %w(b 2)], E('a','b'), %w(1 2 3 4))
      zip_tests([%w(a 1), %w(b 2), ['c', nil], ['d', nil]],
                E('a','b','c','d'), %w(1 2))
      zip_tests([[1], [2]], E(1, 2))
      zip_tests([["a\n"], ["b\n"], ["c"]], "a\nb\nc") unless IS19
      zip_tests([["a\n", 1], ["b\n", 2], ["c", 3]],
                "a\nb\nc", [1, 2, 3]) unless IS19
      zip_tests([[1, nil], [2, nil]], E(1, 2), [])
    end

end
