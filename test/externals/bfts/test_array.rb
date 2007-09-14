require 'test/unit'
require 'rubicon_testcase'

class TestArray < RubiconTestCase

  def setup
    super
    @cls = Array
  end

  def test_and
    assert_equal(@cls[1, 3], @cls[ 1, 1, 3, 5 ] & @cls[ 1, 2, 3 ])
    assert_equal(@cls[1, 3], @cls[ 1, 1, 3, 5 ] & @cls[ 3, 2, 1 ])
    assert_equal(@cls[1, 3], @cls[ 1, 1, 3, 5 ] & @cls[ 3, 3, 2, 1 ])
    assert_equal(@cls[],     @cls[ 1, 1, 3, 5 ] & @cls[ ])
    assert_equal(@cls[],     @cls[  ]           & @cls[ 1, 2, 3 ])
    assert_equal(@cls[],     @cls[ 1, 2, 3 ]    & @cls[ 4, 5, 6 ])

    # == is not used
    a1 = 1
    a2 = 1.0
    assert_equal(true,  a1 == a2)
    assert_equal(false, a1.eql?(a2))
    assert_equal(@cls[2], @cls[ a1, 2, 3 ] & @cls[ a2, 2, 4 ])

    # eql? is used
    a1 = [1,2]
    a2 = [1,2]
    assert_equal(true,  a1 == a2)
    assert_equal(true,  a1.eql?(a2))
    assert_equal(@cls[[1,2], 2], @cls[ a1, 2, 3 ] & @cls[ a2, 2, 4 ])

    # error case
    assert_raises(TypeError) { @cls[ 1, 1, 3, 5 ] & Object.new }
  end

  def test_assoc
    a1 = @cls[*%w( cat feline )]
    a2 = @cls[*%w( dog canine )]
    a3 = @cls[*%w( mule asinine )]
    a4 = @cls[*%w( mule asinine )]

    a = @cls[ a1, a2, a3, a4 ]

    assert_same(a1, a.assoc('cat'))
    assert_same(a3, a.assoc('mule'))
    assert_same(nil, a.assoc('asinine'))
    assert_same(nil, a.assoc('wombat'))
    assert_same(nil, a.assoc(1..2))
  end

  def test_at
    a = @cls[*(0..99).to_a]
    assert_equal(0,   a.at(0))
    assert_equal(10,  a.at(10))
    assert_equal(99,  a.at(99))
    assert_equal(nil, a.at(100))
    assert_equal(99,  a.at(-1))
    assert_equal(0,  a.at(-100))
    assert_equal(nil, a.at(-101))
    assert_raises(TypeError) { a.at('cat') }
  end

  def test_class_index
    a = @cls[ 5, 4, 3, 2, 1 ]
    assert_instance_of(@cls, a)
    assert_equal(5, a.length)
    5.times { |i| assert_equal(5-i, a[i]) }
    assert_nil(a[6])

    a = @cls[]
    assert_instance_of(@cls, a)
    assert_equal(0, a.length)
  end

  def test_clear
    a = @cls[1, 2, 3]
    b = a.clear
    assert_equal(@cls[], a)
    assert_equal(@cls[], b)
    assert_equal(a.__id__, b.__id__)
  end

  def test_clone
    for taint in [ false, true ]
      for frozen in [ false, true ]
        a = @cls[*(0..99).to_a]
        a.taint  if taint
        a.freeze if frozen
        b = a.clone

        assert_equal(a, b)
        assert(a.__id__ != b.__id__)
        assert_equal(a.frozen?, b.frozen?)
        assert_equal(a.tainted?, b.tainted?)
      end
    end
  end

  def test_collect
    a = @cls[ 1, 'cat', 1..1 ]
    assert_equal([ Fixnum, String, Range], a.collect {|e| e.class} )
    assert_equal([ 99, 99, 99], a.collect { 99 } )

    assert_equal([], @cls[].collect { 99 })

    assert_equal([1, 2, 3], @cls[1, 2, 3].collect)
  end

  def test_collect_bang
    util_collect_bang(:collect!)
  end

  def util_collect_bang(method)
    a = @cls[ 1, 'cat', 1..1 ]
    res = a.send(method) {|e| e.class}
    assert_equal([ Fixnum, String, Range], a)
    assert_same(a, res)
   
    a = @cls[ 1, 'cat', 1..1 ]
    res = a.send(method) { 99 }
    assert_equal([ 99, 99, 99], a)
    assert_equal(a, res)

    a = @cls[ ]
    res = a.send(method) { 99 }
    assert_equal([], a)
    assert_equal(a, res)
  end

  def test_compact
    a = @cls[ 1, nil, nil, 2, 3, nil, 4 ]
    assert_equal(@cls[1, 2, 3, 4], a.compact)

    a = @cls[ nil, 1, nil, 2, 3, nil, 4 ]
    assert_equal(@cls[1, 2, 3, 4], a.compact)

    a = @cls[ 1, nil, nil, 2, 3, nil, 4, nil ]
    assert_equal(@cls[1, 2, 3, 4], a.compact)

    a = @cls[ 1, 2, 3, 4 ]
    assert_equal(@cls[1, 2, 3, 4], a.compact)
  end

  def test_compact_bang
    a = @cls[ 1, nil, nil, 2, 3, nil, 4 ]
    assert_equal(@cls[1, 2, 3, 4], a.compact!)
    assert_equal(@cls[1, 2, 3, 4], a)

    a = @cls[ nil, 1, nil, 2, 3, nil, 4 ]
    assert_equal(@cls[1, 2, 3, 4], a.compact!)
    assert_equal(@cls[1, 2, 3, 4], a)

    a = @cls[ 1, nil, nil, 2, 3, nil, 4, nil ]
    assert_equal(@cls[1, 2, 3, 4], a.compact!)
    assert_equal(@cls[1, 2, 3, 4], a)

    a = @cls[ 1, 2, 3, 4 ]
    assert_equal(nil, a.compact!)
    assert_equal(@cls[1, 2, 3, 4], a)
  end

  def test_concat
    assert_equal(@cls[1, 2, 3, 4],     @cls[1, 2].concat(@cls[3, 4]))
    assert_equal(@cls[1, 2, 3, 4],     @cls[].concat(@cls[1, 2, 3, 4]))
    assert_equal(@cls[1, 2, 3, 4],     @cls[1, 2, 3, 4].concat(@cls[]))
    assert_equal(@cls[],               @cls[].concat(@cls[]))
    assert_equal(@cls[@cls[1, 2], @cls[3, 4]], @cls[@cls[1, 2]].concat(@cls[@cls[3, 4]]))
    
    a = @cls[1, 2, 3]
    a.concat(a)
    assert_equal([1, 2, 3, 1, 2, 3], a)
  end

  def test_delete
    a = @cls[*('cab'..'cat').to_a]
    assert_equal('cap', a.delete('cap'))
    assert_equal(@cls[*('cab'..'cao').to_a] + @cls[*('caq'..'cat').to_a], a)

    a = @cls[*('cab'..'cat').to_a]
    assert_equal('cab', a.delete('cab'))
    assert_equal(@cls[*('cac'..'cat').to_a], a)

    a = @cls[*('cab'..'cat').to_a]
    assert_equal('cat', a.delete('cat'))
    assert_equal(@cls[*('cab'..'cas').to_a], a)

    a = @cls[*('cab'..'cat').to_a]
    assert_equal(nil, a.delete('cup'))
    assert_equal(@cls[*('cab'..'cat').to_a], a)

    a = @cls[*('cab'..'cat').to_a]
    assert_equal(99, a.delete('cup') { 99 } )
    assert_equal(@cls[*('cab'..'cat').to_a], a)
  end

  def test_delete_at
    a = @cls[*(1..5).to_a]
    assert_equal(3, a.delete_at(2))
    assert_equal(@cls[1, 2, 4, 5], a)

    a = @cls[*(1..5).to_a]
    assert_equal(4, a.delete_at(-2))
    assert_equal(@cls[1, 2, 3, 5], a)

    a = @cls[*(1..5).to_a]
    assert_equal(nil, a.delete_at(5))
    assert_equal(@cls[1, 2, 3, 4, 5], a)

    a = @cls[*(1..5).to_a]
    assert_equal(nil, a.delete_at(-6))
    assert_equal(@cls[1, 2, 3, 4, 5], a)
  end

  # almost identical to reject!
  def test_delete_if
    a = @cls[ 1, 2, 3, 4, 5 ]
    assert_equal(a, a.delete_if { false }) # reject! returns nil here
    assert_equal(@cls[1, 2, 3, 4, 5], a)

    a = @cls[ 1, 2, 3, 4, 5 ]
    assert_equal(a, a.delete_if { true })
    assert_equal(@cls[], a)

    a = @cls[ 1, 2, 3, 4, 5 ]
    assert_equal(a, a.delete_if { |i| i > 3 })
    assert_equal(@cls[1, 2, 3], a)
  end

  def test_dup
    for taint in [ false, true ]
      for frozen in [ false, true ]
        a = @cls[*(0..99).to_a]
        a.taint  if taint
        a.freeze if frozen
        b = a.dup

        assert_equal(a, b)
        assert(a.__id__ != b.__id__)
        assert_equal(false, b.frozen?)
        assert_equal(a.tainted?, b.tainted?)
      end
    end
  end

  def test_each
    a = @cls[*%w( ant bat cat dog )]
    i = 0
    a.each { |e|
      assert_equal(a[i], e)
      i += 1
    }
    assert_equal(4, i)

    a = @cls[]
    i = 0
    a.each { |e|
      assert_equal(a[i], e)
      i += 1
    }
    assert_equal(0, i)

    assert_equal(a, a.each {})
  end

  def test_each_index
    a = @cls[*%w( ant bat cat dog )]
    i = 0
    a.each_index { |ind|
      assert_equal(i, ind)
      i += 1
    }
    assert_equal(4, i)

    a = @cls[]
    i = 0
    a.each_index { |ind|
      assert_equal(i, ind)
      i += 1
    }
    assert_equal(0, i)

    assert_equal(a, a.each_index {})
  end

  def test_empty_eh
    assert(@cls[].empty?)
    assert(!@cls[1].empty?)
  end

  def test_eql_eh
    assert(@cls[].eql?(@cls[]))
    assert(@cls[1].eql?(@cls[1]))
    assert(@cls[1, 1, 2, 2].eql?(@cls[1, 1, 2, 2]))
    assert(!@cls[1.0, 1.0, 2.0, 2.0].eql?(@cls[1, 1, 2, 2]))
  end

  def test_equals2
    assert_equal(true,  @cls[] == @cls[])
    assert_equal(false, @cls[] == @cls[nil])
    assert_equal(false, @cls[] == @cls[9])
    assert_equal(false, @cls[8] == @cls[9])
    assert_equal(false, @cls[8] == @cls[])
    assert_equal(false, @cls[nil] == @cls[])
    assert_equal(true,  @cls[1] == @cls[1])
    assert_equal(false, @cls[1] == Object.new)

    assert_equal(true,  @cls[1, 1, 2, 2] == @cls[1, 1, 2, 2])
    assert_equal(false, @cls[1, 9, 2, 2] == @cls[1, 1, 2, 2])
    assert_equal(false, @cls[1, 1, 2, 2] == @cls[1, 1, 2, 2, 9])
    assert_equal(false, @cls[1, 1, 2, 2] == @cls[1, 1, 2])

    # elements are compared with ==
    assert_equal(true,  @cls[1] == @cls[1.000])
    assert_equal(false, @cls[1] == @cls[1.001])
    assert_equal(true,  @cls[1.0, 1.0, 2.0, 2.0] == @cls[1, 1, 2, 2])
    assert_equal(false, @cls[1.0, 1.0, 2.001, 2.0] == @cls[1, 1, 2, 2])

    # nested arrays
    assert_equal(true,  @cls[11,@cls[22,33],44] == @cls[11,@cls[22,33],44])
    assert_equal(true,  @cls[11,@cls[22,33],44] == @cls[11,@cls[22,33.0],44])
    assert_equal(false, @cls[11,@cls[22,33],44] == @cls[11,@cls[22,99],44])
    assert_equal(false, @cls[11,@cls[22,33],44] == @cls[11,@cls[22,33,99],44])
  end

  # FIX!!! stupid fuckers!
  def test_equals3
    # === is the same as == for arrays
    # copies of the ones in test_equals2 (WHAT?!?! DUMB!)

    assert_equal(true,  @cls[] == @cls[])
    assert_equal(false, @cls[] == @cls[nil])
    assert_equal(false, @cls[] == @cls[9])
    assert_equal(false, @cls[8] == @cls[9])
    assert_equal(false, @cls[8] == @cls[])
    assert_equal(false, @cls[nil] == @cls[])
    assert_equal(true,  @cls[1] == @cls[1])
    assert_equal(false, @cls[1] == Object.new)

    assert_equal(true,  @cls[1, 1, 2, 2] == @cls[1, 1, 2, 2])
    assert_equal(false, @cls[1, 9, 2, 2] == @cls[1, 1, 2, 2])
    assert_equal(false, @cls[1, 1, 2, 2] == @cls[1, 1, 2, 2, 9])
    assert_equal(false, @cls[1, 1, 2, 2] == @cls[1, 1, 2])

    # elements are compared with ==
    assert_equal(true,  @cls[1] == @cls[1.000])
    assert_equal(false, @cls[1] == @cls[1.001])
    assert_equal(true,  @cls[1.0, 1.0, 2.0, 2.0] == @cls[1, 1, 2, 2])
    assert_equal(false, @cls[1.0, 1.0, 2.001, 2.0] == @cls[1, 1, 2, 2])

    # nested arrays
    assert_equal(true,  @cls[11,@cls[22,33],44] == @cls[11,@cls[22,33],44])
    assert_equal(true,  @cls[11,@cls[22,33],44] == @cls[11,@cls[22,33.0],44])
    assert_equal(false, @cls[11,@cls[22,33],44] == @cls[11,@cls[22,99],44])
    assert_equal(false, @cls[11,@cls[22,33],44] == @cls[11,@cls[22,33,99],44])
  end

  def test_fetch
    a = %w(a b c d)
    assert_equal('a', a.fetch(0))
    assert_equal('c', a.fetch(2))
    assert_raises(IndexError) { a.fetch(4) }
    assert_equal(8, a.fetch(4) {|k| k*2} )
    assert_equal('default', a.fetch(4, 'default'))
    
    assert_equal('d', a.fetch(-1))
    assert_equal('b', a.fetch(-3))
    assert_raises(IndexError) { a.fetch(-5) }
    assert_equal(-10, a.fetch(-5) {|k| k*2} )
    assert_equal('default', a.fetch(-5, 'default'))
  end

  def test_fill
    assert_equal @cls[],       @cls[]    .fill(99)
    assert_equal @cls[],       @cls[]    .fill(99,  nil)
    assert_equal @cls[],       @cls[]    .fill(99,  0)
    assert_equal @cls[],       @cls[]    .fill(99, -1)
    assert_equal @cls[],       @cls[]    .fill(99, -9)
    assert_equal @cls[],       @cls[]    .fill(99,  0, nil)
    assert_equal @cls[99],     @cls[]    .fill(99,  0, 1)
    assert_equal @cls[99],     @cls[1]   .fill(99, -1, 1)
    assert_equal @cls[99],     @cls[1]   .fill(99, -9, 1), "Underflow"
    assert_equal @cls[99],     @cls[]    .fill(99, 0..0)

    assert_raises ArgumentError do
                               @cls[]    .fill(99, 0, 0, 0)
    end

    assert_raises TypeError, "Can't convert Range into Integer" do
                               @cls[]    .fill(99, 0..0, :X)
    end

    assert_equal @cls[99],     @cls[1]   .fill(99)
    assert_equal @cls[99],     @cls[1]   .fill(99,  nil)
    assert_equal @cls[99],     @cls[1]   .fill(99,  0)
    assert_equal @cls[99],     @cls[1]   .fill(99, -1)
    assert_equal @cls[99],     @cls[1]   .fill(99, -9)
    assert_equal @cls[99],     @cls[1]   .fill(99,  0, nil)
    assert_equal @cls[99],     @cls[1]   .fill(99,  0, 1)
    assert_equal @cls[99],     @cls[1]   .fill(99, -1, 1)
    assert_equal @cls[99],     @cls[1]   .fill(99, -9, 1), "Underflow"
    assert_equal @cls[99],     @cls[1]   .fill(99, 0..0)

    assert_equal @cls[99, 99], @cls[1, 2].fill(99)
    assert_equal @cls[99, 99], @cls[1, 2].fill(99,  nil)
    assert_equal @cls[99, 99], @cls[1, 2].fill(99,  0)
    assert_equal @cls[ 1, 99], @cls[1, 2].fill(99, -1)
    assert_equal @cls[99, 99], @cls[1, 2].fill(99, -9)
    assert_equal @cls[ 1, 99], @cls[1, 2].fill(99,  1, nil)
    assert_equal @cls[99,  2], @cls[1, 2].fill(99,  0, 1)
    assert_equal @cls[ 1, 99], @cls[1, 2].fill(99, -1, 1)
    assert_equal @cls[99,  2], @cls[1, 2].fill(99, -2, 1)
    assert_equal @cls[99,  2], @cls[1, 2].fill(99, -9, 1), "Underflow"
    assert_equal @cls[99,  2], @cls[1, 2].fill(99, 0..0)

    assert_equal @cls[1, 2, 99, 99], @cls[1, 2].fill(99, 2, 2), "Out of bounds"
    assert_equal @cls[1, 2, 99, 99], @cls[1, 2].fill(99, 2..3), "Out of bounds"

    assert_equal @cls[1, 102],    @cls[1, 2].fill(1)       { |i| 101 + i }
    assert_equal @cls[1, 2],      @cls[1, 2].fill(2)       { |i| 101 + i }
    assert_equal @cls[101, 102],  @cls[1, 2].fill( 0, nil) { |i| 101 + i }
    assert_equal @cls[101, 2],    @cls[1, 2].fill( 0, 1)   { |i| 101 + i }
    assert_equal @cls[101, 102],  @cls[1, 2].fill( 0, 2)   { |i| 101 + i }
    assert_equal @cls[1, 2, 103], @cls[1, 2].fill( 2, 1)   { |i| 101 + i }
    assert_equal @cls[1, 102],    @cls[1, 2].fill(-1, 1)   { |i| 101 + i }
    assert_equal @cls[101, 2],    @cls[1, 2].fill(-2, 1)   { |i| 101 + i }
    assert_equal @cls[101, 2],    @cls[1, 2].fill(-9, 1)   { |i| 101 + i }

    assert_equal @cls[101, 102],       @cls[1, 2].fill(0..1) { |i| 101 + i }
    assert_equal @cls[1, 102, 103],    @cls[1, 2].fill(1..2) { |i| 101 + i }
    assert_equal @cls[1, 2, 103, 104], @cls[1, 2].fill(2..3) { |i| 101 + i }

    assert_raises TypeError do
      @cls[].fill(4..6, 1) { }
    end

    assert_raises ArgumentError do
      @cls[].fill(1, 1, 1) { }
    end

    assert_raises ArgumentError do
      @cls[].fill(4..6, 1, 1) { }
    end

    assert_raises ArgumentError do
      @cls[].fill(99, 0..0, :X, :Y)
    end
  end

  def test_first
    assert_equal(3,   @cls[3,4,5].first)
    assert_equal(4,   @cls[4,5].first)
    assert_equal(5,   @cls[5].first)
    assert_equal(nil, @cls[].first)

    # with argument
    assert_equal([],      @cls[3,4,5].first(0))
    assert_equal([3],     @cls[3,4,5].first(1))
    assert_equal([3,4],   @cls[3,4,5].first(2))
    assert_equal([3,4,5], @cls[3,4,5].first(3))
    assert_equal([3,4,5], @cls[3,4,5].first(4))
    assert_equal([3,4,5], @cls[3,4,5].first(999))

    # error cases
    assert_raises(ArgumentError) { @cls[3,4,5].first(-1) }
    assert_raises(TypeError)     { @cls[3,4,5].first(Object.new) }
  end

  def test_flatten
    a1 = @cls[ 1, 2, 3]
    a2 = @cls[ 5, 6 ]
    a3 = @cls[ 4, a2 ]
    a4 = @cls[ a1, a3 ]
    assert_equal(@cls[1, 2, 3, 4, 5, 6], a4.flatten)
    assert_equal(@cls[ a1, a3], a4)

    a5 = @cls[ a1, @cls[], a3 ]
    assert_equal(@cls[1, 2, 3, 4, 5, 6], a5.flatten)
    assert_equal(@cls[], @cls[].flatten)
    assert_equal(@cls[], 
                 @cls[@cls[@cls[@cls[],@cls[]],@cls[@cls[]],@cls[]],@cls[@cls[@cls[]]]].flatten)
  end

  def test_flatten_bang
    a1 = @cls[ 1, 2, 3]
    a2 = @cls[ 5, 6 ]
    a3 = @cls[ 4, a2 ]
    a4 = @cls[ a1, a3 ]
    assert_equal(@cls[1, 2, 3, 4, 5, 6], a4.flatten!)
    assert_equal(@cls[1, 2, 3, 4, 5, 6], a4)

    a5 = @cls[ a1, @cls[], a3 ]
    assert_equal(@cls[1, 2, 3, 4, 5, 6], a5.flatten!)
    assert_equal(@cls[1, 2, 3, 4, 5, 6], a5)

    assert_equal(@cls[], @cls[].flatten)
    assert_equal(@cls[], 
                 @cls[@cls[@cls[@cls[],@cls[]],@cls[@cls[]],@cls[]],@cls[@cls[@cls[]]]].flatten)
  end

  def test_frozen_eh
    # TODO: raise NotImplementedError, 'Need to write test_frozen_eh'
  end

  def test_hash
    a1 = @cls[ 'cat', 'dog' ]
    a2 = @cls[ 'cat', 'dog' ]
    a3 = @cls[ 'dog', 'cat' ]
    assert(a1.hash == a2.hash)
    assert(a1.hash != a3.hash)
  end

  def test_include_eh
    a = @cls[ 'cat', 99, /a/, @cls[ 1, 2, 3] ]
    assert(a.include?('cat'))
    assert(a.include?(99))
    assert(a.include?(/a/))
    assert(a.include?([1,2,3]))
    assert(!a.include?('ca'))
    assert(!a.include?([1,2]))
  end

  def test_index
    a = @cls[*(1..100).to_a]

    assert_equal(1, a[0])
    assert_equal(100, a[99])
    assert_nil(a[100])
    assert_equal(100, a[-1])
    assert_equal(99,  a[-2])
    assert_equal(1,   a[-100])
    assert_nil(a[-101])
    assert_nil(a[-101,0])
    assert_nil(a[-101,1])
    assert_nil(a[-101,-1])
    assert_nil(a[10,-1])

    assert_equal(@cls[1],   a[0,1])
    assert_equal(@cls[100], a[99,1])
    assert_equal(@cls[],    a[100,1])
    assert_equal(@cls[100], a[99,100])
    assert_equal(@cls[100], a[-1,1])
    assert_equal(@cls[99],  a[-2,1])
    assert_equal(@cls[],    a[-100,0])
    assert_equal(@cls[1],   a[-100,1])

    assert_equal(@cls[10, 11, 12], a[9, 3])
    assert_equal(@cls[10, 11, 12], a[-91, 3])

    assert_equal(@cls[1],   a[0..0])
    assert_equal(@cls[100], a[99..99])
    assert_equal(@cls[],    a[100..100])
    assert_equal(@cls[100], a[99..200])
    assert_equal(@cls[100], a[-1..-1])
    assert_equal(@cls[99],  a[-2..-2])

    assert_equal(@cls[10, 11, 12], a[9..11])
    assert_equal(@cls[10, 11, 12], a[-91..-89])
    
    assert_nil(a[10, -3])
    assert_equal([], a[10..7])

    assert_raises(TypeError) {a['cat']}
  end

  def test_index_equals
    a = @cls[*(0..99).to_a]
    assert_equal(0, a[0] = 0)
    assert_equal(@cls[0] + @cls[*(1..99).to_a], a)

    a = @cls[*(0..99).to_a]
    assert_equal(0, a[10,10] = 0)
    assert_equal(@cls[*(0..9).to_a] + @cls[0] + @cls[*(20..99).to_a], a)

    a = @cls[*(0..99).to_a]
    assert_equal(0, a[-1] = 0)
    assert_equal(@cls[*(0..98).to_a] + @cls[0], a)

    a = @cls[*(0..99).to_a]
    assert_equal(0, a[-10, 10] = 0)
    assert_equal(@cls[*(0..89).to_a] + @cls[0], a)

    a = @cls[*(0..99).to_a]
    assert_equal(0, a[0,1000] = 0)
    assert_equal(@cls[0] , a)

    a = @cls[*(0..99).to_a]
    assert_equal(0, a[10..19] = 0)
    assert_equal(@cls[*(0..9).to_a] + @cls[0] + @cls[*(20..99).to_a], a)

    b = @cls[*%w( a b c )]
    a = @cls[*(0..99).to_a]
    assert_equal(b, a[0,1] = b)
    assert_equal(b + @cls[*(1..99).to_a], a)

    a = @cls[*(0..99).to_a]
    assert_equal(b, a[10,10] = b)
    assert_equal(@cls[*(0..9).to_a] + b + @cls[*(20..99).to_a], a)

    a = @cls[*(0..99).to_a]
    assert_equal(b, a[-1, 1] = b)
    assert_equal(@cls[*(0..98).to_a] + b, a)

    a = @cls[*(0..99).to_a]
    assert_equal(b, a[-10, 10] = b)
    assert_equal(@cls[*(0..89).to_a] + b, a)

    a = @cls[*(0..99).to_a]
    assert_equal(b, a[0,1000] = b)
    assert_equal(b , a)

    a = @cls[*(0..99).to_a]
    assert_equal(b, a[10..19] = b)
    assert_equal(@cls[*(0..9).to_a] + b + @cls[*(20..99).to_a], a)

    if ruby_version < "1.9.0" then
      # Before 1.9.0 assigning nil to an array slice deletes
      # the elements from the array.

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[0,1] = nil)
      assert_equal(@cls[*(1..99).to_a], a)

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[10,10] = nil)
      assert_equal(@cls[*(0..9).to_a] + @cls[*(20..99).to_a], a)

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[-1, 1] = nil)
      assert_equal(@cls[*(0..98).to_a], a)

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[-10, 10] = nil)
      assert_equal(@cls[*(0..89).to_a], a)

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[0,1000] = nil)
      assert_equal(@cls[] , a)

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[10..19] = nil)
      assert_equal(@cls[*(0..9).to_a] + @cls[*(20..99).to_a], a)
    else

      # From 1.9.0 assigning nil to an array slice removes
      # the elements from the array and then inserts the
      # nil element there.

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[0,1] = nil)
      assert_equal(@cls[nil] + @cls[*(1..99).to_a], a)

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[10,10] = nil)
      assert_equal(@cls[*(0..9).to_a] + @cls[nil] + @cls[*(20..99).to_a], a)

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[-1, 1] = nil)
      assert_equal(@cls[*(0..98).to_a] + @cls[nil], a)

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[-10, 10] = nil)
      assert_equal(@cls[*(0..89).to_a] + @cls[nil], a)

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[0,1000] = nil)
      assert_equal(@cls[nil], a)

      a = @cls[*(0..99).to_a]
      assert_equal(nil, a[10..19] = nil)
      assert_equal(@cls[*(0..9).to_a] + @cls[nil] + @cls[*(20..99).to_a], a)
    end

    a = @cls[1, 2, 3]
    a[1, 0] = a
    assert_equal([1, 1, 2, 3, 2, 3], a)

    a = @cls[1, 2, 3]
    a[-1, 0] = a
    assert_equal([1, 2, 1, 2, 3, 3], a)
  end

  def test_index_real
    a = @cls[ 'cat', 99, /a/, 99, @cls[ 1, 2, 3] ]
    assert_equal(0, a.index('cat'))
    assert_equal(1, a.index(99))
    assert_equal(4, a.index([1,2,3]))
    assert_nil(a.index('ca'))
    assert_nil(a.index([1,2]))
  end

  def test_indexes
    # TODO: raise NotImplementedError, 'Need to write test_indexes'
  end

  def test_indices
    # TODO: raise NotImplementedError, 'Need to write test_indices'
  end

  def test_initialize
    # no argument
    a = @cls.new()
    assert_instance_of(@cls, a)
    assert_equal(0, a.length)
    assert_nil(a[0])

    # a length argument
    a = @cls.new(3)
    assert_instance_of(@cls, a)
    assert_equal(3, a.length)
    assert_nil(a[0])
    assert_nil(a[1])
    assert_nil(a[2])

    # length and value arguments
    a = @cls.new(3, "value")
    assert_instance_of(@cls, a)
    assert_equal(3, a.length)
    assert_equal("value", a[0])
    assert_equal("value", a[1])
    assert_equal("value", a[2])

    # length and value arguments:
    # even same instance in all array entries
    v = ["some", "complex", "value"]
    a = @cls.new(3, v)
    assert_instance_of(@cls, a)
    assert_equal(3, a.length)
    assert_same(v, a[0])
    assert_same(v, a[1])
    assert_same(v, a[2])

    # one argument that is an array:
    # like a "replace"
    v = @cls["some", "array", "value"]
    a = @cls.new(v)
    assert_instance_of(@cls, a)
    assert_equal(3, a.length)
    assert_not_same(v, a)
    assert_same(v[0], a[0])
    assert_same(v[1], a[1])
    assert_same(v[2], a[2])

    # with a block
    a = @cls.new(3) {|i| [i + 10] }
    assert_instance_of(@cls, a)
    assert_equal(3, a.length)
    3.times {|i| assert_equal([i + 10], a[i]) }

    # error cases
    assert_raises(ArgumentError) { @cls.new(-10) }
    
  end

  def generic_index_test(symbol) # TODO: rename
    a = @cls[*('a'..'j').to_a]
    assert_equal(@cls['a', 'c', 'e'], a.send(symbol, 0, 2, 4))
    assert_equal(@cls['j', 'h', 'f'], a.send(symbol,-1, -3, -5))
    assert_equal(@cls['h', nil, 'a'], a.send(symbol,-3, 99, 0))
  end

  def test_insert
    x = %w(a b c)
    assert_equal(%w(a b c), x.insert(2))
    assert_equal(%w(a b x c), x.insert(2, 'x'))
    assert_equal(%w(a b x c), x)
    assert_equal(%w(a b x y z w c), x.insert(-2, 'y', 'z', 'w'))
    assert_equal(%w(a b x y z w c) + [nil, nil] + %w(Q), x.insert(9, 'Q'))
    assert_raises(IndexError) { x.insert(-12, 'Z') }
  end

  def test_inspect
    # TODO: raise NotImplementedError, 'Need to write test_inspect'
  end

  def test_join
    $, = ""
    a = @cls[]
    assert_equal("", a.join)
    assert_equal("", a.join(','))

    $, = ""
    a = @cls[1, 2]
    assert_equal("12", a.join)
    assert_equal("1,2", a.join(','))

    $, = ""
    a = @cls[1, 2, 3]
    assert_equal("123", a.join)
    assert_equal("1,2,3", a.join(','))

    $, = ":"
    a = @cls[1, 2, 3]
    assert_equal("1:2:3", a.join)
    assert_equal("1,2,3", a.join(','))

    $, = ""
  end

  def test_last
    assert_equal(5,   @cls[3,4,5].last)
    assert_equal(4,   @cls[3,4].last)
    assert_equal(3,   @cls[3].last)
    assert_equal(nil, @cls[].last)

    # with argument
    assert_equal([],      @cls[3,4,5].last(0))
    assert_equal([5],     @cls[3,4,5].last(1))
    assert_equal([4,5],   @cls[3,4,5].last(2))
    assert_equal([3,4,5], @cls[3,4,5].last(3))
    assert_equal([3,4,5], @cls[3,4,5].last(4))
    assert_equal([3,4,5], @cls[3,4,5].last(999))

    # error cases
    assert_raises(ArgumentError) { @cls[3,4,5].last(-1) }
    assert_raises(TypeError)     { @cls[3,4,5].last(Object.new) }

    # misc
    assert_equal(nil, @cls[].last)
    assert_equal(1, @cls[1].last)
    assert_equal(99, @cls[*(3..99).to_a].last)
  end

  def test_length
    assert_equal(0, @cls[].length)
    assert_equal(1, @cls[1].length)
    assert_equal(2, @cls[1, nil].length)
    assert_equal(2, @cls[nil, 1].length)
    assert_equal(234, @cls[*(0..233).to_a].length)
  end

  def test_lt2
    a = @cls[]
    res = a << 1
    assert_equal(@cls[1], a)
    assert_same(a, res)

    res = a << 2 << 3
    assert_equal(@cls[1, 2, 3], a)
    assert_same(a, res)

    res = a << nil << 'cat'
    assert_equal(@cls[1, 2, 3, nil, 'cat'], a)
    assert_same(a, res)

    res = a << a
    assert_equal(@cls[1, 2, 3, nil, 'cat', a], a)
    assert_same(a, res)
  end

  def test_map_bang
    util_collect_bang(:map!)
  end

  def test_minus
    assert_equal(@cls[],  @cls[1] - @cls[1])
    assert_equal(@cls[1], @cls[1, 2, 3, 4, 5] - @cls[2, 3, 4, 5])
    assert_equal(@cls[1,1,1,1], @cls[1, 2, 1, 3, 1, 4, 1, 5] - @cls[2, 3, 4, 5])
    a = @cls[]
    to_contrast = @cls[]
    1000.times { a << 1 }
    1000.times { to_contrast << 1}
    assert_equal(1000, a.length)
    assert_equal(to_contrast, a - @cls[2])
    assert_equal(@cls[1,1],  @cls[ 1, 2, 1] - @cls[2])
    assert_equal(@cls[1, 2, 3], @cls[1, 2, 3] - @cls[4, 5, 6])
  end

  def test_nitems
    assert_equal(0, @cls[].nitems)
    assert_equal(1, @cls[1].nitems)
    assert_equal(1, @cls[1, nil].nitems)
    assert_equal(1, @cls[nil, 1].nitems)
    assert_equal(3, @cls[1, nil, nil, 2, nil, 3, nil].nitems)
  end

  def test_or
    assert_equal(@cls[],  @cls[]  | @cls[])
    assert_equal(@cls[1], @cls[1] | @cls[])
    assert_equal(@cls[1], @cls[]  | @cls[1])
    assert_equal(@cls[1], @cls[1] | @cls[1])

    assert_equal(@cls[1,2], @cls[1] | @cls[2])
    assert_equal(@cls[1,2], @cls[1, 1] | @cls[2, 2])
    assert_equal(@cls[1,2], @cls[1, 2] | @cls[1, 2])
  end

  def test_plus
    assert_equal(@cls[],     @cls[]  + @cls[])
    assert_equal(@cls[1],    @cls[1] + @cls[])
    assert_equal(@cls[1],    @cls[]  + @cls[1])
    assert_equal(@cls[1, 1], @cls[1] + @cls[1])
    assert_equal(@cls['cat', 'dog', 1, 2, 3], %w(cat dog) + (1..3).to_a)
  end

  def test_pop
    a = @cls[ 'cat', 'dog' ]
    assert_equal('dog', a.pop)
    assert_equal(@cls['cat'], a)
    assert_equal('cat', a.pop)
    assert_equal(@cls[], a)
    assert_nil(a.pop)
    assert_equal(@cls[], a)
  end

  def test_push
    a = @cls[1, 2, 3]
    assert_equal(@cls[1, 2, 3, 4, 5], a.push(4, 5))
    assert_equal(@cls[1, 2, 3, 4, 5], a.push())
    assert_equal(@cls[1, 2, 3, 4, 5, nil], a.push(nil))
  end

  def test_rassoc
    a1 = @cls[*%w( cat  feline )]
    a2 = @cls[*%w( dog  canine )]
    a3 = @cls[*%w( mule asinine )]
    a4 = @cls[*%w( mule asinine )]
    a  = @cls[ a1, a2, a3, a4 ]

    assert_same(a1,  a.rassoc('feline'))
    assert_same(a3,  a.rassoc('asinine'))
    assert_same(nil, a.rassoc('dog'))
    assert_same(nil, a.rassoc('mule'))
    assert_same(nil, a.rassoc(1..2))
  end

  # almost identical to delete_if
  def test_reject_bang
    a = @cls[ 1, 2, 3, 4, 5 ]
    assert_equal(nil, a.reject! { false }) # delete_if returns a here
    assert_equal(@cls[1, 2, 3, 4, 5], a)

    a = @cls[ 1, 2, 3, 4, 5 ]
    assert_equal(a, a.reject! { true })
    assert_equal(@cls[], a)

    a = @cls[ 1, 2, 3, 4, 5 ]
    assert_equal(a, a.reject! { |i| i > 3 })
    assert_equal(@cls[1, 2, 3], a)
  end

  def test_replace
    a = @cls[ 1, 2, 3]
    a_id = a.__id__
    assert_equal(@cls[4, 5, 6], a.replace(@cls[4, 5, 6]))
    assert_equal(@cls[4, 5, 6], a)
    assert_equal(a_id, a.__id__)
    assert_equal(@cls[], a.replace(@cls[]))
  end

  def test_reverse
    a = @cls[*%w( dog cat bee ant )]
    assert_equal(@cls[*%w(ant bee cat dog)], a.reverse)
    assert_equal(@cls[*%w(dog cat bee ant)], a)
    assert_equal(@cls[], @cls[].reverse)
  end

  def test_reverse_bang
    a = @cls[*%w( dog cat bee ant )]
    assert_equal(@cls[*%w(ant bee cat dog)], a.reverse!)
    assert_equal(@cls[*%w(ant bee cat dog)], a)
    assert_equal(@cls[],@cls[].reverse!)
  end

  def test_reverse_each
    a = @cls[*%w( dog cat bee ant )]
    i = a.length
    a.reverse_each { |e|
      i -= 1
      assert_equal(a[i], e)
    }
    assert_equal(0, i)

    a = @cls[]
    i = 0
    a.reverse_each { |e|
      assert(false, "Never get here")
    }
    assert_equal(0, i)
  end

  def test_rindex
    a = @cls[ 'cat', 99, /a/, 99, [ 1, 2, 3] ]
    assert_equal(0, a.rindex('cat'))
    assert_equal(3, a.rindex(99))
    assert_equal(4, a.rindex([1,2,3]))
    assert_nil(a.rindex('ca'))
    assert_nil(a.rindex([1,2]))
  end

  def test_select
    # TODO: raise NotImplementedError, 'Need to write test_select'
  end

  def test_shift
    a = @cls[ 'cat', 'dog' ]
    assert_equal('cat', a.shift)
    assert_equal(@cls['dog'], a)
    assert_equal('dog', a.shift)
    assert_equal(@cls[], a)
    assert_nil(a.shift)
    assert_equal(@cls[], a)
  end

  def test_size
    assert_equal(0,   @cls[].size)
    assert_equal(1,   @cls[1].size)
    assert_equal(100, @cls[*(0..99).to_a].size)
  end

  def test_slice
    a = @cls[*(1..100).to_a]

    assert_equal(1, a.slice(0))
    assert_equal(100, a.slice(99))
    assert_nil(a.slice(100))
    assert_equal(100, a.slice(-1))
    assert_equal(99,  a.slice(-2))
    assert_equal(1,   a.slice(-100))
    assert_nil(a.slice(-101))

    assert_equal(@cls[1],   a.slice(0,1))
    assert_equal(@cls[100], a.slice(99,1))
    assert_equal(@cls[],    a.slice(100,1))
    assert_equal(@cls[100], a.slice(99,100))
    assert_equal(@cls[100], a.slice(-1,1))
    assert_equal(@cls[99],  a.slice(-2,1))

    assert_equal(@cls[10, 11, 12], a.slice(9, 3))
    assert_equal(@cls[10, 11, 12], a.slice(-91, 3))

    assert_equal(@cls[1],   a.slice(0..0))
    assert_equal(@cls[100], a.slice(99..99))
    assert_equal(@cls[],    a.slice(100..100))
    assert_equal(@cls[100], a.slice(99..200))
    assert_equal(@cls[100], a.slice(-1..-1))
    assert_equal(@cls[99],  a.slice(-2..-2))

    assert_equal(@cls[10, 11, 12], a.slice(9..11))
    assert_equal(@cls[10, 11, 12], a.slice(-91..-89))
    
    assert_nil(a.slice(10, -3))
    assert_equal([], a.slice(10..7))
  end

  def test_slice_bang
    a = @cls[1, 2, 3, 4, 5]
    assert_equal(3, a.slice!(2))
    assert_equal(@cls[1, 2, 4, 5], a)

    a = @cls[1, 2, 3, 4, 5]
    assert_equal(4, a.slice!(-2))
    assert_equal(@cls[1, 2, 3, 5], a)

    a = @cls[1, 2, 3, 4, 5]
    assert_equal(@cls[3,4], a.slice!(2,2))
    assert_equal(@cls[1, 2, 5], a)

    a = @cls[1, 2, 3, 4, 5]
    assert_equal(@cls[4,5], a.slice!(-2,2))
    assert_equal(@cls[1, 2, 3], a)

    a = @cls[1, 2, 3, 4, 5]
    assert_equal(@cls[3,4], a.slice!(2..3))
    assert_equal(@cls[1, 2, 5], a)

    a = @cls[1, 2, 3, 4, 5]
    assert_equal(nil, a.slice!(20))
    assert_equal(@cls[1, 2, 3, 4, 5], a)
  end

  def test_sort
    a = @cls[ 4, 1, 2, 3 ]
    assert_equal(@cls[1, 2, 3, 4], a.sort)
    assert_equal(@cls[4, 1, 2, 3], a)

    assert_equal(@cls[4, 3, 2, 1], a.sort { |x, y| y <=> x} )
    assert_equal(@cls[4, 1, 2, 3], a)

    a.fill(1)
    assert_equal(@cls[1, 1, 1, 1], a.sort)
    
    assert_equal(@cls[], @cls[].sort)
  end

  def test_sort_bang
    a = @cls[ 4, 1, 2, 3 ]
    assert_equal(@cls[1, 2, 3, 4], a.sort!)
    assert_equal(@cls[1, 2, 3, 4], a)

    assert_equal(@cls[4, 3, 2, 1], a.sort! { |x, y| y <=> x} )
    assert_equal(@cls[4, 3, 2, 1], a)

    a.fill(1)
    assert_equal(@cls[1, 1, 1, 1], a.sort!)

    assert_equal(@cls[1], @cls[1].sort!)
    assert_equal(@cls[], @cls[].sort!)
  end

  def test_spaceship
    assert_equal(0,  @cls[] <=> @cls[])
    assert_equal(0,  @cls[1] <=> @cls[1])
    assert_equal(0,  @cls[1, 2, 3, 'cat'] <=> @cls[1, 2, 3, 'cat'])
    assert_equal(-1, @cls[] <=> @cls[1])
    assert_equal(1,  @cls[1] <=> @cls[])
    assert_equal(-1, @cls[1, 2, 3] <=> @cls[1, 2, 3, 'cat'])
    assert_equal(1,  @cls[1, 2, 3, 'cat'] <=> @cls[1, 2, 3])
    assert_equal(-1, @cls[1, 2, 3, 'cat'] <=> @cls[1, 2, 3, 'dog'])
    assert_equal(1,  @cls[1, 2, 3, 'dog'] <=> @cls[1, 2, 3, 'cat'])

    assert_equal(0,  @cls[1] <=> @cls[1.000])
    assert_equal(-1, @cls[1] <=> @cls[1.001])
    assert_equal(1,  @cls[1] <=> @cls[0.999])

    # nested arrays
    assert_equal(0,  @cls[1, @cls[0, 3], 5] <=> @cls[1, @cls[0, 3], 5])
    assert_equal(1,  @cls[1, @cls[0, 3], 5] <=> @cls[1, @cls[0, 2], 5])
    assert_equal(-1, @cls[1, @cls[0, 3], 5] <=> @cls[1, @cls[0, 4], 5])
    assert_equal(1,  @cls[1, @cls[0, 3], 5] <=> @cls[1, @cls[0, 3], 4])
    assert_equal(-1, @cls[1, @cls[0, 3], 5] <=> @cls[1, @cls[0, 3], 6])

    # error cases
    assert_equal(nil, @cls[1,2,3] <=> @cls[1, "two", 3])

    # FIXME: JRUBY-1354 SystemStackError is untestable, because it is unpredictable
    #a = @cls[1,2,3]
    #a.push(a)
    #assert_raises(SystemStackError) { a <=> a }
  end

  def test_times
    assert_equal(@cls[], @cls[]*3)
    assert_equal(@cls[1, 1, 1], @cls[1]*3)
    assert_equal(@cls[1, 2, 1, 2, 1, 2], @cls[1, 2]*3)
    assert_equal(@cls[], @cls[1, 2, 3] * 0)
    assert_raises(ArgumentError) { @cls[1, 2]*(-3) }
    assert_raises(TypeError) { @cls[1, 2]*(Object.new) }

    assert_equal('1-2-3-4-5', @cls[1, 2, 3, 4, 5] * '-')
    assert_equal('12345',     @cls[1, 2, 3, 4, 5] * '')

  end

  def test_to_a
    a = @cls[ 1, 2, 3 ]
    a_id = a.__id__
    assert_equal(a, a.to_a)
    assert_equal(a_id, a.to_a.__id__)
  end

  def test_to_ary
    a = [ 1, 2, 3 ]
    b = @cls[*a]

    a_id = a.__id__
    assert_equal(a, b.to_ary)
    if (@cls == Array)
      assert_equal(a_id, a.to_ary.__id__)
    end
  end

  def test_to_s
    $, = ""
    a = @cls[]
    assert_equal("", a.to_s)

    $, = ""
    a = @cls[1, 2]
    assert_equal("12", a.to_s)

    $, = ""
    a = @cls[1, 2, 3]
    assert_equal("123", a.to_s)

    $, = ":"
    a = @cls[1, 2, 3]
    assert_equal("1:2:3", a.to_s)

    $, = ""
  end

  def test_transpose
    # normal case
    ary = [%w(a b c), %w(d e f), %w(g h i), %w(j k l)]
    exp = [%w(a d g j), %w(b e h k), %w(c f i l)]
    assert_equal(exp, ary.transpose)
    assert_equal(ary, ary.transpose.transpose)
    # the following happens in case of malformed data
    e = assert_raises(IndexError) { [%w(a b c), %w(d)].transpose }
    assert_match(/1 should be 3/, e.message)
  end

  def test_uniq
    a = @cls[ 1, 2, 3, 2, 1, 2, 3, 4, nil ]
    b = a.dup
    assert_equal(@cls[1, 2, 3, 4, nil], a.uniq)
    assert_equal(b, a)

    assert_equal(@cls[1, 2, 3], @cls[1, 2, 3].uniq)
  end

  def test_uniq_bang
    a = @cls[ 1, 2, 3, 2, 1, 2, 3, 4, nil ]
    assert_equal(@cls[1, 2, 3, 4, nil], a.uniq!)
    assert_equal(@cls[1, 2, 3, 4, nil], a)

    assert_nil(@cls[1, 2, 3].uniq!)
  end

  def test_unshift
    a = @cls[]
    assert_equal(@cls['cat'], a.unshift('cat'))
    assert_equal(@cls['dog', 'cat'], a.unshift('dog'))
    assert_equal(@cls[nil, 'dog', 'cat'], a.unshift(nil))
    assert_equal(@cls[@cls[1,2], nil, 'dog', 'cat'], a.unshift(@cls[1, 2]))
  end

  def test_values_at
    generic_index_test(:values_at)
  end

  def test_values_at_extra_wtf # FIX
    assert_equal([],         %w(a b c d e).values_at)
    assert_equal(%w(c),      %w(a b c d e).values_at(2))
    assert_equal(%w(b d),    %w(a b c d e).values_at(1, 3))
    assert_equal([nil, nil], %w(a b c d e).values_at(5, 300))
    assert_equal(%w(e),      %w(a b c d e).values_at(-1))
    assert_equal(%w(d b),    %w(a b c d e).values_at(-2, -4))
    assert_equal([nil, nil], %w(a b c d e).values_at(-6, -500))
    assert_equal(%w(a c e),  %w(a b c d e).values_at(0.0, 2.0, 4.0)) # float as index
    assert_equal(%w(b),      %w(a b c d e).values_at(1.8)) # float as index
    assert_equal(%w(b c d),  %w(a b c d e).values_at(1..3))
    assert_raises(TypeError) { %w(a b c).values_at(nil) }
    assert_raises(TypeError) { %w(a b c).values_at("x") }
    assert_raises(TypeError) { %w(a b c).values_at([]) }
    assert_raises(TypeError) { %w(a b c).values_at(true) }
  end

  def test_zip
    a = @cls[4, 5, 6]
    b = @cls[7, 8, 9]

    assert_equal([[1, 4, 7], [2, 5, 8], [3, 6, 9]], @cls[1, 2, 3].zip(a, b))
    assert_equal([[1, 4, 7], [2, 5, 8]], @cls[1, 2].zip(a, b))
    assert_equal([[4, 1, 8], [5, 2, nil], [6, nil, nil]], a.zip([1, 2], [8]))
  end

# TODO: question these:
#   def ===(*args)
#     raise NotImplementedError, 'Need to write ==='
#   end

#   def clone(*args)
#     raise NotImplementedError, 'Need to write clone'
#   end

#   def dup(*args)
#     raise NotImplementedError, 'Need to write dup'
#   end
end

