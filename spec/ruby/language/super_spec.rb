require File.expand_path('../../spec_helper', __FILE__)
require File.expand_path('../fixtures/super', __FILE__)

describe "The super keyword" do
  it "calls the method on the calling class" do
    Super::S1::A.new.foo([]).should == ["A#foo","A#bar"]
    Super::S1::A.new.bar([]).should == ["A#bar"]
    Super::S1::B.new.foo([]).should == ["B#foo","A#foo","B#bar","A#bar"]
    Super::S1::B.new.bar([]).should == ["B#bar","A#bar"]
  end

  it "searches the full inheritence chain" do
    Super::S2::B.new.foo([]).should == ["B#foo","A#baz"]
    Super::S2::B.new.baz([]).should == ["A#baz"]
    Super::S2::C.new.foo([]).should == ["B#foo","C#baz","A#baz"]
    Super::S2::C.new.baz([]).should == ["C#baz","A#baz"]
  end

  it "searches class methods" do
    Super::S3::A.new.foo([]).should == ["A#foo"]
    Super::S3::A.foo([]).should == ["A::foo"]
    Super::S3::A.bar([]).should == ["A::bar","A::foo"]
    Super::S3::B.new.foo([]).should == ["A#foo"]
    Super::S3::B.foo([]).should == ["B::foo","A::foo"]
    Super::S3::B.bar([]).should == ["B::bar","A::bar","B::foo","A::foo"]
  end

  it "calls the method on the calling class including modules" do
    Super::MS1::A.new.foo([]).should == ["ModA#foo","ModA#bar"]
    Super::MS1::A.new.bar([]).should == ["ModA#bar"]
    Super::MS1::B.new.foo([]).should == ["B#foo","ModA#foo","ModB#bar","ModA#bar"]
    Super::MS1::B.new.bar([]).should == ["ModB#bar","ModA#bar"]
  end

  it "searches the full inheritence chain including modules" do
    Super::MS2::B.new.foo([]).should == ["ModB#foo","A#baz"]
    Super::MS2::B.new.baz([]).should == ["A#baz"]
    Super::MS2::C.new.baz([]).should == ["C#baz","A#baz"]
    Super::MS2::C.new.foo([]).should == ["ModB#foo","C#baz","A#baz"]
  end

  it "can resolve to different methods in an included module method" do
    Super::MultiSuperTargets::A.new.foo.should == :BaseA
    Super::MultiSuperTargets::B.new.foo.should == :BaseB
  end

  it "searches class methods including modules" do
    Super::MS3::A.new.foo([]).should == ["A#foo"]
    Super::MS3::A.foo([]).should == ["ModA#foo"]
    Super::MS3::A.bar([]).should == ["ModA#bar","ModA#foo"]
    Super::MS3::B.new.foo([]).should == ["A#foo"]
    Super::MS3::B.foo([]).should == ["B::foo","ModA#foo"]
    Super::MS3::B.bar([]).should == ["B::bar","ModA#bar","B::foo","ModA#foo"]
  end

  it "calls the correct method when the method visibility is modified" do
    Super::MS4::A.new.example.should == 5
  end

  it "calls the correct method when the superclass argument list is different from the subclass" do
    Super::S4::A.new.foo([]).should == ["A#foo"]
    Super::S4::B.new.foo([],"test").should == ["B#foo(a,test)", "A#foo"]
  end

  it "raises an error error when super method does not exist" do
    sup = Class.new
    sub_normal = Class.new(sup) do
      def foo
        super()
      end
    end
    sub_zsuper = Class.new(sup) do
      def foo
        super
      end
    end

    lambda {sub_normal.new.foo}.should raise_error(NoMethodError, /super/)
    lambda {sub_zsuper.new.foo}.should raise_error(NoMethodError, /super/)
  end

  it "calls the superclass method when in a block" do
    Super::S6.new.here.should == :good
  end

  it "calls the superclass method when initial method is defined_method'd" do
    Super::S7.new.here.should == :good
  end

  it "can call through a define_method multiple times (caching check)" do
    obj = Super::S7.new

    2.times do
      obj.here.should == :good
    end
  end

  it "supers up appropriate name even if used for multiple method names" do
    sup = Class.new do
      def a; "a"; end
      def b; "b"; end
    end

    sub = Class.new(sup) do
      [:a, :b].each do |name|
        define_method name do
          super()
        end
      end
    end

    sub.new.a.should == "a"
    sub.new.b.should == "b"
    sub.new.a.should == "a"
  end

  it "raises a RuntimeError when called with implicit arguments from a method defined with define_method" do
    super_class = Class.new do
      def a(arg)
        arg
      end
    end

    klass = Class.new super_class do
      define_method :a do |arg|
        super
      end
    end

    lambda { klass.new.a(:a_called) }.should raise_error(RuntimeError)
  end

  # Rubinius ticket github#157
  it "calls method_missing when a superclass method is not found" do
    lambda {
      Super::MM_B.new.is_a?(Hash).should == false
    }.should_not raise_error(NoMethodError)
  end

  # Rubinius ticket github#180
  it "respects the original module a method is aliased from" do
    lambda {
      Super::Alias3.new.name3.should == [:alias2, :alias1]
    }.should_not raise_error(RuntimeError)
  end

  it "sees the included version of a module a method is alias from" do
    lambda {
      Super::AliasWithSuper::Trigger.foo.should == [:b, :a]
    }.should_not raise_error(NoMethodError)
  end

  it "passes along modified rest args when they weren't originally empty" do
    Super::RestArgsWithSuper::B.new.a("bar").should == ["bar", "foo"]
  end

  it "passes along modified rest args when they were originally empty" do
    Super::RestArgsWithSuper::B.new.a.should == ["foo"]
  end

  it "invokes methods from a chain of anonymous modules" do
    Super::AnonymousModuleIncludedTwice.new.a([]).should == ["anon", "anon", "non-anon"]
  end
end
