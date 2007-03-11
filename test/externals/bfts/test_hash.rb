require 'test/unit'
require 'rubicon_testcase'

class TestHash < RubiconTestCase

  def setup
    @h = Hash[
      1, 'one',
      2, 'two',
      3, 'three',
      self, 'self',
      true, 'true',
      nil, 'nil',
      'nil', nil
    ]
  end

  def shut_it
    $VERBOSE = nil
    yield
  ensure
    $VERBOSE = true
  end

  def util_index_tester(symbol)
    res = @h.send symbol, *%w( dog cat horse ) 
    assert_equal([nil, nil, nil], res)

    res = @h.send symbol
    assert_equal([], res)

    res = @h.send symbol, 3, 2, 1, nil 
    assert_equal(%w( three two one nil ), res)

    res = @h.send symbol, 3, 99, 1, nil 
    assert_equal([ 'three', nil, 'one', 'nil' ], res)
  end

  def util_update_tester(symbol)
    h1 = Hash[ 1, 2, 2, 3,     3, 4 ]
    h2 = Hash[       2, 'two',       4, 'four' ]

    ha = Hash[ 1, 2, 2, 'two', 3, 4, 4, 'four' ]
    hb = Hash[ 1, 2, 2, 3,     3, 4, 4, 'four' ]

    assert_equal(ha, h1.send(symbol, h2))
    assert_equal(ha, h1)

    h1 = Hash[ 1, 2, 2, 3, 3, 4 ]
    h2 = Hash[ 2, 'two', 4, 'four' ]

    assert_equal(hb, h2.send(symbol, h1))
    assert_equal(hb, h2)
  end

  def test_class_index # Hash[blahblah]
    h = Hash["a", 100, "b", 200]
    assert_equal(100, h['a'])
    assert_equal(200, h['b'])
    assert_nil(h['c'])

    h = Hash.[]("a", 100, "b", 200)
    assert_equal(100, h['a'])
    assert_equal(200, h['b'])
    assert_nil(h['c'])
  end

  def test_clear
    assert(@h.size > 0)
    @h.clear
    assert_equal(0, @h.size)
    assert_nil(@h[1])
  end

  def util_clone(meth)
    for taint in [ false, true ]
      for frozen in [ false, true ]
        desc = "#{taint}:#{frozen}"
        a = @h.send(meth)
        a.taint  if taint
        a.freeze if frozen
        b = a.send(meth)

        assert_equal(a, b, "equal: " + desc)
        assert_not_equal(a.__id__, b.__id__, "id: " + desc)
        exp_frozen = case meth
                     when :dup then
                       false
                     when :clone then
                       a.frozen?
                     else
                       raise "Unknown method #{meth}"
                     end
        assert_equal(exp_frozen, b.frozen?,  "frozen: " + desc)
        assert_equal(a.tainted?, b.tainted?, "tainted: " + desc)
      end
    end
  end

  def test_clone
    util_clone(:clone)
  end

  def test_default
    assert_nil(@h.default)
    h = Hash.new(:xyzzy)
    assert_equal(:xyzzy, h.default)
  end

  def test_default_equals
    assert_nil(@h.default)
    @h.default = :xyzzy
    assert_equal(:xyzzy, @h.default)
  end

  def test_default_proc
    last = nil
    h = Hash.new { |hash, k| last=[hash.inspect, k] }
    assert_not_nil(h.default_proc)
    assert_equal(["{}",42], h[42])
    h[3] = -3
    assert_equal(["{3=>-3}",42], h[42])
    h[42] = 666
    assert_equal(666, h[42])
    assert_equal(["{3=>-3, 42=>666}",43], h[43])
    last = nil
    h[42] = nil
    assert_nil(last)
    assert_equal(nil, h[42])
    last = nil
    h.delete(42)
    h.delete(3)
    assert_nil(last)
    last = nil
    assert_equal(["{}",42], h[42])
    assert_equal(["{}",42], last)
  end

  def test_delete
    h1 = Hash[ 1, 'one', 2, 'two', true, 'true' ]
    h2 = Hash[ 1, 'one', 2, 'two' ]
    h3 = Hash[ 2, 'two' ]

    assert_equal('true', h1.delete(true))
    assert_equal(h2, h1)

    assert_equal('one', h1.delete(1))
    assert_equal(h3, h1)

    assert_equal('two', h1.delete(2))
    assert_equal(Hash[], h1)

    assert_nil(h1.delete(99))
    assert_equal(Hash[], h1)

    assert_equal('default 99', h1.delete(99) {|i| "default #{i}" })
  end

  def test_delete_if
    base = Hash[ 1, 'one', 2, false, true, 'true', 'cat', 99 ]
    h1   = Hash[ 1, 'one', 2, false, true, 'true' ]
    h2   = Hash[ 2, false, 'cat', 99 ]
    h3   = Hash[ 2, false ]

    h = base.dup
    assert_equal(h, h.delete_if { false })
    h.delete_if { true }
    assert_equal(Hash[], h)

    h = base.dup
    assert_equal(h1, h.delete_if {|k,v| k.instance_of?(String) })
    assert_equal(h1, h)

    h = base.dup
    assert_equal(h2, h.delete_if {|k,v| v.instance_of?(String) })
    assert_equal(h2, h)

    h = base.dup
    assert_equal(h3, h.delete_if {|k,v| v })
    assert_equal(h3, h)
  end

  def test_dup
    util_clone(:dup)
  end

  def test_each
    count = 0
    Hash[].each { |k, v| count + 1 }
    assert_equal(0, count)

    # constructive test, should always work.
    h = @h
    r = Hash[]
    h.each do |k, v|
      r[k] = v
    end
    assert_equal(r, h)

    # destructive test, probably stupid.
    h = @h
    h.each do |k, v|
      assert_equal(v, h.delete(k))
    end
    assert_equal(Hash[], h)
  end

  def test_each_key
    count = 0
    Hash[].each_key { |k| count + 1 }
    assert_equal(0, count)

    h = @h
    h.each_key do |k|
      h.delete(k)
    end
    assert_equal(Hash[], h)
  end

  def test_each_pair
    count = 0
    Hash[].each_pair { |k, v| count + 1 }
    assert_equal(0, count)

    # constructive test, should always work.
    h = @h
    r = Hash[]
    h.each do |k, v|
      r[k] = v
    end
    assert_equal(h, r)

    # destructive test, probably stupid.
    h = @h
    h.each_pair do |k, v|
      assert_equal(v, h.delete(k))
    end
    assert_equal(Hash[], h)
  end

  def test_each_value
    res = []
    Hash[].each_value { |v| res << v }
    assert_equal(0, [].length)

    @h.each_value { |v| res << v }
    assert_equal(0, [].length)

    expected = []
    @h.each { |k, v| expected << v }

    assert_equal([], expected - res)
    assert_equal([], res - expected)
  end

  def test_empty_eh
    assert(Hash[].empty?)
    assert(!@h.empty?)
  end

  def test_equals2 # '=='
    h1 = Hash[ "a", 1, "c", 2 ]
    h2 = Hash[ "a", 1, "c", 2, 7, 35 ]
    h3 = Hash[ "a", 1, "c", 2, 7, 35 ]
    h4 = Hash[ ]
    assert(h1 == h1)
    assert(h2 == h2)
    assert(h3 == h3)
    assert(h4 == h4)
    assert(!(h1 == h2))
    assert(h2 == h3)
    assert(!(h3 == h4))
  end

  def test_fetch
    assert_raise(IndexError) { Hash[].fetch(1) }
    assert_raise(IndexError) { @h.fetch('gumby') }
    assert_equal('gumbygumby',     @h.fetch('gumby') {|k| k*2} )
    assert_equal('pokey',          @h.fetch('gumby', 'pokey'))

    assert_equal('one', @h.fetch(1))
    assert_equal(nil,   @h.fetch('nil'))
    assert_equal('nil', @h.fetch(nil))
  end


  def test_has_key_eh
    assert(!Hash[].has_key?(1))
    assert(!Hash[].has_key?(nil))
    assert(@h.has_key?(nil))
    assert(@h.has_key?(1))
    assert(!@h.has_key?('gumby'))
  end

  def test_has_value_eh
    assert(!Hash[].has_value?(1))
    assert(!Hash[].has_value?(nil))
    assert(@h.has_value?('one'))
    assert(@h.has_value?(nil))
    assert(!@h.has_value?('gumby'))
  end

  def test_include_eh
    assert(!Hash[].include?(1))
    assert(!Hash[].include?(nil))
    assert(@h.include?(nil))
    assert(@h.include?(1))
    assert(!@h.include?('gumby'))
  end

  def test_index # '[]'
    t = Time.now
    h = Hash[1, 'one', 2, 'two', 3, 'three',
      self, 'self',
      t, 'time',
      nil, 'nil',
      'nil', nil
    ]

    assert_equal('one',   h[1])
    assert_equal('two',   h[2])
    assert_equal('three', h[3])
    assert_equal('self',  h[self])
    assert_equal('time',  h[t])
    assert_equal('nil',   h[nil])
    assert_equal(nil,     h['nil'])
    assert_equal(nil,     h['koala'])

    h1 = h.dup
    h1.default = :default

    assert_equal('one',    h1[1])
    assert_equal('two',    h1[2])
    assert_equal('three',  h1[3])
    assert_equal('self',   h1[self])
    assert_equal('time',   h1[t])
    assert_equal('nil',    h1[nil])
    assert_equal(nil,      h1['nil'])
    assert_equal(:default, h1['koala'])
  end

  def test_index_equals # '[]='
    t = Time.now
    h = Hash.new
    h[1]     = 'one'
    h[2]     = 'two'
    h[3]     = 'three'
    h[self]  = 'self'
    h[t]     = 'time'
    h[nil]   = 'nil'
    h['nil'] = nil
    assert_equal('one',   h[1])
    assert_equal('two',   h[2])
    assert_equal('three', h[3])
    assert_equal('self',  h[self])
    assert_equal('time',  h[t])
    assert_equal('nil',   h[nil])
    assert_equal(nil,     h['nil'])
    assert_equal(nil,     h['koala'])

    h[1] = 1
    h[nil] = 99
    h['nil'] = nil
    z = [1,2]
    h[z] = 256
    assert_equal(1,       h[1])
    assert_equal('two',   h[2])
    assert_equal('three', h[3])
    assert_equal('self',  h[self])
    assert_equal('time',  h[t])
    assert_equal(99,      h[nil])
    assert_equal(nil,     h['nil'])
    assert_equal(nil,     h['koala'])
    assert_equal(256,     h[z])
  end

  def test_index_literal
    assert_equal(1,     @h.index('one'))
    assert_equal(nil,   @h.index('nil'))
    assert_equal('nil', @h.index(nil))

    assert_equal(nil,   @h.index('gumby'))
    assert_equal(nil,   Hash[].index('gumby'))
  end

  def test_indexes
    shut_it do
      util_index_tester(:indexes)
    end
  end
  
  def test_indices
    shut_it do
      util_index_tester(:indices)
    end
  end

  def test_initialize
    h = Hash.new
    assert_instance_of(Hash, h)
    assert_nil(h.default)
    assert_nil(h['spurious'])

    h = Hash.new('default')
    assert_instance_of(Hash, h)
    assert_equal('default', h.default)
    assert_equal('default', h['spurious'])
    
  end

  def test_inspect
    assert_equal("{}", Hash[].inspect)
    @h.delete(self)
    s = @h.inspect
    s = "{" + s[1..-2].split(/, /).sort.join(', ') + "}"
    assert_equal("{\"nil\"=>nil, 1=>\"one\", 2=>\"two\", 3=>\"three\", nil=>\"nil\", true=>\"true\"}", s)
  end

  def test_invert
    h = @h.invert
    assert_equal(1, h['one'])
    assert_equal(true, h['true'])
    assert_equal(nil,  h['nil'])

    h.each do |k, v|
      assert(@h.has_key?(v))    # not true in general, but works here
    end

    h = Hash[ 'a', 1, 'b', 2, 'c', 1].invert
    assert_equal(2, h.length)
    assert(h[1] == 'a' || h[1] == 'c')
    assert_equal('b', h[2])
  end

  def test_key_eh
    assert(!Hash[].key?(1))
    assert(!Hash[].key?(nil))
    assert(@h.key?(nil))
    assert(@h.key?(1))
    assert(!@h.key?('gumby'))
  end

  def test_keys
    assert_equal([], Hash[].keys)

    keys = @h.keys
    expected = []
    @h.each { |k, v| expected << k }
    assert_equal([], keys - expected)
    assert_equal([], expected - keys)
  end

  def test_length
    assert_equal(0, Hash[].length)
    assert_equal(7, @h.length)
  end

  def test_member_eh
    assert(!Hash[].member?(1))
    assert(!Hash[].member?(nil))
    assert(@h.member?(nil))
    assert(@h.member?(1))
    assert(!@h.member?('gumby'))
  end

  def test_merge
    h1 = Hash[ 1, 2, 2, 3, 3, 4 ]
    h2 = Hash[ 2, 'two', 4, 'four' ]
    hmerge = Hash[ 1, 2, 2, 'two', 3, 4, 4, 'four' ]
    h1clone = h1.clone
    assert_equal(hmerge, h1.merge(h2))
    assert_equal(h1clone, h1)
    assert_not_equal(hmerge, h1)

    h1 = Hash[ 1, 2, 2, 3, 3, 4 ]
    h2 = Hash[ 2, 'two', 4, 'four' ]
    hmerge = Hash[ 1, 2, 2, 3, 3, 4, 4, 'four' ]
    h2clone = h2.clone
    assert_equal(hmerge, h2.merge(h1))
    assert_equal(h2clone, h2)
    assert_not_equal(hmerge, h2)
  end

  def test_merge_bang
    h1 = Hash[ "a", 100, "b", 200 ]
    h2 = Hash[ "b", 254, "c", 300 ]
    expected = Hash["a", 100, "b", 254, "c", 300]
    h1clone = h1.clone
    assert_equal(expected, h1.merge!(h2))
    assert_not_equal(h1clone, h1)
  end

  def test_rehash
    a = [ "a", "b" ]
    c = [ "c", "d" ]
    h = Hash[ a, 100, c, 300 ]
    assert_equal(100, h[a])
    a[0] = "z"
