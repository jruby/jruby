require File.dirname(__FILE__) + "/../spec_helper"

describe "an interface" do

  before :all do
    require 'tmpdir'; @tmpdir = Dir.mktmpdir
    files = []

    src = <<-JAVA
    public interface Java8Interface {
      static String message() { return "hello"; }
      static String message(CharSequence name) { return "hello " + name; }

      abstract String bar() ;
      default CharSequence foo(Object prefix) { return prefix + "foo" + ' ' + bar(); }

      public static CharSequence foo(Java8Interface impl) { return impl.foo(""); }
      public static CharSequence bar(Java8Interface impl) { return impl.bar(); }

      public static CharSequence subFoo(SubInterface impl) { return impl.foo(""); }
      public static CharSequence subBar(SubInterface impl) { return impl.bar(); }
      public static CharSequence subBaz(SubInterface impl) { return impl.baz(); }

      interface SubInterface extends Java8Interface { abstract String baz() ; }
    }
    JAVA
    files << (file = "#{@tmpdir}/Java8Interface.java"); File.open(file, 'w') { |f| f.print(src) }

    src = <<-JAVA
    import java.util.*;
    import java.util.stream.*;
    import java.util.function.*;

    public class Java8Implementor implements Java8Interface {
      public String bar() { return getClass().getSimpleName(); }

      // re-using the same class for proc-impl testing

      public static Object withConsumerCall(Integer i, java.util.function.IntConsumer c) {
          c.accept(i * 10); return i + 1;
      }

      public static boolean withPredicateCall(Object obj, java.util.function.Predicate<Object> p) {
          // TODO following line fails (obviously due test method in Ruby) :
          // ArgumentError: wrong number of arguments (1 for 2)
          //   org/jruby/gen/InterfaceImpl817905333.gen:13:in `test'
          return p.test(obj);
      }

      public static String ambiguousCall1(String str, java.util.Map<?, ?> map) {
          return "ambiguousWithMap";
      }
      public static String ambiguousCall1(String str, java.util.function.Consumer<StringBuilder> c) {
          StringBuilder builder = new StringBuilder(str);
          c.accept(builder.append("ambiguousWithConsumer")); return builder.toString();
      }

      public static String ambiguousCall2(CharSequence str, java.util.function.Consumer<StringBuilder> c) {
          StringBuilder builder = new StringBuilder(str);
          c.accept(builder.append("ambiguousWithConsumer")); return builder.toString();
      }
      public static String ambiguousCall2(String str, java.util.Map<?, ?> map) {
          return "ambiguousWithMap";
      }

      public static String ambiguousCall3(CharSequence str, java.util.function.Consumer<StringBuilder> c) {
          StringBuilder builder = new StringBuilder(str);
          c.accept(builder.append("ambiguousWithConsumer")); return builder.toString();
      }
      public static String ambiguousCall3(String str, java.util.Map<?, ?> map) {
          return "ambiguousWithMap";
      }
      public static String ambiguousCall3(String str, java.util.function.DoubleBinaryOperator op) {
          return "ambiguousWithBinaryOperator";
      }

      public static <T> Function<Integer, Integer> composepp(Function<Integer, Integer> fx) {
        return fx.compose(i -> i + 1);
      }

      public static <T> Function<Integer, Integer> compose10pp() {
        return composepp(i -> i + 10);
      }

    }
    JAVA
    files << (file = "#{@tmpdir}/Java8Implementor.java"); File.open(file, 'w') { |f| f.print(src) }

    fail "#{files.inspect} compilation failed (see above javac output)!" unless javac_compile files

    $CLASSPATH << @tmpdir
  end

  after :all do
    FileUtils.rm_rf @tmpdir
  end

  it "binds static methods on the proxy module" do
    expect(Java::Java8Interface.message).to eq("hello")
  end

  it "exposes static methods via java_send" do
    expect(Java::Java8Interface.java_send(:message)).to eq("hello")
  end

  it "exposes static methods via java_send with type" do
    expect(Java::Java8Interface.java_send(:message, [java.lang.CharSequence], 'world')).to eq("hello world")
  end

  it "exposes static methods via java_method" do
    expect(Java::Java8Interface.java_method(:message).call).to eq("hello")
  end

  it "exposes instance method via java_method" do
    method = Java::Java8Interface.java_method(:foo, [ java.lang.Object ])
    expect(method.name).to eq(:"foo(java.lang.Object)") # default
    method = Java::Java8Interface.java_method(:bar)
    expect(method.name).to eq(:"bar()") # abstract
  end if RUBY_VERSION > '1.9'

  it "default java_method is callable" do
    method = Java::Java8Interface.java_method(:foo, [ java.lang.Object ])
    expect( method.bind(Java::Java8Implementor.new).call '' ).to eql 'foo Java8Implementor'
  end

  it "binds default method as instance method" do
    expect( Java::Java8Interface.instance_methods(false) ).to include :foo
    expect( Java::Java8Implementor.new.foo(42) ).to eq("42foo Java8Implementor")
  end

  it "binds default method as instance method (Ruby receiver)" do
    klass = Class.new do
      include java.util.Iterator
      def hasNext; false end
      def next; nil end
    end
    expect( java.util.Iterator.instance_methods(false) ).to include :remove
    begin
      klass.new.remove
    rescue java.lang.UnsupportedOperationException
      # pass
    end
  end

  it "java_send works on implemented interface (default method)" do
    impl = Java::Java8Implementor.new
    expect(impl.java_send(:bar)).to eq("Java8Implementor")
    expect(impl.java_send(:foo, [ java.lang.Object ], 11)).to eq("11foo Java8Implementor")
  end

  it "works with java.util.function-al interface using proc implementation" do
    expect( Java::Java8Implementor.withConsumerCall(1) do |i|
      expect(i).to eql 10
    end ).to eql 2

    ret = Java::Java8Implementor.withPredicateCall([ ]) { |obj| obj.empty? }
    expect( ret ).to be true
    ret = Java::Java8Implementor.withPredicateCall('x') { |obj| obj.empty? }
    expect( ret ).to be false
  end

  it "does not override default methods using proc implementation" do
    expect( Java::Java8Interface.bar { 'BAR' } ).to eq 'BAR'
    expect( Java::Java8Interface.foo { 'BAR' } ).to eq 'foo BAR' # 'BAR' prior to 9.1

    res = Java::Java8Implementor.compose10pp # Java + 10 impl
    expect( res.apply(1.to_java(:int)) ).to eql 12
    # NOTE: has been failing prior to 9.1 as { ... } override #apply as well as #compose default method!
    res = Java::Java8Implementor.composepp { |i| i + 10 } # Ruby + 10 impl
    expect( res.apply(1.to_java(:int)) ).to eql 12
  end

  it "does not override default methods using proc implementation (non-functional interface)" do
    expect( Java::Java8Interface.subBar { |*args| "#{args.inspect}-SUB-BAR" } ).to eq '[]-SUB-BAR'
    expect( Java::Java8Interface.subBaz { |*args| "#{args.inspect}-SUB-BAR" } ).to eq '[]-SUB-BAR'
    expect( Java::Java8Interface.subFoo { |*args| "#{args.inspect}-SUB-BAR" } ).to eq 'foo []-SUB-BAR' # [""]-SUB-BAR prior to 9.1
  end

  it 'interface .impl() overrides all methods (by default)' do
    impl = Java::Java8Interface.impl { |*args| "#{args.inspect}-IMPL" }
    # NOTE: (historically) override every method (including default methods)
    expect( Java::Java8Interface.bar(impl) ).to eq '[:bar]-IMPL'
    expect( Java::Java8Interface.foo(impl) ).to eq '[:foo, ""]-IMPL'

    impl = Java::Java8Interface.impl(true) { |*args| "#{args.inspect}-IMPL" }
    expect( Java::Java8Interface.bar(impl) ).to eq '[:bar]-IMPL'
    expect( Java::Java8Interface.foo(impl) ).to eq '[:foo, ""]-IMPL'
  end

  it 'interface .impl(false) only overrides abstract methods' do
    impl = Java::Java8Interface.impl(false) { |*args| "#{args.inspect}-IMPL" }
    expect( Java::Java8Interface.bar(impl) ).to eq '[:bar]-IMPL'
    expect( Java::Java8Interface.foo(impl) ).to eq 'foo [:bar]-IMPL'

    impl = Java::Java8Interface::SubInterface.impl(false) { |*args| "#{args.inspect}-IMPL" }
    expect( Java::Java8Interface.bar(impl) ).to eq '[:bar]-IMPL'
    expect( Java::Java8Interface.foo(impl) ).to eq 'foo [:bar]-IMPL'
  end

  it 'interface .impl(method_names) only generates those methods' do
    impl = Java::Java8Interface.impl(:bar) { |*args| "#{args.inspect}-IMPL" }
    expect( Java::Java8Interface.bar(impl) ).to eq '[:bar]-IMPL'
    # NOTE: previously - NoMethodError: undefined method `foo' for Java::Default::Java8Interface:Module
    expect( Java::Java8Interface.foo(impl) ).to eq 'foo [:bar]-IMPL'
  end

  it 'interface .impl with unknown methods passed in warns' do
    output = with_stderr_captured do # prints a warning (since 9.1)
      impl = Java::Java8Interface.impl(:baz) { |*args| "#{args.inspect}-IMPL" }
      expect{ Java::Java8Interface.bar(impl) }.to raise_error(NoMethodError)
    end
    expect( output.index("`baz' is not a declared method in interface") ).to_not be nil
  end

  it "does not consider Map vs func-type Consumer ambiguous" do
    output = with_stderr_captured do # exact match should not warn :
      ret = Java::Java8Implementor.ambiguousCall1('') do |c|
        c.append('+proc')
      end
      expect( ret ).to eql "ambiguousWithConsumer+proc"
    end
    expect( output.index('ambiguous') ).to be nil

    output = with_stderr_captured do # exact match should not warn :
      ret = Java::Java8Implementor.ambiguousCall2('') do |c|
        c.append('+proc')
      end
      expect( ret ).to eql "ambiguousWithConsumer+proc"
    end
    expect( output.index('ambiguous') ).to be nil

    output = with_stderr_captured do # exact match should not warn :
      ret = Java::Java8Implementor.ambiguousCall3('') do |c|
        c.append('+proc')
      end
      expect( ret ).to eql "ambiguousWithConsumer+proc"
    end
    expect( output.index('ambiguous') ).to be nil
  end

  def javac_compile(files)
    compiler = javax.tools.ToolProvider.getSystemJavaCompiler
    fmanager = compiler.getStandardFileManager(nil, nil, nil)
    diagnostics = nil
    units = fmanager.getJavaFileObjectsFromStrings( files.map { |fname| fname.to_s } )
    compilation_task = compiler.getTask(nil, fmanager, diagnostics, nil, nil, units)
    compilation_task.call # returns boolean
  end

end
