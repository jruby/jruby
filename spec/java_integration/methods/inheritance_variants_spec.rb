require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.inheritance.MultiCtorBase"
java_import "java_integration.fixtures.inheritance.MultiCtorSubclass"

# Exercises the various split-constructor fast paths exposed by SplitCtorPlan: direct forwarding
# (no-args / fixed-required / rest), terminal-literal super, terminal-literal-with-cached-terminator,
# arbitrary pre-super work, and the interpreted split fallback. Each variant is intentionally a
# slightly different shape so that the JIT/interpreter take the correct strategy and the cached
# terminator does not get reused across different super-arg shapes.
describe "Java subclassing - constructor argument variants" do

  describe "literal super (terminal)" do
    it "selects the no-arg Java ctor for super()" do
      cls = Class.new(MultiCtorBase) { def initialize; super(); end }
      obj = cls.new
      expect(obj.ctor).to eq("()")
      expect(obj.trace.to_a).to eq(["Java () ctor"])
    end

    it "selects the (String) Java ctor for super('s')" do
      cls = Class.new(MultiCtorBase) { def initialize; super("hello"); end }
      obj = cls.new
      expect(obj.ctor).to eq("(String)")
      expect(obj.trace.to_a).to eq(["Java (String) ctor with hello"])
    end

    it "selects the (int) Java ctor for super(7)" do
      cls = Class.new(MultiCtorBase) { def initialize; super(7); end }
      obj = cls.new
      expect(obj.ctor).to eq("(int)")
      expect(obj.trace.to_a).to eq(["Java (int) ctor with 7"])
    end

    it "selects the (String,int) Java ctor for super('foo', 42)" do
      cls = Class.new(MultiCtorBase) { def initialize; super("foo", 42); end }
      obj = cls.new
      expect(obj.ctor).to eq("(String,int)")
      expect(obj.trace.to_a).to eq(["Java (String,int) ctor with foo,42"])
    end

    it "selects the (int,int,int) Java ctor for super(1,2,3)" do
      cls = Class.new(MultiCtorBase) { def initialize; super(1, 2, 3); end }
      obj = cls.new
      expect(obj.ctor).to eq("(int,int,int)")
      expect(obj.trace.to_a).to eq(["Java (int,int,int) ctor with 1,2,3"])
    end

    it "selects a different cached terminator per class with different literal args" do
      cls_a = Class.new(MultiCtorBase) { def initialize; super(1); end }
      cls_b = Class.new(MultiCtorBase) { def initialize; super("a", 2); end }
      cls_c = Class.new(MultiCtorBase) { def initialize; super(); end }

      # Interleave constructions to make sure each class' cached terminator is independent
      a1 = cls_a.new; b1 = cls_b.new; c1 = cls_c.new
      a2 = cls_a.new; b2 = cls_b.new; c2 = cls_c.new

      expect(a1.ctor).to eq("(int)")
      expect(a2.ctor).to eq("(int)")
      expect(b1.ctor).to eq("(String,int)")
      expect(b2.ctor).to eq("(String,int)")
      expect(c1.ctor).to eq("()")
      expect(c2.ctor).to eq("()")
    end

    it "still works after many invocations (cached terminator is reused)" do
      cls = Class.new(MultiCtorBase) { def initialize; super("x", 9); end }
      1_000.times do
        obj = cls.new
        expect(obj.ctor).to eq("(String,int)")
      end
    end
  end

  describe "direct super forwarding (no pre-super work)" do
    it "forwards *args -> super(*args) to the matching Java ctor" do
      cls = Class.new(MultiCtorBase) do
        def initialize(*args)
          super(*args)
        end
      end

      expect(cls.new("hi").ctor).to eq("(String)")
      expect(cls.new(7).ctor).to eq("(int)")
      expect(cls.new("hi", 7).ctor).to eq("(String,int)")
      expect(cls.new(1, 2, 3).ctor).to eq("(int,int,int)")
      expect(cls.new.ctor).to eq("()")
    end

    it "forwards fixed-required-args super(a,b) without ZSuper" do
      cls = Class.new(MultiCtorBase) do
        def initialize(a, b)
          super(a, b)
        end
      end

      expect(cls.new("hi", 7).ctor).to eq("(String,int)")
    end

    it "forwards single fixed arg super(a)" do
      cls = Class.new(MultiCtorBase) do
        def initialize(a)
          super(a)
        end
      end

      expect(cls.new("hi").ctor).to eq("(String)")
      expect(cls.new(7).ctor).to eq("(int)")
    end

    it "forwards bare super inside (first, *rest) form" do
      cls = Class.new(MultiCtorBase) do
        def initialize(first, *rest)
          super
        end
      end

      expect(cls.new("hi").ctor).to eq("(String)")
      expect(cls.new("hi", 7).ctor).to eq("(String,int)")
    end

    it "forwards bare super inside no-arg form" do
      cls = Class.new(MultiCtorBase) do
        def initialize
          super
        end
      end

      expect(cls.new.ctor).to eq("()")
    end
  end

  describe "arbitrary pre-super work (split interpreter path)" do
    it "computes super args dynamically from received args" do
      cls = Class.new(MultiCtorBase) do
        def initialize(opts)
          a = opts[:s]
          b = opts[:n] + 1
          super(a, b)
        end
      end
      obj = cls.new(s: "z", n: 41)
      expect(obj.ctor).to eq("(String,int)")
      expect(obj.trace.to_a).to eq(["Java (String,int) ctor with z,42"])
    end

    it "respects reassignment of arg locals before super" do
      cls = Class.new(MultiCtorBase) do
        def initialize(s)
          s = s.upcase
          super(s)
        end
      end
      expect(cls.new("foo").ctor).to eq("(String)")
      expect(cls.new("foo").trace.to_a).to eq(["Java (String) ctor with FOO"])
    end

    it "runs Ruby code after super (post-super continuation)" do
      cls = Class.new(MultiCtorBase) do
        attr_reader :ruby_marker
        def initialize(s)
          super(s)
          @ruby_marker = "after-#{s}"
        end
      end
      obj = cls.new("bar")
      expect(obj.ruby_marker).to eq("after-bar")
      expect(obj.ctor).to eq("(String)")
    end

    it "handles multiple separate pre-super computations" do
      cls = Class.new(MultiCtorBase) do
        def initialize(parts)
          a = parts.first * 2
          b = parts.last + 100
          super(a.to_s, b.to_i)
        end
      end
      obj = cls.new([3, 5])
      expect(obj.ctor).to eq("(String,int)")
      expect(obj.trace.to_a).to eq(["Java (String,int) ctor with 6,105"])
    end
  end

  describe "subsubclassing (Ruby < Ruby < Java)" do
    it "chains constructors when middle class adds Ruby pre-super work" do
      middle = Class.new(MultiCtorBase) do
        attr_reader :middle_marker
        def initialize(s)
          super("M-#{s}")
          @middle_marker = "middle"
        end
      end
      leaf = Class.new(middle) do
        attr_reader :leaf_marker
        def initialize(s)
          super("L-#{s}")
          @leaf_marker = "leaf"
        end
      end

      obj = leaf.new("x")
      expect(obj.ctor).to eq("(String)")
      expect(obj.middle_marker).to eq("middle")
      expect(obj.leaf_marker).to eq("leaf")
      expect(obj.trace.to_a).to eq(["Java (String) ctor with M-L-x"])
    end

    it "chains constructors via direct super forwarding all the way down" do
      middle = Class.new(MultiCtorBase) do
        def initialize(*args); super(*args); end
      end
      leaf = Class.new(middle) do
        def initialize(*args); super(*args); end
      end

      expect(leaf.new("hi").ctor).to eq("(String)")
      expect(leaf.new(7).ctor).to eq("(int)")
      expect(leaf.new("hi", 7).ctor).to eq("(String,int)")
    end

    it "supports a 3-deep Ruby chain with mixed strategies" do
      l1 = Class.new(MultiCtorBase) do
        def initialize(s)
          super(s) # direct fixed forwarding
        end
      end
      l2 = Class.new(l1) do
        attr_reader :l2_called
        def initialize(s)
          tag = "l2-#{s}"
          super(tag) # arbitrary pre-super (computed local)
          @l2_called = true
        end
      end
      l3 = Class.new(l2) do
        attr_reader :l3_called
        def initialize(s)
          super(s) # direct fixed forwarding again
          @l3_called = true
        end
      end

      obj = l3.new("z")
      expect(obj.ctor).to eq("(String)")
      expect(obj.l2_called).to be true
      expect(obj.l3_called).to be true
      expect(obj.trace.to_a).to eq(["Java (String) ctor with l2-z"])
    end

    it "constructs subclass of Java subclass with terminal literal super" do
      cls = Class.new(MultiCtorSubclass) { def initialize; super(99); end }
      obj = cls.new
      expect(obj.ctor).to eq("(int)")
      expect(obj.trace.to_a).to eq([
        "Java (int) ctor with 99",
        "Java MultiCtorSubclass(int) ctor with 99"
      ])
    end

    it "constructs subclass of Java subclass with direct forwarding" do
      cls = Class.new(MultiCtorSubclass) do
        def initialize(*args); super(*args); end
      end
      obj = cls.new("hi")
      expect(obj.ctor).to eq("(String)")
      expect(obj.trace.to_a).to eq([
        "Java (String) ctor with hi",
        "Java MultiCtorSubclass(String) ctor with hi"
      ])
    end

    it "constructs subclass of Java subclass with arbitrary pre-super work" do
      cls = Class.new(MultiCtorSubclass) do
        attr_reader :tag
        def initialize(s, n)
          @tag = "pre-#{s}-#{n}"
          super("#{s}!")
        end
      end
      obj = cls.new("x", 7)
      expect(obj.ctor).to eq("(String)")
      expect(obj.tag).to eq("pre-x-7")
      expect(obj.trace.to_a).to eq([
        "Java (String) ctor with x!",
        "Java MultiCtorSubclass(String) ctor with x!"
      ])
    end
  end

  describe "blocks (terminal super shouldn't break when a block is passed)" do
    it "ignores a passed block (no Java ctor accepts one)" do
      cls = Class.new(MultiCtorBase) { def initialize; super("blockless"); end }
      # The block is passed to .new but the Java ctor doesn't accept it - just verify no error
      obj = cls.new { :unused }
      expect(obj.ctor).to eq("(String)")
    end
  end

end
