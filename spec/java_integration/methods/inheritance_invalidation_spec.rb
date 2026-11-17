require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.inheritance.MultiCtorBase"
java_import "java_integration.fixtures.inheritance.MultiCtorSubclass"

# Verifies that the SplitCtorPlan / terminator / recursive-plan caches are correctly invalidated
# when the receiving class' generation changes (e.g. method (re)definition, module inclusion, etc.).
# These cases were added alongside the perf caches; without proper invalidation, redefining
# `initialize` after a class has been constructed once would silently keep using the stale strategy.
describe "Java subclassing - cache invalidation" do

  describe "redefining initialize" do
    it "re-resolves the strategy when initialize is redefined to a different shape" do
      cls = Class.new(MultiCtorBase) do
        def initialize; super("first"); end
      end

      a = cls.new
      expect(a.ctor).to eq("(String)")
      expect(a.trace.to_a).to eq(["Java (String) ctor with first"])

      # Redefine to call a different Java ctor; this must bump the generation and invalidate
      # both the top-level plan and the cached literal-super terminator.
      cls.class_eval do
        def initialize; super(123); end
      end

      b = cls.new
      expect(b.ctor).to eq("(int)")
      expect(b.trace.to_a).to eq(["Java (int) ctor with 123"])
    end

    it "re-resolves when switching from terminal literal super to arbitrary pre-super work" do
      cls = Class.new(MultiCtorBase) do
        def initialize; super(7); end
      end

      a = cls.new
      expect(a.ctor).to eq("(int)")

      cls.class_eval do
        def initialize(x)
          y = x + 100
          super(y)
        end
      end

      b = cls.new(5)
      expect(b.ctor).to eq("(int)")
      expect(b.trace.to_a).to eq(["Java (int) ctor with 105"])
    end

    it "re-resolves when switching between direct forwarding and pre-super computation" do
      cls = Class.new(MultiCtorBase) do
        def initialize(*args); super(*args); end
      end

      a = cls.new("hi")
      expect(a.ctor).to eq("(String)")
      a2 = cls.new(7)
      expect(a2.ctor).to eq("(int)")

      cls.class_eval do
        def initialize(s)
          super(s.upcase)
        end
      end

      b = cls.new("foo")
      expect(b.ctor).to eq("(String)")
      expect(b.trace.to_a).to eq(["Java (String) ctor with FOO"])
    end

    it "re-resolves when initialize is replaced with one that has no super" do
      cls = Class.new(MultiCtorBase) do
        def initialize; super("with-super"); end
      end

      a = cls.new
      expect(a.ctor).to eq("(String)")

      # No super call - the default Java ctor () must still get picked
      cls.class_eval do
        def initialize; end
      end

      b = cls.new
      expect(b.ctor).to eq("()")
    end

    it "re-resolves when initialize is removed (falls back to inherited)" do
      cls = Class.new(MultiCtorBase) do
        def initialize; super(7); end
      end
      expect(cls.new.ctor).to eq("(int)")

      cls.send(:remove_method, :initialize)
      # Without an explicit initialize, the inherited path must still set up the proxy
      # MultiCtorBase has a no-arg java ctor, so .new with no args should hit that.
      expect(cls.new.ctor).to eq("()")
    end
  end

  describe "module prepend / include after first use" do
    it "re-resolves when a module redefining initialize is prepended" do
      cls = Class.new(MultiCtorBase) do
        def initialize; super("base"); end
      end

      a = cls.new
      expect(a.ctor).to eq("(String)")

      mod = Module.new do
        def initialize
          super
          @pre_module = "tagged"
        end
      end
      cls.prepend(mod)

      b = cls.new
      expect(b.instance_variable_get(:@pre_module)).to eq("tagged")
      expect(b.ctor).to eq("(String)")
      expect(b.trace.to_a).to eq(["Java (String) ctor with base"])
    end

    it "re-resolves when a module is included that intercepts initialize" do
      cls = Class.new(MultiCtorBase) do
        def initialize(n); super(n); end
      end

      a = cls.new(1)
      expect(a.ctor).to eq("(int)")

      mod = Module.new do
        def initialize(n)
          super(n * 2)
          @doubled = true
        end
      end
      cls.prepend(mod) # `include` would be below `cls` so its #initialize wouldn't intercept

      b = cls.new(3)
      expect(b.ctor).to eq("(int)")
      expect(b.trace.to_a).to eq(["Java (int) ctor with 6"])
      expect(b.instance_variable_get(:@doubled)).to be true
    end
  end

  describe "redefining initialize on the parent Java proxy class itself" do
    around(:each) do |example|
      # Snapshot the existing MultiCtorBase#initialize so we can restore (it may be
      # absent in which case we simply re-remove after the test).
      had_method = MultiCtorBase.method_defined?(:initialize) ||
                   MultiCtorBase.private_method_defined?(:initialize)
      example.run
      MultiCtorBase.send(:remove_method, :initialize) if MultiCtorBase.method_defined?(:initialize) || MultiCtorBase.private_method_defined?(:initialize)
      # Restore default proxy initialize by triggering a fresh search; nothing to do here since
      # ConcreteJavaProxy installs the default. The fixture is shared across the file though, so
      # leaving it without a custom initialize is exactly the desired baseline.
    end

    it "re-resolves for a subclass when the parent's initialize is redefined later" do
      child = Class.new(MultiCtorBase) do
        def initialize; super("child"); end
      end

      first = child.new
      expect(first.ctor).to eq("(String)")
      expect(first.trace.to_a).to eq(["Java (String) ctor with child"])

      # Now redefine MultiCtorBase#initialize - any cached plan derived from the parent's
      # state must be invalidated for descendants too.
      MultiCtorBase.class_eval do
        def initialize(*args)
          super(*args)
          getTrace().add("Ruby parent initialize")
        end
      end

      second = child.new
      expect(second.ctor).to eq("(String)")
      expect(second.trace.to_a).to eq([
        "Java (String) ctor with child",
        "Ruby parent initialize"
      ])
    end
  end

  describe "subclass invalidation does not affect siblings" do
    it "independently caches plans per subclass" do
      sib_a = Class.new(MultiCtorBase) { def initialize; super("a"); end }
      sib_b = Class.new(MultiCtorBase) { def initialize; super(2); end }

      # Construct each so its plan/terminator gets cached
      expect(sib_a.new.ctor).to eq("(String)")
      expect(sib_b.new.ctor).to eq("(int)")

      # Redefining sib_a must not affect sib_b's plan
      sib_a.class_eval do
        def initialize; super("a-prime"); end
      end

      expect(sib_a.new.trace.to_a).to eq(["Java (String) ctor with a-prime"])
      expect(sib_b.new.trace.to_a).to eq(["Java (int) ctor with 2"])
    end

    it "independently caches recursive (Ruby<Ruby<Java) plans" do
      mid_a = Class.new(MultiCtorBase) { def initialize(s); super(s); end }
      mid_b = Class.new(MultiCtorBase) { def initialize(s); super(s.upcase); end }
      leaf_a = Class.new(mid_a) { def initialize(s); super(s); end }
      leaf_b = Class.new(mid_b) { def initialize(s); super(s); end }

      expect(leaf_a.new("x").trace.to_a).to eq(["Java (String) ctor with x"])
      expect(leaf_b.new("x").trace.to_a).to eq(["Java (String) ctor with X"])

      # Mutate mid_a; mid_b path stays intact
      mid_a.class_eval do
        def initialize(s); super("[#{s}]"); end
      end

      expect(leaf_a.new("y").trace.to_a).to eq(["Java (String) ctor with [y]"])
      expect(leaf_b.new("y").trace.to_a).to eq(["Java (String) ctor with Y"])
    end
  end

  describe "high-volume reuse exercises the caches" do
    it "stays correct under tight loops of the same subclass" do
      cls = Class.new(MultiCtorBase) { def initialize; super("loop"); end }
      5_000.times do
        obj = cls.new
        expect(obj.ctor).to eq("(String)")
      end
    end

    it "stays correct interleaving multiple subclasses sharing the same Java parent" do
      cls_a = Class.new(MultiCtorBase) { def initialize; super(1); end }
      cls_b = Class.new(MultiCtorBase) { def initialize; super(2, 3, 4); end }
      cls_c = Class.new(MultiCtorBase) { def initialize; super("x", 9); end }

      1_000.times do
        expect(cls_a.new.ctor).to eq("(int)")
        expect(cls_b.new.ctor).to eq("(int,int,int)")
        expect(cls_c.new.ctor).to eq("(String,int)")
      end
    end

    it "stays correct interleaving construction with method redefinitions" do
      cls = Class.new(MultiCtorBase) { def initialize; super("v1"); end }
      expect(cls.new.trace.to_a).to eq(["Java (String) ctor with v1"])

      cls.class_eval { def initialize; super("v2"); end }
      expect(cls.new.trace.to_a).to eq(["Java (String) ctor with v2"])
      expect(cls.new.trace.to_a).to eq(["Java (String) ctor with v2"]) # cached

      cls.class_eval { def initialize; super(99); end }
      expect(cls.new.trace.to_a).to eq(["Java (int) ctor with 99"])

      cls.class_eval { def initialize(*args); super(*args); end }
      expect(cls.new("hi").trace.to_a).to eq(["Java (String) ctor with hi"])
      expect(cls.new(7).trace.to_a).to eq(["Java (int) ctor with 7"])
    end
  end

  describe "method visibility / class manipulation" do
    it "tolerates singleton method definition on a subclass instance" do
      cls = Class.new(MultiCtorBase) { def initialize; super("inst"); end }
      obj = cls.new
      def obj.extra; "extra-method"; end

      expect(obj.ctor).to eq("(String)")
      expect(obj.extra).to eq("extra-method")

      # Next construction of the class still uses the cached plan correctly.
      expect(cls.new.ctor).to eq("(String)")
    end
  end

end
