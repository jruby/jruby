require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.SingleMethodInterface"
java_import "java_integration.fixtures.UsesSingleMethodInterface"
java_import "java_integration.fixtures.DescendantOfSingleMethodInterface"
java_import "java_integration.fixtures.UsesDescendantOfSingleMethodInterface"
java_import "java_integration.fixtures.BeanLikeInterface"
java_import "java_integration.fixtures.BeanLikeInterfaceHandler"
java_import "java_integration.fixtures.ConstantHoldingInterface"
java_import "java_integration.fixtures.CoerceToInterface"
java_import "java_integration.fixtures.ReturnsInterface"
java_import "java_integration.fixtures.ReturnsInterfaceConsumer"
java_import "java_integration.fixtures.AnotherRunnable"
java_import "java_integration.fixtures.BooleanReturningInterface"
java_import "java_integration.fixtures.BooleanReturningInterfaceConsumer"
java_import "java.lang.Runnable"

describe "Single-method Java interfaces implemented in Ruby" do

  # JRUBY-6945
  it "should allow aggregating interfaces in a module" do
    a = Module.new do
      include java.awt.event.ActionListener
    end

    expect do
      b = Module.new do
        include a
      end
    end.not_to raise_error
  end

  before :all do
    @value_holder1 = Class.new do
      include SingleMethodInterface
      def initialize(val)
        @value = val
      end
      def callIt
        @value
      end
    end
  end

  it "should be kind_of? the interface" do
    expect( @value_holder1.new(1) ).to be_kind_of SingleMethodInterface
    expect( @value_holder1.new(1) ).to be_a SingleMethodInterface
    expect( SingleMethodInterface === @value_holder1.new(1) ).to be true
  end

  it "should be implemented with 'include InterfaceClass'" do
    expect(UsesSingleMethodInterface.callIt(@value_holder1.new(1))).to eq(1)
  end

  it "should be cast-able to the interface on the Java side" do
    expect(UsesSingleMethodInterface.castAndCallIt(@value_holder1.new(2))).to eq(2)
  end

  it "should allow implementation using the underscored version" do
    klass = Class.new do
      include SingleMethodInterface

      def initialize(val)
        @value = val
      end
      def call_it
        @value
      end
    end

    expect(UsesSingleMethodInterface.callIt(klass.new(3))).to eq(3)
    expect(UsesSingleMethodInterface.castAndCallIt(klass.new(2))).to eq(2)
  end

  it "should allow reopening implementations" do
    klass = Class.new do
      include SingleMethodInterface

      def initialize(val); @value = val end
      def callIt; @value end
    end

    obj = klass.new(4)
    expect(UsesSingleMethodInterface.callIt(obj)).to eq(4)
    klass.class_eval do
      def callIt; @value + @value end
    end
    expect(UsesSingleMethodInterface.callIt(obj)).to eq(8)
  end

  it "should allow reopening implementations (underscore version)" do
    klass = Class.new do
      include SingleMethodInterface

      def call_it; 4 end
    end

    obj = klass.new
    expect(UsesSingleMethodInterface.callIt(obj)).to eq(4)
    klass.class_eval do
      def call_it; 8 end
    end
    expect(UsesSingleMethodInterface.callIt(obj)).to eq(8)
  end

  it "should use Object#equals if there is no Ruby equals defined" do
    klass = Class.new
    klass.send :include, java.util.Map
    val = klass.new

    arr = java.util.ArrayList.new; arr.add(val)
    expect(arr).to include(val)

    eq = UsesSingleMethodInterface.equals(val, val)
    expect(eq).to be true
    eq = UsesSingleMethodInterface.equals(val, nil)
    expect(eq).to be false
    eq = UsesSingleMethodInterface.equals(val, klass.new)
    expect(eq).to be false
  end

  it "should use Object#hashCode if there is no Ruby hashCode defined" do
    klass = Class.new
    klass.send :include, java.util.Map

    hash = UsesSingleMethodInterface.hashCode(val = klass.new)
    expect( hash ).to eql java.lang.System.identityHashCode(val)
  end

  it "should use Object#toString if there is no Ruby toString defined" do
    klass = Class.new
    klass.send :include, java.util.Map

    str = UsesSingleMethodInterface.toString(klass.new)
    expect( str ).to match(/\:0x[0-9a-f]+>$/)
  end

  it "should use Ruby defined equals/hashCode/toString impls" do
    klass = Class.new do
      include java.util.Map

      attr_reader :val
      def initialize(val); @val = val end

      def equals(obj); obj.respond_to?(:val) ? val == obj.val : false end
      def hashCode; val == 'a' ? 42 : val.hash end

      def toString; val == 'a' ? raise(NotImplementedError.new('a')) : val end
    end
    val_a = klass.new 'a'
    val_b = klass.new 'b'

    eq = UsesSingleMethodInterface.equals(val_a, val_a)
    expect(eq).to be true
    eq = UsesSingleMethodInterface.equals(val_a, nil)
    expect(eq).to be false
    eq = UsesSingleMethodInterface.equals(val_a, val_b)
    expect(eq).to be false
    eq = UsesSingleMethodInterface.equals(val_a, klass.new(:'a'.to_s))
    expect(eq).to be true

    hash = UsesSingleMethodInterface.hashCode(val_a)
    expect( hash ).to eql 42

    # TODO: currently this only works as expected with Proxy-based impls
    # RealClassGenerator#defineRealImplClass does not trigger a toString
    # method definition since toString is not part of java.util.Map ...
    if java.lang.reflect.Proxy.isProxyClass(val_b.java_class)
      str = UsesSingleMethodInterface.toString(val_b)
      expect( str ).to eql 'b'

      expect { UsesSingleMethodInterface.toString(val_a) }.to raise_error(NotImplementedError)
    end
  end

  it "should allow including the same interface twice" do
    c = Class.new do
      include SingleMethodInterface
      include SingleMethodInterface

      def initialize(val)
        @value = val
      end
      def callIt
        @value
      end
    end
    expect(UsesSingleMethodInterface.callIt(c.new(1))).to eq(1)
  end
