require 'test/unit'

class TestMethods < Test::Unit::TestCase
  class A
    undef_method :id
    (class<<self;self;end).send(:undef_method,:id)
  end
  Adup = A.dup

  def test_undef_id
    assert_raise(NoMethodError) { A.id }
    assert_raise(NoMethodError) { A.new.id }
    assert_raise(NoMethodError) { Adup.id }
    assert_raise(NoMethodError) { Adup.new.id }
  end
  
  class Foo
    private
    def foo; end
  end
  
  def test_foo
    assert_raise(NoMethodError) {Foo.class_eval "new.foo"}
    begin
      Foo.class_eval "new.foo"
    rescue NoMethodError
      $!.to_s =~ /private/
    end
  end

  # JRUBY-2277
  def test_alias_method_calls_correct_method_added_with_sym
    $methods_added = []
    $singleton_methods_added = []

    c  = Class.new do
      class << self
        def singleton_method_added(x) # will also add its own def!
          $singleton_methods_added << x
        end
        def method_added(x)
          $methods_added << x
        end
        def another_singleton
        end
        alias_method :yet_another_singleton, :another_singleton
      end
      def foo
      end
      def bar
      end
      alias_method :baz, :bar  
    end

    expected_methods = [:foo, :bar, :baz]
    expected_singletons = [:singleton_method_added, :method_added, :another_singleton, :yet_another_singleton]

    assert_equal(expected_methods, $methods_added)
    assert_equal(expected_singletons, $singleton_methods_added)

    # test coercion of alias names to symbols
    name = Object.new
    def name.to_str
      "boo"
    end
    assert_nothing_raised { c.send("alias_method", name, "foo") }
    assert_equal(:boo, $methods_added.last)

  end
end

