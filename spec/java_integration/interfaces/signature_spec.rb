require File.expand_path('../../spec_helper', __FILE__)

describe "Java interface implemented method signatures and arity" do

  before :all do
    require 'tmpdir'
    @tmpdir = Dir.mktmpdir
    files = []

    src = <<-JAVA
    public interface CustomOverloadInterface {
        void zeroOrTwo();
        void zeroOrTwo(int a, int b);

        void oneOrTwo(int a);
        void oneOrTwo(int a, int b);

        void sameArityDiffTypes(int a);
        void sameArityDiffTypes(String a);

        void withVarargs(String prefix, int... numbers);

        default String defaultMethodWithOverload(String s) { return s; }
        String defaultMethodWithOverload(String s, int n);
    }
    JAVA
    files << (f1 = "#{@tmpdir}/CustomOverloadInterface.java")
    File.open(f1, 'w') { |f| f.print(src) }

    src = <<-JAVA
    public class CustomOverloadCaller {
        public interface FunctionalWithDefault {
            int compute(int x);
            default int compute(int x, int y) { return compute(x) + y; }
        }

        public static int callCompute(int val, FunctionalWithDefault f) {
            return f.compute(val);
        }
    }
    JAVA
    files << (f2 = "#{@tmpdir}/CustomOverloadCaller.java")
    File.open(f2, 'w') { |f| f.print(src) }

    fail "compilation failed" unless javac_compile files
    $CLASSPATH << @tmpdir
  end

  def javac_compile(files)
    compiler = javax.tools.ToolProvider.getSystemJavaCompiler
    fmanager = compiler.getStandardFileManager(nil, nil, nil)
    units = fmanager.getJavaFileObjectsFromStrings(files.map(&:to_s))
    task = compiler.getTask(nil, fmanager, nil, nil, nil, units)
    task.call
  end

  after :all do
    FileUtils.rm_rf @tmpdir
  end

  describe "Interface.impl" do
    it "reports correct arity for 0-argument methods" do
      runnable = java.lang.Runnable.impl { |*args| }
      expect(runnable.method(:run).arity).to eq 0
    end

    it "reports correct arity for 1-argument methods" do
      consumer = java.util.function.Consumer.impl { |*args| }
      expect(consumer.method(:accept).arity).to eq 1

      comparable = java.lang.Comparable.impl { |*args| 0 }
      expect(comparable.method(:compareTo).arity).to eq 1
    end

    it "reports correct arity for 2-argument methods" do
      comparator = java.util.Comparator.impl { |*args| 0 }
      expect(comparator.method(:compare).arity).to eq 2

      bifunction = java.util.function.BiFunction.impl { |*args| "result" }
      expect(bifunction.method(:apply).arity).to eq 2
    end

    it "reports correct arity for 3-argument and 4-argument methods" do
      iface3 = Java::java_integration.fixtures.iface.SingleMethodInterfaceWith3Args.impl { |*args| }
      expect(iface3.method(:doIt).arity).to eq 3

      iface4 = Java::java_integration.fixtures.iface.SingleMethodInterfaceWith4Args.impl { |*args| }
      expect(iface4.method(:doIt).arity).to eq 4
    end

    it "reports correct arity for overloaded methods with same arity but different parameter types" do
      # Path has resolve(String) and resolve(Path) - both take 1 argument
      path = java.nio.file.Path.impl { |*args| nil }
      expect(path.method(:resolve).arity).to eq 1

      custom = Java::CustomOverloadInterface.impl { |*args| }
      expect(custom.method(:sameArityDiffTypes).arity).to eq 1
    end

    it "reports correct arity for overloaded methods with varying parameter counts" do
      # Appendable has append(char) [1], append(CharSequence) [1], and append(CharSequence, int, int) [3]
      appendable = java.lang.Appendable.impl { |*args| }
      expect(appendable.method(:append).arity).to eq(-2)

      # BlockingQueue has poll() [0] and poll(long, TimeUnit) [2] -> min=0, opt=2 -> -(0 + 1) = -1
      bq = java.util.concurrent.BlockingQueue.impl { |*args| nil }
      expect(bq.method(:poll).arity).to eq(-1)
      # BlockingQueue has offer(e) [1] and offer(e, long, TimeUnit) [3] -> min=1, opt=2 -> -(1 + 1) = -2
      expect(bq.method(:offer).arity).to eq(-2)

      custom = Java::CustomOverloadInterface.impl { |*args| }
      expect(custom.method(:zeroOrTwo).arity).to eq(-1)
      expect(custom.method(:oneOrTwo).arity).to eq(-2)
    end

    it "reports correct arity for overloaded methods with varargs" do
      # System.Logger has log(Level, String, Object...) -> 2 required + varargs -> -(2 + 1) = -3
      logger = java.lang.System::Logger.impl { |*args| }
      expect(logger.method(:log).arity).to eq(-3)

      # withVarargs(String, int...) -> 1 required + varargs -> -(1 + 1) = -2
      custom = Java::CustomOverloadInterface.impl { |*args| }
      expect(custom.method(:withVarargs).arity).to eq(-2)
    end

    it "reports correct arity when implementing specific methods" do
      comparator = java.util.Comparator.impl(:compare) { |*args| 0 }
      expect(comparator.method(:compare).arity).to eq 2
      expect(comparator.class.instance_methods(false)).not_to include(:reversed)
      expect(comparator.method(:reversed).owner).to eq java.util.Comparator
    end

    it "reports correct arity when impl(false) only implements abstract methods" do
      comparator = java.util.Comparator.impl(false) { |*args| 0 }
      expect(comparator.method(:compare).arity).to eq 2
      expect(comparator.class.instance_methods(false)).not_to include(:reversed)
      expect(comparator.method(:reversed).owner).to eq java.util.Comparator

      # defaultMethodWithOverload has default (String) and abstract (String, int)
      # With impl(false), only the abstract (String, int) is implemented -> arity is 2
      custom_false = Java::CustomOverloadInterface.impl(false) { |*args| }
      expect(custom_false.method(:defaultMethodWithOverload).arity).to eq 2

      # With default impl(true), both are overridden -> min=1, max=2 -> -(1 + 1) = -2
      custom_all = Java::CustomOverloadInterface.impl { |*args| }
      expect(custom_all.method(:defaultMethodWithOverload).arity).to eq(-2)
    end

    it "generates working Java wrapper with to_java" do
      comparator = java.util.Comparator.impl(false) { |name, a, b| a <=> b }
      wrapper = comparator.to_java
      expect(wrapper.compare("a", "b")).to eq(-1)
      expect(wrapper.compare("b", "a")).to eq(1)
      expect(wrapper.compare("a", "a")).to eq(0)
    end
  end

  describe "proc-to-interface conversion" do
    it "reports correct arity for 1-argument coerced interfaces" do
      list = java.util.ArrayList.new
      list.add("hello")
      pr = proc { |s| s.length > 3 }
      list.removeIf(pr)
      expect(pr.method(:test).arity).to eq 1
    end

    it "reports correct arity for 2-argument coerced interfaces" do
      list = java.util.ArrayList.new
      list.add("b")
      list.add("a")
      pr = proc { |a, b| a <=> b }
      java.util.Collections.sort(list, pr)
      expect(pr.method(:compare).arity).to eq 2
    end

    it "reports correct arity when functional interface has default overloads" do
      pr = proc { |x| x * 3 }
      result = Java::CustomOverloadCaller.callCompute(4, pr)
      expect(result).to eq 12
      # The proc implements only the abstract compute(int) [1 arg], not the default compute(int, int)
      expect(pr.method(:compute).arity).to eq 1
    end
  end

end