end

describe "Single-method Java interfaces" do
  it "can be coerced from a block passed to a constructor" do
    expect(UsesSingleMethodInterface.new { 1 }.result).to eq(1)
    expect(UsesSingleMethodInterface.new(nil) { 1 }.result).to eq(1)
    expect(UsesSingleMethodInterface.new(nil, nil) { 1 }.result).to eq(1)
    expect(UsesSingleMethodInterface.new(nil, nil, nil) { 1 }.result).to eq(1)
    # 3 normal args is our cutoff for specific-arity optz, so test four
    expect(UsesSingleMethodInterface.new(nil, nil, nil, nil) { 1 }.result).to eq(1)
  end

  it "can be coerced from a block passed to a static method" do
    expect(UsesSingleMethodInterface.callIt { 1 }).to eq(1)
    expect(UsesSingleMethodInterface.callIt(nil) { 1 }).to eq(1)
    expect(UsesSingleMethodInterface.callIt(nil, nil) { 1 }).to eq(1)
    expect(UsesSingleMethodInterface.callIt(nil, nil, nil) { 1 }).to eq(1)
    # 3 normal args is our cutoff for specific-arity optz, so test four
    expect(UsesSingleMethodInterface.callIt(nil, nil, nil, nil) { 1 }).to eq(1)
  end

  it "can be coerced from a block passed to a instance method" do
    expect(UsesSingleMethodInterface.new.callIt2 do 1 end).to eq(1)
    expect(UsesSingleMethodInterface.new.callIt2(nil) do 1 end).to eq(1)
    expect(UsesSingleMethodInterface.new.callIt2(nil, nil) do 1 end).to eq(1)
    expect(UsesSingleMethodInterface.new.callIt2(nil, nil, nil) do 1 end).to eq(1)
    # 3 normal args is our cutoff for specific-arity optz, so test four
    expect(UsesSingleMethodInterface.new.callIt2(nil, nil, nil, nil) do 1 end).to eq(1)
  end

  it "should be implementable with .impl" do
    impl = SingleMethodInterface.impl { |name| name }
    expect(impl).to be_kind_of(SingleMethodInterface)
    expect(SingleMethodInterface).to be === impl

    expect(UsesSingleMethodInterface.callIt(impl)).to eq(:callIt)
  end

  it "should allow assignable equivalents to be passed to a method" do
    impl = DescendantOfSingleMethodInterface.impl { |name| name }
    expect(impl).to be_kind_of(SingleMethodInterface)
    expect(DescendantOfSingleMethodInterface).to be === impl
    expect(UsesSingleMethodInterface.callIt(impl)).to eq(:callIt)
    expect(UsesSingleMethodInterface.new.callIt2(impl)).to eq(:callIt)
  end

  it '.impl should always dispatch interface method (even when it conflicts from method in Ruby hierarchy)' do
    begin
      Kernel.module_eval { def callIt; raise RuntimeError.new('Kernel#callIt') end }
      expect { callIt }.to raise_error(RuntimeError)

      impl = SingleMethodInterface.impl { |name| name ? 'CALL-IT' : 'FALSY!' }

      # NOTE: prior to 9.1 impls would fail with RuntimeError 'Kernel#callIt'

      expect(UsesSingleMethodInterface.new.callIt2(impl)).to eq 'CALL-IT'

      impl = DescendantOfSingleMethodInterface.impl { |name| name.to_s.upcase }
      #expect(UsesSingleMethodInterface.new.callIt2(impl)).to eq 'CALLIT'
    ensure
      Kernel.send :remove_method, :callIt if defined? Kernel.callIt
    end
  end

  it "passes correct arguments to proc implementation" do
    Java::java.io.File.new('.').list do |dir, name| # FilenameFilter
      expect(dir).to be_kind_of(java.io.File)
      expect(name).to be_kind_of(String)
      true # boolean accept(File dir, String name)
    end

    caller = Java::java_integration.fixtures.iface.SingleMethodInterfaceWithArg::Caller

    caller.call { |arg| expect(arg).to eq(42) }
    caller.call('x') { |arg| expect(arg).to eq('x') }

    Java::java_integration.fixtures.iface.SingleMethodInterfaceWith4Args::Caller.call do
      |arg1, arg2, arg3, arg4|
      expect(arg2).to eq('hello')
      expect(arg3).to eq('world')
      expect(arg4).to eq(42)
      [ arg2, arg3, arg4 ] # return Object[]
    end
  end

  it "resolves 'ambiguous' method by proc argument count (with proc-implementation)" do
    java.io.File.new('.').listFiles do |pathname| # FileFilter#accept(File)
      expect(pathname).to be_kind_of(java.io.File)
    end
    java.io.File.new('.').listFiles do |dir, name| # FilenameFilter#accept(File, String)
      expect(dir).to be_kind_of(java.io.File)
      expect(name).to be_kind_of(String)
    end

    java.io.File.new('.').listFiles do |dir, name, invalid|
      # should choose FilenameFilter#accept(File, String)
      expect(dir).to be_kind_of(java.io.File)
      expect(name).to be_kind_of(String)
    end

    java.io.File.new('.').listFiles do |pathname, *args|
      expect(pathname).to be_kind_of(java.io.File)
      expect(args).to be_empty
    end

    java.io.File.new('.').listFiles do |dir, name, *args|
      expect(dir).to be_kind_of(java.io.File)
      expect(name).to be_kind_of(String)
      expect(args).to be_empty
    end

    java.io.File.new('.').listFiles do |*args|
      expect(args[0]).to be_kind_of(java.io.File)
      expect(args.size).to eql 1
    end

    #executor = java.util.concurrent.Executors.newSingleThreadExecutor
    #executor.execute { |*args| args.should be_empty }; sleep 0.1
    #executor.shutdown

    work_queue = java.util.concurrent.LinkedBlockingQueue.new
    executor = java.util.concurrent.ThreadPoolExecutor.new(0, 2, 0, java.util.concurrent.TimeUnit::SECONDS, work_queue) do
      |*args| # newThread(Runnable)
      expect(args[0]).to be_kind_of(java.lang.Runnable)
      expect(args.size).to eql 1
      java.lang.Thread.new(args[0])
    end
    executor.execute { |*args| expect(args).to be_empty }; sleep 0.1
    executor.shutdown
  end

  it "should maintain Ruby object equality when passed through Java and back" do
    result = SingleMethodInterface.impl { |name| name }
    callable = double "callable"
    expect(callable).to receive(:call).and_return result
    expect(UsesSingleMethodInterface.new.callIt3(callable)).to eq(result)
  end

  it "coerces to that interface after duck-typed implementation has happened" do
    callable = double "SingleMethodInterfaceImpl"
    expect(callable).to receive(:callIt).and_return :callIt
    expect(UsesSingleMethodInterface.callIt(callable)).to eq(:callIt)

    # receives Object, but should return the coerced impl
    cls = UsesSingleMethodInterface.getClass(callable)

    # pull out interfaces from the resulting class
    interfaces = []
    while cls
      interfaces.concat(cls.interfaces)
      cls = cls.superclass
    end

    expect(interfaces).to include(SingleMethodInterface.java_class)
  end

  it "preserves singleton Proc behavior and callbacks" do
    pr = proc { }
    def pr.singleton_method_added(name)
      (@names ||= []) << [name, self]
    end

    old_singleton_class = pr.singleton_class

    r = Runnable.impl(&pr)

    def pr.another_method; end

    expect(pr.singleton_class).to eq old_singleton_class

    expect(pr.instance_variable_get(:@names)).to eq [
        [:singleton_method_added, pr],
        [:another_method, pr]
    ]
  end
