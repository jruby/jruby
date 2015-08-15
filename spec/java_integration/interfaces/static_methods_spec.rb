require File.dirname(__FILE__) + "/../spec_helper"

describe "an interface with static methods (Java 8+)" do

  before :all do
    src = <<-JAVA
    public interface Java8Interface {
      static String message() { return "hello"; }
      static String message(CharSequence name) { return "hello " + name; }
    }
    JAVA

    require 'tmpdir'; @tmpdir = Dir.mktmpdir
    @src = File.open(file = "#{@tmpdir}/Java8Interface.java", 'w') { |f| f.print(src) }

    fail "#{file.inspect} compilation failed!" unless javac_compile [ file ]
    # puts Dir.entries(@tmpdir).inspect
    $CLASSPATH << @tmpdir
  end

  after :all do
    FileUtils.rm_rf @tmpdir
  end

  it "binds those methods on the proxy module" do
    expect(Java::Java8Interface.message).to eq("hello")
  end

  it "exposes those methods via java_send" do
    expect(Java::Java8Interface.java_send(:message)).to eq("hello")
  end

  it "exposes those methods via java_send with type" do
    expect(Java::Java8Interface.java_send(:message, [java.lang.CharSequence], 'world')).to eq("hello world")
  end

  it "exposes those methods via java_method" do
    expect(Java::Java8Interface.java_method(:message).call).to eq("hello")
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
