require File.dirname(__FILE__) + "/../spec_helper"

describe "a java.util.Map instance" do

  it 'return compared_by_identity for IdentityHashMap' do
    h = java.util.HashMap.new
    expect( h.compare_by_identity? ).to be false
    h.compare_by_identity # has no effect
    expect( h.compare_by_identity? ).to be false
    h = java.util.IdentityHashMap.new
    expect( h.compare_by_identity? ).to be true
  end

  it 'digs like a Hash' do
    m3 = java.util.TreeMap.new; m3.put('3'.to_java, obj = java.lang.Object.new)
    m1 = java.util.HashMap.new; m1['1'] = { 2 => m3 }
    expect( m1.dig(1) ).to be nil
    expect( m1.dig('1', 2) ).to be m3
    expect( m1.dig('1', 2, '3') ).to be obj
  end

  it 'compares like a Hash' do
    m1 = java.util.HashMap.new; m1['a'] = 1; m1['b'] = 2
    m2 = java.util.LinkedHashMap.new; m2['b'] = 2; m2['a'] = 1; m2['c'] = 3
    m3 = java.util.Hashtable.new; m3['b'] = 3; m3['a'] = 2
    expect( m1 > m2 ).to be false
    expect( m1 >= m2 ).to be false
    expect( m2 > m1 ).to be true
    expect( m1 <= m2 ).to be true
    expect( m1 > m1 ).to be false
    expect( m3 < m1 ).to be false
    expect( m3 > m1 ).to be false
  end

  it 'compares with a Hash' do
    m1 = Hash.new; m1['a'] = 1; m1['b'] = 2
    m2 = java.util.LinkedHashMap.new; m2['b'] = 2; m2['a'] = 1; m2['c'] = 3
    m3 = java.util.Hashtable.new; m3['b'] = 3; m3['a'] = 2
    expect( m1 > m2 ).to be false
    expect( m1 >= m2 ).to be false

    pending 'TODO need more handling to compare Map-s with Hash-es (as expected)'

    expect( m2 > m1 ).to be true
    expect( m1 <= m2 ).to be true
    expect( m1 > m1 ).to be false
    expect( m3 < m1 ).to be false
    expect( m3 > m1 ).to be false
    expect( m2 >= { 'a' => 1, 'b' => 2 } ).to be true
  end

  it 'fetch-es values' do
    m = java.util.HashMap.new({ '1' => 1, '2' => 2, 3 => '3' })
    expect( m.fetch_values(3) ).to eql [ '3' ]
    expect( m.fetch_values('2', '1') ).to eql [ 2, 1 ]
    expect { m.fetch_values(1) }.to raise_error(KeyError)
  end

  it 'converts to a proc' do
    m = java.util.TreeMap.new({ '1' => 1, '2' => 2 })
    expect( m.to_proc.call('3') ).to be nil
    expect( m.to_proc.call('1') ).to be 1
  end

  it 'handles any?' do
    h = java.util.Hashtable.new; h[1] = 10; h['2'] = 20
    expect( h.any? ).to be true
    expect( h.any? { |e, v| v > 10 } ).to be true
    expect( h.any? { |e, v| v > 20 } ).to be false
    expect( h.any? { |e| e[1] > 10 } ).to be true
  end

  it 'returns self on clear like a Hash (if aliased)' do
    java.util.concurrent.ConcurrentSkipListMap.class_eval { alias clear ruby_clear }
    m = java.util.concurrent.ConcurrentSkipListMap.new({ '1' => 1, '2' => 2 })
    expect( m.clear ).to_not be nil # Java's clear returns void
    expect( m.empty? ).to be true
  end

  it "supports Hash-like operations" do
    h = java.util.HashMap.new
    test_ok(h.kind_of? java.util.Map)
    h.put(1, 2); h.put(3, 4); h.put(5, 6)
    test_equal(4, h[3])
    test_equal(nil, h[10])

    h[7]=8
    test_ok({3=>4, 1=>2, 7=>8, 5=>6} == h)
    test_equal(0, h.clear.size)
    test_equal(Java::JavaUtil::HashMap, h.class)

    h.put("a", 100); h.put("b", 200); h.put("c", 300)
    test_equal(100, h.delete("a"))
    test_equal(nil, h.delete("z"))
    test_equal("z not found", h.delete("z") { |el| "#{el} not found" })
    test_equal({"c"=>300}, h.delete_if { |key, value| key <= "b" })

    h = java.util.concurrent.ConcurrentHashMap.new(10)
    h.put(:b, 200); h.put(:c, 300); h.put(:a, 100)
    test_equal(h.delete_if { |key, value| key <= :b }, {:"c"=>300})
    expect( {:c => 300}.eql? h ).to be true
    h.put(:b, 200)
    expect( {'c'.to_sym => 300}.eql? h ).to be false
    h.remove(:b)
    expect( h.eql?({:c => 300}) ).to be true

    h.clear; h['a'] = 100; h['c'] = 300; h.put('b', 200)

    h2 = java.util.TreeMap.new
    h2.put('b', 200); h2.put('c', 300); h2.put('a', 100)
    test_equal( h2, h )
    expect( h2.eql? h ).to be true
    expect( h2.equal? h ).to be false

    h.remove('c')
    test_equal( h2.delete_if { |key, value| key >= 'c' }, h )

    h = java.util.LinkedHashMap.new
    h.put("a", 100); h.put("b", 200); h.put("c", 300)
    a1=[]; a2=[]
    h.each { |key, value| a1 << key; a2 << value }
    test_equal(["a", "b", "c"], a1)
    test_equal([100, 200, 300], a2)
    a1=[]; a2=[]
    h.each_key { |key| a1 << key }
    h.each_value { |value| a2 << value }
    test_equal(["a", "b", "c"], a1)
    test_equal([100, 200, 300], a2)

    test_ok(h.clear.empty?)

    # Java 8 adds a replace method to Map that takes a key and value
    h.ruby_replace({1=>100})

    test_equal({1=>100}, h)
    h[2]=200; h[3]=300
    test_equal(300, h.fetch(3))
    test_exception(IndexError) { h.fetch(10) }
    test_equal("hello", h.fetch(10, "hello"))
    test_equal("hello 10", h.fetch(10) { |e| "hello #{e}" })
    test_ok(h.has_key?(1))
    test_ok(!h.has_key?(0))
    test_ok(h.has_value?(300))
    test_ok(!h.has_value?(-1))
    test_ok(h.include?(2))
    test_equal({100=>1, 200=>2, 300=>3}, h.invert)
    test_ok(!h.key?(0))
    test_equal([1, 2, 3], h.keys)
    test_ok(!h.value?(0.1))