# HACK assert_nil(h[a]) # this is current behavior, but not sure if spec
    h.rehash
    assert_equal(100, h[a])

    # TODO: If +Hash#rehash+ is called while an iterator is traversing the hash, an +IndexError+ will be raised in the iterator.

  end

  def test_reject
    base = Hash[ 1, 'one', 2, false, true, 'true', 'cat', 99 ]
    h1   = Hash[ 1, 'one', 2, false, true, 'true' ]
    h2   = Hash[ 2, false, 'cat', 99 ]
    h3   = Hash[ 2, false ]

    h = base.dup
    assert_equal(h, h.reject { false })
    assert_equal(Hash[], h.reject { true })

    h = base.dup
    assert_equal(h1, h.reject {|k,v| k.instance_of?(String) })

    assert_equal(h2, h.reject {|k,v| v.instance_of?(String) })

    assert_equal(h3, h.reject {|k,v| v })
    assert_equal(base, h)
  end

  def test_reject_bang
    base = Hash[ 1, 'one', 2, false, true, 'true', 'cat', 99 ]
    h1   = Hash[ 1, 'one', 2, false, true, 'true' ]
    h2   = Hash[ 2, false, 'cat', 99 ]
    h3   = Hash[ 2, false ]

    h = base.dup
    assert_equal(nil, h.reject! { false })
    assert_equal(Hash[],  h.reject! { true })

    h = base.dup
    assert_equal(h1, h.reject! {|k,v| k.instance_of?(String) })
    assert_equal(h1, h)

    h = base.dup
    assert_equal(h2, h.reject! {|k,v| v.instance_of?(String) })
    assert_equal(h2, h)

    h = base.dup
    assert_equal(h3, h.reject! {|k,v| v })
    assert_equal(h3, h)
  end

  def test_replace
    h = Hash[ 1, 2, 3, 4 ]
    h1 = h.replace(Hash[ 9, 8, 7, 6 ])
    assert_equal(h, h1)
    assert_equal(8, h[9])
    assert_equal(6, h[7])
    assert_nil(h[1])
    assert_nil(h[2])
  end

  def test_select
    h = Hash["a", 100, "b", 200, "c", 300]
    r = h.select {|k,v| k > "a"}
    assert_equal([["b", 200], ["c", 300]], r.sort_by {|o| o.first })
    r = h.select {|k,v| v < 200}
    assert_equal([["a", 100]], r)
  end

  def test_shift
    h = @h.dup
    
    @h.length.times {
      k, v = h.shift
      assert(@h.has_key?(k))
      assert_equal(@h[k], v)
    }

    assert_equal(0, h.length)
  end

  def test_size
    assert_equal(0, Hash[].length)
    assert_equal(7, @h.length)
  end

  def test_sort
    h = Hash.new
    h = h.sort
    assert_equal([], h)

    h = Hash[ 1, 1, 2, 1 ].sort
    assert_equal([[1,1], [2,1]], h)

    h = Hash[ 'cat', 'feline', 'ass', 'asinine', 'bee', 'beeline' ]
    h1 = h.sort
    assert_equal([ %w(ass asinine), %w(bee beeline), %w(cat feline)], h1)
  end

  def test_store
    t = Time.now
    h = Hash.new
    h.store(1, 'one')
    h.store(2, 'two')
    h.store(3, 'three')
    h.store(self, 'self')
    h.store(t,  'time')
    h.store(nil, 'nil')
    h.store('nil', nil)
    assert_equal('one',   h[1])
    assert_equal('two',   h[2])
    assert_equal('three', h[3])
    assert_equal('self',  h[self])
    assert_equal('time',  h[t])
    assert_equal('nil',   h[nil])
    assert_equal(nil,     h['nil'])
    assert_equal(nil,     h['koala'])

    h.store(1, 1)
    h.store(nil,  99)
    h.store('nil', nil)
    assert_equal(1,       h[1])
    assert_equal('two',   h[2])
    assert_equal('three', h[3])
    assert_equal('self',  h[self])
    assert_equal('time',  h[t])
    assert_equal(99,      h[nil])
    assert_equal(nil,     h['nil'])
    assert_equal(nil,     h['koala'])
  end

  def test_to_a
    assert_equal([], Hash[].to_a)
    assert_equal([[1,2]], Hash[ 1, 2 ].to_a)
    a = Hash[ 1, 2, 3, 4, 5, 6 ].to_a
    assert_equal([1,2], a.delete([1,2]))
    assert_equal([3,4], a.delete([3,4]))
    assert_equal([5,6], a.delete([5,6]))
    assert_equal(0, a.length)
  end

  def test_to_hash
    h = @h.to_hash
    assert_equal(@h, h)
  end

  def test_to_s
    h = Hash[ 1, 2, "cat", "dog", 1.5, :fred ]
    assert_equal(h.to_a.join, h.to_s)
    $, = ":"
    assert_equal(h.to_a.join, h.to_s)
    h = Hash[]
    assert_equal(h.to_a.join, h.to_s)
    $, = nil
  end

  def test_update
    util_update_tester(:update)
    util_update_tester(:merge!)
  end

  def test_value_eh
    assert(!Hash[].value?(1))
    assert(!Hash[].value?(nil))
    assert(@h.value?(nil))
    assert(@h.value?('one'))
    assert(!@h.value?('gumby'))
  end

  def test_values
    assert_equal([], Hash[].values)

    vals = @h.values
    expected = []
    @h.each { |k, v| expected << v }
    assert_equal([], vals - expected)
    assert_equal([], expected - vals)
  end

  def test_values_at
    util_index_tester(:values_at)
  end

end