end

describe "A bean-like Java interface" do
  it "allows implementation with attr* methods" do
    myimpl1 = Class.new do
      include BeanLikeInterface
      attr_accessor :value, :my_value, :foo, :my_foo
    end
    myimpl2 = Class.new do
      include BeanLikeInterface
      attr_accessor :value, :myValue, :foo, :myFoo
    end
    [myimpl1, myimpl2].each do |impl|
      bli = impl.new
      blih = BeanLikeInterfaceHandler.new(bli)
      expect do
        blih.setValue(1)
        blih.setMyValue(2)
        blih.setFoo(true)
        blih.setMyFoo(true)
        expect(blih.getValue()).to eq(1)
        expect(blih.getMyValue()).to eq(2)
        expect(blih.isFoo()).to eq(true)
        expect(blih.isMyFoo()).to eq(true)
      end.not_to raise_error
    end
  end

  it "allows implementing boolean methods with ? names" do
    # Java name before Ruby name (un-beaned)
    myimpl1 = Class.new do
      include BeanLikeInterface
      def isMyFoo; true; end
      def is_my_foo; false; end
    end
    expect(BeanLikeInterfaceHandler.new(myimpl1.new).isMyFoo()).to eq(true)
    # Ruby name before beaned Java name
    myimpl2 = Class.new do
      include BeanLikeInterface
      def is_my_foo; true; end
      def myFoo; false; end
    end
    expect(BeanLikeInterfaceHandler.new(myimpl2.new).isMyFoo()).to eq(true)
    # Beaned Java name before beaned Ruby name
    myimpl3 = Class.new do
      include BeanLikeInterface
      def myFoo; true; end
      def my_foo; false; end
    end
    expect(BeanLikeInterfaceHandler.new(myimpl3.new).isMyFoo()).to eq(true)
    # Beaned Ruby name before q-marked beaned Java name
    myimpl4 = Class.new do
      include BeanLikeInterface
      def my_foo; true; end
      def myFoo?; false; end
    end
    expect(BeanLikeInterfaceHandler.new(myimpl4.new).isMyFoo()).to eq(true)
    # Q-marked beaned Java name before Q-marked beaned Ruby name
    myimpl5 = Class.new do
      include BeanLikeInterface
      def myFoo?; true; end
      def my_foo?; false; end
    end
    expect(BeanLikeInterfaceHandler.new(myimpl5.new).isMyFoo()).to eq(true)
    # Confirm q-marked beaned Ruby name works
    myimpl6 = Class.new do
      include BeanLikeInterface
      def my_foo?; true; end
    end
    expect(BeanLikeInterfaceHandler.new(myimpl6.new).isMyFoo()).to eq(true)

    # Java name before Ruby name
    myimpl1 = Class.new do
      include BeanLikeInterface
      def supahFriendly; true; end
      def supah_friendly; false; end
    end
    expect(BeanLikeInterfaceHandler.new(myimpl1.new).supahFriendly()).to eq(true)
    # Ruby name before q-marked Java name
    myimpl2 = Class.new do
      include BeanLikeInterface
      def supah_friendly; true; end
      def supahFriendly?; false; end
    end
    expect(BeanLikeInterfaceHandler.new(myimpl2.new).supahFriendly()).to eq(true)
    # Q-marked Java name before Q-marked Ruby name
    myimpl3 = Class.new do
      include BeanLikeInterface
      def supahFriendly?; true; end
      def supah_friendly?; false; end
    end
    expect(BeanLikeInterfaceHandler.new(myimpl3.new).supahFriendly()).to eq(true)
    # confirm Q-marked Ruby name works
    myimpl4 = Class.new do
      include BeanLikeInterface
      def supah_friendly?; true; end
    end
    expect(BeanLikeInterfaceHandler.new(myimpl4.new).supahFriendly()).to eq(true)
  end

  it "searches for implementation names in a predictable order" do
    myimpl1 = Class.new do
      include BeanLikeInterface
      def foo?
        true
      end
      def my_foo?
        true
      end
      def friendly?
        true
      end
      def supah_friendly?
        true
      end
    end
    myimpl2 = Class.new do
      include BeanLikeInterface
      def foo?
        true
      end
      def myFoo?
        true
      end
      def friendly?
        true
      end
      def supahFriendly?
        true
      end
    end
    [myimpl1, myimpl2].each do |impl|
      bli = impl.new
      blih = BeanLikeInterfaceHandler.new(bli)
      expect do
        expect(blih.isFoo()).to eq(true)
        expect(blih.isMyFoo()).to eq(true)
        expect(blih.friendly()).to eq(true)
        expect(blih.supahFriendly()).to eq(true)
      end.not_to raise_error
    end
  end

  it "does not honor beanified implementations of methods that don't match javabean spec" do
    myimpl1 = Class.new do
      include BeanLikeInterface
      def something_foo(x)
        x
      end
      def something_foo=(x,y)
        y
      end
    end
    myimpl2 = Class.new do
      include BeanLikeInterface
      def somethingFoo(x)
        x
      end
      def somethingFoo=(x,y)
        y
      end
    end
    [myimpl1, myimpl2].each do |impl|
      bli = impl.new
      blih = BeanLikeInterfaceHandler.new(bli)
      expect { blih.getSomethingFoo(1) }.to raise_error(NameError)
      expect { blih.setSomethingFoo(1,2) }.to raise_error(NameError)
    end
  end
