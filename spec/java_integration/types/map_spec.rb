require File.dirname(__FILE__) + "/../spec_helper"

describe "A Java primitive Array of type" do
  if {}.respond_to? :key
    def key(hash, value)
      hash.key(value)
    end
  else
    def key(hash, value)
      hash.index(value)
    end
  end
    
  def test_equal(a, b)
    b.should == a
  end
  def test_ok(a)
    a.should be_true
  end
  def test_exception(a, &b)
    lambda {b.call}.should raise_exception(a)
  end
  def test_no_exception(&b)
    lambda {b.call}.should_not raise_exception
  end
  it "supports Hash-like operations on java.util.Map classes" do
    h = java.util.HashMap.new
    test_ok(h.kind_of? java.util.Map)
    h.put(1, 2); h.put(3, 4); h.put(5, 6)
    test_equal({1=>2, 3=>4, 5=>6}, eval(h.inspect))
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
    if ENV_JAVA['java.specification.version'] < '1.8'
      h.replace({1=>100})
    else
      h.clear
      h[1]=100
    end
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
    test_ok(h.member?(3))
    test_equal(Java::JavaUtil::LinkedHashMap, h.class)

    h1 = java.util.LinkedHashMap.new
    h1.put("a", 100); h1.put("b", 200)
    h2 = java.util.LinkedHashMap.new
    h2.put("b", 254); h2.put("c", 300)
    # Java 8 adds a merge method to Map used for merging multiple values for a given key in-place
    if ENV_JAVA['java.specification.version'] < '1.8'
      test_equal({"a"=>100, "b"=>254, "c"=>300}, h1.merge(h2))
      test_equal({"a"=>100, "b"=>454, "c"=>300}, h1.merge(h2) { |k, o, n| o+n })
      test_equal("{\"a\"=>100, \"b\"=>200}", h1.inspect)
    end
    h1.merge!(h2) { |k, o, n| o }
    test_equal("{\"a\"=>100, \"b\"=>200, \"c\"=>300}", h1.inspect)
    test_equal(Java::JavaUtil::LinkedHashMap, h1.class)

    h.clear
    h.put(1, 100); h.put(2, 200); h.put(3, 300)
    test_equal({1=>100, 2=>200}, h.reject { |k, v| k > 2 })
    test_equal("{1=>100, 2=>200, 3=>300}", h.inspect)
    test_equal({1=>100, 2=>200}, h.reject! { |k, v| k > 2 })
    test_equal("{1=>100, 2=>200}", h.inspect)

    # Java 8 adds a replace method to Map that takes a key and value
    if ENV_JAVA['java.specification.version'] < '1.8'
      test_equal({"c"=>300, "d"=>400, "e"=>500}, h.replace({"c"=>300, "d"=>400, "e"=>500}))
    else
      h.clear
      h.put_all({"c"=>300, "d"=>400, "e"=>500})
    end
    test_equal(Java::JavaUtil::LinkedHashMap, h.class)

    if RUBY_VERSION =~ /1\.9/
      test_equal({"d"=>400, "e"=>500}, h.select {|k,v| k > "c"})
      test_equal({"c"=>300}, h.select {|k,v| v < 400})
    end

    # Java 8 adds a replace method to Map that takes a key and value
    if ENV_JAVA['java.specification.version'] < '1.8'
      h.replace({"a"=>20, "d"=>10, "c"=>30, "b"=>0})
    else
      h.clear
      h.put_all({"a"=>20, "d"=>10, "c"=>30, "b"=>0})
    end
    test_equal([["a", 20], ["b", 0], ["c", 30], ["d", 10]], h.sort)
    test_equal([["b", 0], ["d", 10], ["a", 20], ["c", 30]], h.sort { |a, b| a[1]<=>b[1] })

    test_equal(20, h.store("e", 20))
    test_equal([["a", 20], ["d", 10], ["c", 30], ["b", 0], ["e", 20]], h.to_a)
    rh = h.to_hash
    test_equal(Java::JavaUtil::LinkedHashMap, h.class)
    test_equal(Hash, rh.class)

    # 1.9
    if RUBY_VERSION =~ /1\.9/
      test_equal("{\"a\"=>20, \"d\"=>10, \"c\"=>30, \"b\"=>0, \"e\"=>20}", h.to_s)
    else
      test_equal("a20d10c30b0e20", h.to_s)
    end

    test_ok(h.all? { |k, v| k.length == 1 })
    test_ok(!h.all? { |k, v| v > 100 })
    test_equal([["a", 20], ["d", 10], ["c", 30], ["b", 0], ["e", 20]], h.map{|a|a})
    test_equal([true, false, false, false, false], h.collect { |k, v| k == "a" })
    test_equal([["a", 20], ["d", 10]], h.take(2))

    # Java 8 adds a replace method to Map that takes a key and value
    if ENV_JAVA['java.specification.version'] < '1.8'
      h.replace({"a"=>100, "b"=>200})
    else
      h.clear
      h.put_all({"a"=>100, "b"=>200})
    end
    h2 = {"b"=>254, "c"=>300}
    test_equal({"a"=>100, "b"=>200, "c"=>300}, h.update(h2) { |k, o, n| o })
    test_equal("{\"a\"=>100, \"b\"=>200, \"c\"=>300}", h.inspect)
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
    test_equal(1, key(h, 2))
    test_equal(nil, key(h, 10))
    test_equal(nil, h.default_proc)
    h.default = :hello
    test_equal(nil, h.default_proc)
    test_equal(1, key(h, 2))
    test_equal(nil, key(h, 10))

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
    if RUBY_VERSION =~ /1\.9/
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
    end

    class H1 < java.util.HashMap
    end

    test_no_exception { H1.new.clone }
  end
end