# java.util.Map has values method. Java's values() is used.
    test_equal("[100, 200, 300]", h.values.to_a.inspect)
    test_equal(3, h.length)
    h.delete(1)
    test_equal(2, h.length)
    expect( h.member?(3) ).to be_truthy
    test_equal(Java::JavaUtil::LinkedHashMap, h.class)

    h1 = java.util.LinkedHashMap.new
    h1.put("a", 100); h1.put("b", 200)
    h2 = java.util.LinkedHashMap.new
    h2.put("b", 254); h2.put("c", 300)
    # Java 8 adds a merge method to Map used for merging multiple values for a given key in-place
    if ENV_JAVA['java.specification.version'] < '1.8'
      test_equal({"a"=>100, "b"=>254, "c"=>300}, h1.merge(h2))
    else
      test_equal({"a"=>100, "b"=>254, "c"=>300}, h1.ruby_merge(h2))
    end
    test_equal({"a"=>100, "b"=>454, "c"=>300}, h1.ruby_merge(h2) { |k, o, n| o+n })
    expect( h1.inspect ).to include "{\"a\"=>100, \"b\"=>200}"

    h1.merge!(h2) { |k, o, n| o }
    test_equal('#<Java::JavaUtil::LinkedHashMap: {"a"=>100, "b"=>200, "c"=>300}>', h1.inspect)
    test_equal(Java::JavaUtil::LinkedHashMap, h1.class)

    h.clear
    h.put(1, 100); h.put(2, 200); h.put(3, 300)
    test_equal({1=>100, 2=>200}, h.reject { |k, v| k > 2 })
    expect( h.inspect ).to include "{1=>100, 2=>200, 3=>300}"
    test_equal({1=>100, 2=>200}, h.reject! { |k, v| k > 2 })
    expect( h.inspect ).to include "{1=>100, 2=>200}"

    # Java 8 adds a replace method to Map that takes a key and value
    test_equal({"c"=>300, "d"=>400, "e"=>500}, h.ruby_replace({"c"=>300, "d"=>400, "e"=>500}))
    test_equal(Java::JavaUtil::LinkedHashMap, h.class)

    test_equal({"d"=>400, "e"=>500}, h.select {|k,v| k > "c"})
    test_equal({"c"=>300}, h.select {|k,v| v < 400})

    # Java 8 adds a replace method to Map that takes a key and value
    if ENV_JAVA['java.specification.version'] < '1.8'
      h.replace({"a"=>20, "d"=>10, "c"=>30, "b"=>0})
    else
      h.ruby_replace({"a"=>20, "d"=>10, "c"=>30, "b"=>0})
    end
    test_equal([["a", 20], ["b", 0], ["c", 30], ["d", 10]], h.sort)
    test_equal([["b", 0], ["d", 10], ["a", 20], ["c", 30]], h.sort { |a, b| a[1]<=>b[1] })

    test_equal(20, h.store("e", 20))
    test_equal([["a", 20], ["d", 10], ["c", 30], ["b", 0], ["e", 20]], h.to_a)
    rh = h.to_hash
    test_equal(Java::JavaUtil::LinkedHashMap, h.class)
    test_equal(Hash, rh.class)

    test_equal("{a=20, d=10, c=30, b=0, e=20}", h.to_s)
    test_equal(h.toString, h.to_s)

    test_ok(h.all? { |k, v| k.length == 1 })
    test_ok(!h.all? { |k, v| v > 100 })
    test_equal([["a", 20], ["d", 10], ["c", 30], ["b", 0], ["e", 20]], h.map{|a|a})
    test_equal([true, false, false, false, false], h.collect { |k, v| k == "a" })
    test_equal([["a", 20], ["d", 10]], h.take(2))

    # Java 8 adds a replace method to Map that takes a key and value
    h.ruby_replace({"a"=>100, "b"=>200})

    h2 = {"b"=>254, "c"=>300}
    test_equal({"a"=>100, "b"=>200, "c"=>300}, h.update(h2) { |k, o, n| o })
    expect( h.inspect ).to include "{\"a\"=>100, \"b\"=>200, \"c\"=>300}"
    test_equal(Java::JavaUtil::LinkedHashMap, h.class)
    test_equal([100, 200], h.values_at("a", "b"))
    test_equal([100, 200, nil], h.values_at("a", "b", "z"))
    h.default = "cat"
    test_equal([100, 200, "cat"], h.values_at("a", "b", "z"))

    h = java.util.HashMap.new
    k1 = [1]
    h[k1] = 1
    k1[0] = 100
    test_equal(nil, h[k1])

    h.put(1, 2); h.put(3, 4);
    test_equal(1, get_hash_key(h, 2))
    test_equal(nil, get_hash_key(h, 10))
    test_equal(nil, h.default_proc)
    h.default = :hello
    test_equal(nil, h.default_proc)
    test_equal(1, get_hash_key(h, 2))
    test_equal(nil, get_hash_key(h, 10))