end

describe "A Ruby class including a descendant interface" do
  it "implements all methods from that interface and parents" do
    impl = Class.new do
      include DescendantOfSingleMethodInterface

      def callIt; "foo"; end
      def callThat; "bar"; end
    end

    dosmi = impl.new

    expect(UsesSingleMethodInterface.callIt(dosmi)).to eq("foo")
    expect(UsesDescendantOfSingleMethodInterface.callThat(dosmi)).to eq("bar")
  end

  it "inherits implementation of super-interface methods from superclass" do
    super_impl = Class.new do
      include SingleMethodInterface

      def callIt; "foo"; end
    end

    impl = Class.new(super_impl) do
      include DescendantOfSingleMethodInterface
      def callThat; "bar"; end
    end

    dosmi = impl.new

    expect(UsesSingleMethodInterface.callIt(dosmi)).to eq("foo")
    expect(UsesDescendantOfSingleMethodInterface.callThat(dosmi)).to eq("bar")
  end
end

describe "Single object implementing methods of interface" do
  before(:each) do
    @impl = Class.new do
      include SingleMethodInterface
    end
  end

  it "should not be possible to call methods on instances of class from Java" do
    expect do
      SingleMethodInterface::Caller.call(@impl.new)
    end.to raise_error(NoMethodError)
  end

  it "should be possible to call methods on specific object that implements a method" do
    obj = @impl.new
    def obj.callIt
      "foo"
    end
    expect(SingleMethodInterface::Caller.call(obj)).to eq("foo")
  end