class TestMethodObjects < Test::Unit::TestCase
  # all testing return values are in the format
  # [receiver, origin, method_name]
  # origin = Class/Module's name (as Symbol) for instance methods,
  #          Object for singleton methods
  class C
    def foo; [self, :C, :foo]; end
    def bar; [self, :C, :bar]; end

    # for simplicity on singleton representations,
    # let's assure #inspect = #to_s
    alias inspect to_s
  end

  class D < C
    def bar; [self, :D, :bar]; end
    def xyz; [self, :D, :xyz]; end
  end

  class E < D
    def qux; [self, :E, :qux]; end
  end

  class F < C
    def foo; [self, :F, :foo]; end
  end

  def test_method_call_equivalence
    c1 = C.new
    c2 = C.new
    c3 = C.new
    def c3.bar; [self, self, :bar]; end
    def c3.cor; [self, self, :cor]; end
    d1 = D.new
    d2 = D.new
    def d2.bar; [self, self, :bar]; end
    e1 = E.new
    e2 = E.new
    def e2.xyz; [self, self, :xyz]; end

    [c1, c2, c3].each do |c|
      assert_equal(c.foo, c.method(:foo).call)
      assert_equal(c.bar, c.method(:bar).call)
    end
    assert_equal(c3.cor, c3.method(:cor).call)

    [d1, d2].each do |d|
      assert_equal(d.foo, d.method(:foo).call)
      assert_equal(d.bar, d.method(:bar).call)
      assert_equal(d.xyz, d.method(:xyz).call)
    end

    [e1, e2].each do |e|
      assert_equal(e.foo, e.method(:foo).call)
      assert_equal(e.bar, e.method(:bar).call)
      assert_equal(e.xyz, e.method(:xyz).call)
      assert_equal(e.qux, e.method(:qux).call)
    end
  end

  def test_method_equivalence
    c1 = C.new
    c2 = C.new
    d1 = D.new
    e1 = E.new

    c1_foo1 = c1.method(:foo)
    c1_foo2 = c1.method(:foo)
    assert_equal(c1_foo1, c1_foo2)
    assert_equal(C.instance_method(:foo).bind(d1), d1.method(:foo))
    assert_equal(C.instance_method(:foo).bind(e1), e1.method(:foo))
    assert_equal(D.instance_method(:foo).bind(e1), e1.method(:foo))
    assert_equal(D.instance_method(:bar).bind(e1), e1.method(:bar))

    assert_not_equal(c1_foo1, c2.method(:foo))
  end

  def test_direct_method_to_s
    c1 = C.new
    c2 = C.new
    d1 = D.new

    assert_equal("#<Method: #{C}#foo>", c1.method(:foo).to_s)
    assert_equal("#<Method: #{C}#bar>", c1.method(:bar).to_s)
    assert_equal("#<Method: #{D}#bar>", d1.method(:bar).to_s)
    assert_equal("#<Method: #{D}#xyz>", d1.method(:xyz).to_s)

    # non-singleton methods of singleton-ized methods should still
    # be treated as direct methods
    def c2.foo; [self, self, :foo]; end
    assert_equal("#<Method: #{c2}.foo>", c2.method(:foo).to_s)
    assert_equal("#<Method: #{C}#bar>", c2.method(:bar).to_s)
  end

  def test_indirect_method_to_s
    d = D.new
    e = E.new

    assert_equal("#<Method: #{D}(#{C})#foo>", d.method(:foo).to_s)
    assert_equal("#<Method: #{E}(#{C})#foo>", e.method(:foo).to_s)
    assert_equal("#<Method: #{E}(#{D})#bar>", e.method(:bar).to_s)
  end

  def test_method_rebind
    c1 = C.new
    c2 = C.new
    c3 = C.new
    def c3.bar; [self, self, :bar]; end
    d1 = D.new
    c1_foo = c1.method(:foo)
    c2_foo = c2.method(:foo)
    c3_bar = c3.method(:bar)
    d1_foo = d1.method(:foo)
    d1_bar = d1.method(:bar)

    assert_equal(c1_foo, c1_foo.unbind.bind(c1))
    assert_equal(c2_foo, c1_foo.unbind.bind(c2))
    assert_equal(c1_foo.unbind, c2_foo.unbind.bind(c2).unbind)
    assert_equal(d1_foo, c1_foo.unbind.bind(d1))

    assert_equal(c2_foo, c1_foo.unbind.bind(c2))
    assert_equal(c2.foo, c1_foo.unbind.bind(c2).call)

    assert_raise(TypeError) { c3_bar.unbind.bind(c1) }
    assert_raise(TypeError) { d1_bar.unbind.bind(c1) }
  end

  def test_method_redefinition
    f = F.new
    f_bar1 = f.method(:bar)
    F.class_eval do
      def bar; [self, :F, :bar]; end
    end
    f_bar2 = f.method(:bar)
    assert_not_equal(f_bar1, f_bar2)
    assert_equal([f, :C, :bar], f_bar1.call)
    assert_equal([f, :F, :bar], f_bar2.call)
    assert_equal(f_bar1, C.instance_method(:bar).bind(f))
  end

  def test_unbound_method_equivalence
    c1 = C.new
    c2 = C.new
    def c2.bar; [self, self, :bar]; end
    d1 = D.new
    e1 = E.new
    e2 = E.new
    def e2.foo; [self, self, :foo]; end

    unbind = lambda { |o, m| o.method(m).unbind }
    c1_foo = unbind[c1, :foo]
    c1_foo2 = unbind[c1, :foo]
    e1_foo = unbind[e1, :foo]

    assert_equal(c1_foo, c1_foo2)
    assert_equal(c1_foo, unbind[c2, :foo])
    assert_equal(unbind[e1, :bar], unbind[e2, :bar])
    assert_equal(unbind[e1, :xyz], unbind[e2, :xyz])
    assert_equal(unbind[e1, :qux], unbind[e2, :qux])

    assert_not_equal(unbind[c1, :bar], unbind[c2, :bar])
    assert_not_equal(c1_foo, unbind[d1, :foo])
    assert_not_equal(e1_foo, c1_foo)
    assert_not_equal(e1_foo, unbind[d1, :foo])
    assert_not_equal(e1_foo, unbind[e2, :foo])
    assert_not_equal(c1_foo, unbind[e2, :foo])

    c__foo = C.instance_method(:foo)
    c__bar = C.instance_method(:bar)
    d__foo = D.instance_method(:foo)
    e__foo = E.instance_method(:foo)
    e__xyz = E.instance_method(:xyz)

    assert_not_equal(c__foo, d__foo)
    assert_not_equal(c__foo, e__foo)

    assert_equal(c1_foo, c__foo)
    assert_equal(e1_foo, e__foo)
    assert_not_equal(unbind[e2, :foo], e__foo)
  end

  def test_unbound_method_to_s
    c1 = C.new
    c2 = C.new
    d1 = D.new

    unbind = lambda { |o, m| o.method(m).unbind }

    c1_foo_u = unbind[c1, :foo]
    c2_foo_u = unbind[c2, :foo]
    d1_foo_u = unbind[d1, :foo]

    assert_equal(c1_foo_u.to_s, c2_foo_u.to_s)
    assert_equal("#<UnboundMethod: #{C}#foo>", c1_foo_u.to_s)
    assert_equal("#<UnboundMethod: #{D}(#{C})#foo>", d1_foo_u.to_s)

    e1 = E.new
    sing_e1 =
      class << e1
        def xyz; [self, self, :xyz]; end
        self
      end
    e1_foo_u = e1.method(:foo).unbind
    e1_bar_u = e1.method(:bar).unbind
    e1_xyz_u = e1.method(:xyz).unbind

    assert_equal("#<UnboundMethod: #{E}(#{C})#foo>", e1_foo_u.to_s)
    assert_equal("#<UnboundMethod: #{E}(#{D})#bar>", e1_bar_u.to_s)
    assert_equal("#<UnboundMethod: #{sing_e1}#xyz>", e1_xyz_u.to_s)
  end
end