# java.util.HashMap can't have a block as an arg for its constructor
#h = Hash.new {|h,k| h[k] = k.to_i*10 }

#test_ok(!nil, h.default_proc)
#test_equal(100, h[10])
#test_equal(20, h.default(2))

#behavior change in 1.8.5 led to this:
    h = java.util.HashMap.new
    test_equal(nil, h.default)

    h.default = 5
    test_equal(5, h.default)
    test_equal(nil, h.default_proc)

    test_equal(5, h[12])

###
# Maybe this test doens't work for a Java object.
#class << h
# def default(k); 2; end
#end

#test_equal(nil, h.default_proc)
#test_equal(2, h[30])
###

# test that extensions of the base classes are typed correctly
    class HashExt < java.util.HashMap
    end
    test_equal(HashExt, HashExt.new.class)
# [] method of JavaProxy is used, and the test fails.
#test_equal(HashExt, HashExt[:foo => :bar].class)

###
# no need to test these against java.util.HashMap
# make sure hash yields look as expected (copied from MRI iterator test)

#class H
#  def each
#    yield [:key, :value]
#  end
#end

#[{:key=>:value}, H.new].each {|h|
#  h.each{|a| test_equal([:key, :value], a)}
#  h.each{|*a| test_equal([[:key, :value]], a)}
#  h.each{|k,v| test_equal([:key, :value], [k,v])}
#}