end

describe "Calling include to include a Java interface into a Ruby class" do
  it "should incorporate constants from the interface into the class's metaclass" do
    klass = Class.new do
      include ConstantHoldingInterface
    end

    expect(klass::MY_INT).to eq(1)
    expect(klass::MY_STRING).to eq("foo")
  end
end

describe "A ruby module used as a carrier for Java interfaces" do
  it "allows multiple interfaces" do
    mod = Module.new do
      include SingleMethodInterface
      include BeanLikeInterface

      def self.java_interfaces; @java_interface_mods; end
    end
    expect(mod.java_interfaces).to include(SingleMethodInterface)
    expect(mod.java_interfaces).to include(BeanLikeInterface)
  end

  it "calls append_features on each interface" do
    my_smi = SingleMethodInterface.dup
    my_bli = BeanLikeInterface.dup
    [my_smi, my_bli].each do |my|
      class << my
        alias :old_af :append_features
        def append_features(cls)
          if @append_features_called
            @append_features_called += 1
          else
            @append_features_called = 1
          end
          old_af(cls)
        end
        def called; @append_features_called; end
      end
    end

    mod = Module.new do
      include my_smi
      include my_bli
    end

    expect(my_smi.called).to eq(1)
    expect(my_bli.called).to eq(1)

    Class.new { include mod }

    expect(my_smi.called).to eq(2)
    expect(my_bli.called).to eq(2)
  end

  it "causes an including class to implement all interfaces" do
    mod = Module.new do
      include SingleMethodInterface
      include BeanLikeInterface
    end

    klass = Class.new do
      include mod
      def callIt; "bar"; end
      def getValue; 100; end
    end

    obj = klass.new
    blih = BeanLikeInterfaceHandler.new(obj)

    expect(SingleMethodInterface::Caller.call(obj)).to eq("bar")
    expect(blih.value).to eq(100)
  end
