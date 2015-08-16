require File.dirname(__FILE__) + "/../spec_helper"

describe "an interface (Java 8+)" do

  before :all do
    require 'tmpdir'; @tmpdir = Dir.mktmpdir
    files = []

    src = <<-JAVA
    public interface Java8Interface {
      static String message() { return "hello"; }
      static String message(CharSequence name) { return "hello " + name; }

      abstract String bar() ;
      default CharSequence foo(Object prefix) { return prefix + "foo" + ' ' + bar(); }
    }
    JAVA
    files << (file = "#{@tmpdir}/Java8Interface.java"); File.open(file, 'w') { |f| f.print(src) }

    src = <<-JAVA
    public class Java8Implemtor implements Java8Interface {
      public String bar() { return getClass().getSimpleName(); }
    }
    JAVA
    files << (file = "#{@tmpdir}/Java8Implemtor.java"); File.open(file, 'w') { |f| f.print(src) }

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
    expect(method.name).to eq("foo(java.lang.Object)") # default
    method = Java::Java8Interface.java_method(:bar)
    expect(method.name).to eq("bar()") # abstract
  end

  it "(default) java_method is callable" do
    method = Java::Java8Interface.java_method(:foo, [ java.lang.Object ])
    expect( method.bind(Java::Java8Implemtor.new).call '' ).to eql 'foo Java8Implemtor'
  end

  it "java_send works on impl (default method)" do
    impl = Java::Java8Implemtor.new
    expect(impl.java_send(:bar)).to eq("Java8Implemtor")
    expect(impl.java_send(:foo, [ java.lang.Object ], 11)).to eq("11foo Java8Implemtor")
  end

  def javac_compile(files)
    compiler = javax.tools.ToolProvider.getSystemJavaCompiler
    fmanager = compiler.getStandardFileManager(nil, nil, nil)
    diagnostics = nil
    units = fmanager.getJavaFileObjectsFromStrings( files.map { |fname| fname.to_s } )
    compilation_task = compiler.getTask(nil, fmanager, diagnostics, nil, nil, units)
    compilation_task.call # returns boolean
  end

end if ENV_JAVA['java.specification.version'] >= '1.8'