# each_pair should splat args correctly
#{:a=>:b}.each_pair do |*x|
#        test_equal(:a,x[0])
#        test_equal(:b,x[1])
#end
###

# Test hash coercion
    class ToHashImposter
      def initialize(hash)
        @hash = hash
      end

      def to_hash
        @hash
      end
    end

    class SubHash < Hash
    end

    x = java.util.HashMap.new
    x.put(:a, 1); x.put(:b, 2)
    x.update(ToHashImposter.new({:a => 10, :b => 20}))
    test_equal(10, x[:a])
    test_equal(20, x[:b])
    test_exception(TypeError) { x.update(ToHashImposter.new(4)) }

    x.put(:a, 1); x.put(:b, 2)
    sub2 = SubHash.new()
    sub2[:a] = 10
    sub2[:b] = 20
    x.update(ToHashImposter.new(sub2))
    test_equal(10, x[:a])
    test_equal(20, x[:b])

    x.put(:a, 1); x.put(:b, 2)

    # Java 8 adds a replace method to Map that takes a key and value
    if ENV_JAVA['java.specification.version'] < '1.8'
      x.replace(ToHashImposter.new({:a => 10, :b => 20}))
      test_equal(10, x[:a])
      test_equal(20, x[:b])
      test_exception(TypeError) { x.replace(ToHashImposter.new(4)) }

      x.put(:a, 1); x.put(:b, 2)
      x.replace(ToHashImposter.new(sub2))
      test_equal(10, x[:a])
      test_equal(20, x[:b])
    end

    class H1 < java.util.HashMap
    end

    test_no_exception { H1.new.clone }
  end

  it 'converts to_hash' do
    map = java.util.HashMap.new
    map.put(1, '1'); map.put(2, :dva); map.put(3, 3)

    expected = { 1 => '1', 2 => :dva, 3 => 3 }
    expect( h = map.to_hash ).to eql({ 1 => '1', 2 => :dva, 3 => 3 })

    map[4] = 0.1; map[1] = 1
    expect( h[1] ).to eql '1'
    expect( h.key?(4) ).to be false

    expect( map.to_h ).to eql({ 1 => 1, 2 => :dva, 3 => 3, 4 => 0.1 })
  end

  # Rhino's scopes (implements Map) behave in a similar way
  it 'properly handles (internal) filtering map' do
    map = Java::java_integration::fixtures::InternalMap.new
    expect( map.to_java.isEmpty ).to eql true
    expect( map.to_java.size ).to eql 0
    map['_internal_1'] = 1
    map['_internal_2'] = 2
    expect( map.size() ).to eql 2
    expect( map.inspect ).to eql '#<Java::Java_integrationFixtures::InternalMap: {}>'
    yielded = {}
    map.each { |key, val| yielded[key] = val }
    expect(yielded).to be_empty

    map['proper_key'] = 3
    expect( map.size() ).to eql 3
    expect( map.inspect ).to_not eql '{}'
    expect( map.keys ).to eql ['proper_key']

    expect( map.to_java.isEmpty ).to eql false
    expect( map.to_java.size ).to eql 3
  end

  private

  if {}.respond_to? :key
    def get_hash_key(hash, value)
      hash.key(value)
    end
  else
    def get_hash_key(hash, value)
      hash.index(value)
    end
  end

  def test_equal(obj, exp)
    expect(exp).to eq(obj)
  end

  def test_ok(obj)
    expect(obj).to be_truthy
  end

  def test_exception(exc, &block)
    expect { block.call }.to raise_exception(exc)
  end

  def test_no_exception(&block)
    expect { block.call }.not_to raise_exception
  end

end