end

describe "Coercion of normal ruby objects" do
  it "should allow an object passed to a java method to be coerced to the interface" do
    ri = double "returns interface"
    consumer = ReturnsInterfaceConsumer.new
    consumer.set_returns_interface ri
    expect(ri).to be_kind_of(ReturnsInterface)
  end


  it "should return the original ruby object when returned back to Ruby" do
    obj = double "ruby object"
    cti = CoerceToInterface.new
    result = cti.returnArgumentBackToRuby(JRuby.runtime, obj)
    expect(obj).to be_kind_of(Java::JavaLang::Runnable)
    expect(result).to eq(obj)
    expect(obj).to eq(result)
  end

  it "should return the original ruby object when converted back to Ruby" do
    obj = double "ruby object"
    cti = CoerceToInterface.new
    result = cti.coerceArgumentBackToRuby(JRuby.runtime, obj)
    expect(obj).to be_kind_of(Java::JavaLang::Runnable)
    expect(result).to eq(obj)
  end

  it "should pass the original ruby object when converted back to Ruby and used as an argument to another Ruby object" do
    obj = double "ruby object"
    callable = double "callable"
    expect(callable).to receive(:call).with(obj)
    cti = CoerceToInterface.new
    cti.passArgumentToInvokableRubyObject(callable, obj)
    expect(obj).to be_kind_of(Java::JavaLang::Runnable)
  end

  it "should allow an object passed to a java constructor to be coerced to the interface" do
    ri = double "returns interface"
    ReturnsInterfaceConsumer.new(ri)
    expect(ri).to be_kind_of(ReturnsInterface)
  end

  it "should allow an object to be coerced as a return type of a java method" do
    ri = double "returns interface"
    value = double "return value runnable"
    allow(ri).to receive(:getRunnable).and_return value

    consumer = ReturnsInterfaceConsumer.new(ri)
    runnable = consumer.getRunnable
    expect(runnable).to eq(value)
    expect(value).to be_kind_of(Java::JavaLang::Runnable)
  end