class TestCaching < Test::Unit::TestCase
  module Foo
    def the_method
      $THE_METHOD = 'Foo'
    end
  end

  def setup
    $a = Class.new do 
      def the_method
        $THE_METHOD = 'A'
      end
    end.new
  end
  
  def test_extend
    40.times do 
      $a.the_method
      assert_equal "A", $THE_METHOD
    end

    $a.extend Foo

    40.times do 
      $a.the_method
      assert_equal "Foo", $THE_METHOD
    end
  end

  def test_alias
    40.times do 
      $a.the_method
      assert_equal "A", $THE_METHOD
    end

    $a.class.class_eval do 
      def the_bar_method
        $THE_METHOD = "Bar"
      end

      alias_method :the_method, :the_bar_method
    end
    
    $a.the_method
    assert_equal "Bar", $THE_METHOD
  end

  def test_direct
    a = Class.new do
      def foo ; false ; end
    end
    obj = a.new
    assert !obj.foo
    a.class_eval do
      def foo ; true ; end
    end
    assert obj.foo, "redefined method used"
    assert_raise(NoMethodError) { obj.bar }
    a.class_eval do
      def bar ; true ; end
    end
    assert obj.bar, "newly defined method used"
  end
  
  def test_parent
    a = Class.new do
      def foo ; false ; end
    end
    b = Class.new(a)
    obj = b.new
    assert !obj.foo
    a.class_eval do
      def foo ; true ; end
    end
    assert obj.foo, "redefined method used"
    assert_raise(NoMethodError) { obj.bar }
    a.class_eval do
      def bar ; true ; end
    end
    assert obj.bar, "newly defined method used"
  end
  
  def test_parent_shadowed
    a = Class.new do
      def foo ; :a ; end
    end
    b = Class.new(a) do
      def foo ; :b ; end
    end
    obj = b.new
    assert_equal :b, obj.foo
    a.class_eval do
      def foo ; false ; end
    end
    assert_equal :b, obj.foo, "shadowed method not used"
  end
  
  def test_intermediate_ancestor
    a = Class.new do
      def foo ; :a ; end
    end
    b = Class.new(a)
    c = Class.new(b)
    obj = c.new
    assert_equal :a, obj.foo
    b.class_eval do
      def foo ; :b ; end
    end
    assert_equal :b, obj.foo, "new method used"
    b.class_eval do
      def foo ; :b2 ; end
    end
    assert_equal :b2, obj.foo, "redefined method used"
    assert_raise(NoMethodError) { obj.bar }
    b.class_eval do
      def bar ; true ; end
    end
    assert obj.bar, "newly defined method used"
  end

  def test_intermediate_module
    a = Class.new do
      def foo ; :a ; end
    end
    m = Module.new do
      def foo ; :m ; end
    end
    b = Class.new(a)
    c = Class.new(b)
    obj = c.new
    assert_equal :a, obj.foo
    b.class_eval do
      include m
    end
    assert_equal :m, obj.foo, "method from included module used"
    m.module_eval do
      def foo ; :m2 ; end
    end
    assert_equal :m2, obj.foo, "redefined method used"
    assert_raise(NoMethodError) { obj.bar }
    m.module_eval do
      def bar ; true ; end
    end
    assert obj.bar, "newly defined method used"
  end

  def test_class_singleton_intermediate
    a = Class.new
    def a.foo ; :a ; end
    b = Class.new(a)
    c = Class.new(b)
    assert_equal :a, c.foo
    def b.foo ; :b ; end
    assert_equal :b, c.foo, "new shadowing method used"
    def b.foo ; :b2 ; end
    assert_equal :b2, c.foo, "redefined method used"
    assert_raise(NoMethodError) { c.bar }
    def b.bar ; true ; end
    assert c.bar, "newly defined method used"
  end

  def test_class_singleton_intermediate_extension
    a = Class.new
    def a.foo ; :a ; end
    b = Class.new(a)
    c = Class.new(b)
    m = Module.new do
      def foo ; :m ; end
    end
    assert_equal :a, c.foo
    b.extend m
    assert_equal :m, c.foo, "method from extended module used"
    m.module_eval do
      def foo ; :m2 ; end
    end
    assert_equal :m2, c.foo, "redefined method used"
    assert_raise(NoMethodError) { c.bar }
    m.module_eval do
      def bar ; true ; end
    end
    assert c.bar, "newly defined method used"
  end

  # JRUBY-2646
  def test_methods_with_various_arity_and_blocks
    a = Class.new do
      def m()
        yield
      end
      def m1(a)
        yield
      end
      def m2(a,b)
        yield
      end
      def m3(a,b,c)
        yield
      end
      def m4(a,b,c,d)
        yield
      end
    end
    
    obj = a.new

    assert_equal(5, obj.m { 5 })
    assert_equal(5, obj.m1(1) { 5 })
    assert_equal(5, obj.m2(1,2) { 5 })
    assert_equal(5, obj.m3(1,2,3) { 5 })
    assert_equal(5, obj.m4(1,2,3,4) { 5 })
  end
end
