require 'test/unit'

class TestMethod < Test::Unit::TestCase
  # JRUBY-3491: NullPointerException when calling #arity on Klass.method(:new)
  def test_jruby_3491
    assert_nothing_raised do
      String.method(:new).arity
    end
  end

  def test_function_break
    obj = Object.new
    def obj.broken_method
      break # TODO this is a SyntaxError on MRI 2.2.2
    end
    assert_raise(LocalJumpError){ obj.broken_method }
  end

  module Methods
    def self.req2(a1, a2); a1 || a2 end
    def self.opt1(a1, a2 = {}); a1 if a2 end
    def self.key(foo: 1, bar: 2) foo || bar end
    def self.keyrest(foo: 1, **bar) bar end
    def self.key_block(foo: 1, &bar) bar end
    def self.anonkeyrest(**) end
    def self.resta(*a) a end
    def self.mix(a1, *a2, a3, foo: 1, **bar) end
  end

  def test_parameters
    assert_equal [[:req, :a1], [:req, :a2]], Methods.method(:req2).parameters
    assert_equal [[:req, :a1], [:opt, :a2]], Methods.method(:opt1).parameters

    assert_equal [[:key, :foo], [:key, :bar]], Methods.method(:key).parameters
    assert_equal [[:key, :foo], [:keyrest, :bar]], Methods.method(:keyrest).parameters
    assert_equal [[:key, :foo], [:block, :bar]], Methods.method(:key_block).parameters
    assert_equal [[:keyrest]], Methods.method(:anonkeyrest).parameters

    assert_equal [[:rest, :a]], Methods.method(:resta).parameters
    assert_equal [[ :rest ]], String.method(:new).parameters

    assert_equal [[:req, :a1], [:rest, :a2], [:req, :a3], [:key, :foo], [:keyrest, :bar]], Methods.method(:mix).parameters

    assert_equal [[:req, :a1]], lambda { |a1| }.parameters
    assert_equal [[:req, :a1], [:opt, :a2]], lambda { |a1, a2 = {}| }.parameters

    assert_equal [[:key, :foo], [:key, :bar]], lambda { |foo: 1, bar: 2| }.parameters
    assert_equal [[:key, :foo], [:keyrest, :bar]], lambda { |foo: 1, **bar| }.parameters
    assert_equal [[:keyrest]], lambda { |**| }.parameters

    assert_equal [[:rest]], lambda { |*| }.parameters

    assert_equal [[:req, :a1], [:rest, :a2], [:req, :a3], [:key, :foo], [:keyrest, :bar]], lambda { |a1, *a2, a3, foo: 1, **bar| }.parameters
  end

  def test_callee # (passing) part from *mri/ruby/test_method.rb*
    assert_equal(:test_callee, __method__)
    assert_equal(:m, Class.new {def m; __method__; end}.new.m)
    assert_equal(:m, Class.new {def m; tap{return __method__}; end}.new.m)
    assert_equal(:m, Class.new {define_method(:m) {__method__}}.new.m)
    assert_equal(:m, Class.new {define_method(:m) {tap{return __method__}}}.new.m)
    assert_nil(eval("class TestCallee; __method__; end"))

    assert_equal(:test_callee, __callee__)
    [
        ["method",              Class.new {def m; __callee__; end},],
        ["block",               Class.new {def m; tap{return __callee__}; end},],
        ["define_method",       Class.new {define_method(:m) {__callee__}}],
        ["define_method block", Class.new {define_method(:m) {tap{return __callee__}}}],
    ].each do |mesg, c|
      o = c.new
      assert_equal(:m, o.m, mesg)
    end
    assert_nil(eval("class TestCallee; __callee__; end"))
  end

  def test_eq # (modified) part from *mri/ruby/test_method.rb*
    o = Object.new
    class << o
      def foo; end
      alias muu foo
      def baz; end
      alias bar baz
    end
    assert_not_equal(o.method(:foo), nil)
    m = o.method(:foo)
    def o.foo; end
    assert_not_equal(o.method(:foo), m)
    assert_equal(o.method(:foo), o.method(:foo))
    assert_equal(o.method(:baz), o.method(:bar))
    assert_not_equal(o.method(:foo), o.method(:baz))
    assert_not_equal(o.method(:foo), o.method(:muu))

    assert_equal(String.instance_method(:to_s), String.instance_method(:to_str))

    assert_not_equal([0].method(:map), [].method(:map))
  end

  def test_hash
    o = Object.new
    def o.foo; end
    class << o
      alias bar foo
    end

    hash = o.method(:foo).hash
    assert_kind_of(Integer, hash)
    assert_equal(hash, o.method(:bar).hash)

    hash = String.instance_method(:to_s).hash
    assert_kind_of(Integer, hash)
    assert_equal(hash, String.instance_method(:to_str).hash)

    assert_not_equal([0].method(:map).hash, [].method(:map).hash)
  end

  def test_inspect
    c = Class.new do
      def foo; end; alias bar foo
    end
    m = c.new.method(:foo)
    assert_equal("#<Method: #{c.inspect}#foo>", m.inspect)
    m = c.instance_method(:foo)
    assert_equal("#<UnboundMethod: #{c.inspect}#foo>", m.inspect)

    m = c.new.method(:bar)
    assert_equal("#<Method: #{c.inspect}#bar(foo)>", m.inspect)
    m = c.instance_method(:bar)
    assert_equal("#<UnboundMethod: #{c.inspect}#bar(foo)>", m.inspect)
  end

end