end

describe "A child extending a Ruby class that includes Java interfaces" do
  it "should implement all those interfaces" do
    sup = Class.new { include BeanLikeInterface }
    child = Class.new(sup) { def getValue; 1; end }

    obj = child.new
    blih = BeanLikeInterfaceHandler.new(obj)

    expect(blih.value).to eq(1)
  end
end

describe "Calling methods through interface on Ruby objects with methods defined on singleton class" do
  before(:each) do
    @klass = Class.new do
      include SingleMethodInterface
    end

    @obj1 = @klass.new
    @obj2 = @klass.new
  end

  it "should handle one object using instance_eval" do
    @obj1.instance_eval("def callIt; return :ok; end;")

    expect(UsesSingleMethodInterface.callIt(@obj1)).to eq(:ok)
  end

  it "should handle one object extending module" do
    @module = Module.new { def callIt; return :ok; end; }
    @obj1.extend(@module)

    expect(UsesSingleMethodInterface.callIt(@obj1)).to eq(:ok)
  end

  it "should handle two object with combo of instance_eval and module extension" do
    @obj1.instance_eval("def callIt; return :one; end;")
    @module = Module.new { def callIt; return :two; end; }
    @obj2.extend(@module)

    expect(UsesSingleMethodInterface.callIt(@obj1)).to eq(:one)
    expect(UsesSingleMethodInterface.callIt(@obj2)).to eq(:two)
  end

  it "should handle two object with combo of instance_eval and module extension in opposite order" do
    @obj1.instance_eval("def callIt; return :one; end;")
    @module = Module.new { def callIt; return :two; end; }
    @obj2.extend(@module)

    expect(UsesSingleMethodInterface.callIt(@obj2)).to eq(:two)
    expect(UsesSingleMethodInterface.callIt(@obj1)).to eq(:one)
  end
end

# JRUBY-2999
describe "A Ruby class implementing Java interfaces with overlapping methods" do
  it "should implement without error" do
    cls = Class.new do
      include AnotherRunnable
      include Runnable
      attr_accessor :foo
      def run
        @foo = 'success'
      end
    end

    obj = nil
    expect {obj = cls.new}.not_to raise_error
  end
end

# JRUBY-3166
describe "A Ruby class implementing a Java interface with method returning Boolean" do
  it "should correctly box returned value" do
    cls = Class.new do
      include BooleanReturningInterface
      def bar
        true
      end
    end

    obj = cls.new
    expect(BooleanReturningInterfaceConsumer.new.consume(obj)).to be_truthy
  end
end

describe "A Ruby class implementing an interface" do
  describe "that extends a Ruby class implementing an interface" do
    it "can initialize successfully" do
      c1 = Class.new do
        include java.lang.Runnable
      end
      expect {c1.new}.not_to raise_error
      c2 = Class.new(c1) do
        include java.io.Serializable
      end
      expect {c2.new}.not_to raise_error
    end
  end

  it "returns the Java class implementing the interface for .java_class" do
    cls = Class.new do
      include java.lang.Runnable
    end
    obj = cls.new

    java_cls = obj.java_class

    expect(java_cls.interfaces).to include(java.lang.Runnable.java_class)
  end
end

describe "A class that extends a DelegateClass" do

  before(:all) { require 'delegate' }

  it "can include a Java interface without error" do
    c1 = Class.new
    lambda do
      c2 = Class.new(DelegateClass(c1)) do
        include java.io.Serializable
      end
    end
  end
end
