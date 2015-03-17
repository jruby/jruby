require File.expand_path('../spec_helper', __FILE__)
require File.expand_path('../fixtures/class', __FILE__)

load_extension("class")

autoload :ClassUnderAutoload, "#{object_path}/class_under_autoload_spec"

describe :rb_path_to_class, :shared => true do
  it "returns a class or module from a scoped String" do
    @s.send(@method, "CApiClassSpecs::A::B").should equal(CApiClassSpecs::A::B)
  end

  it "resolves autoload constants" do
    @s.send(@method, "CApiClassSpecs::A::D").name.should == "CApiClassSpecs::A::D"
  end

  it "raises an ArgumentError if a constant in the path does not exist" do
    lambda { @s.send(@method, "CApiClassSpecs::NotDefined::B") }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError if the final constant does not exist" do
    lambda { @s.send(@method, "CApiClassSpecs::NotDefined") }.should raise_error(ArgumentError)
  end

  it "raises a TypeError if the constant is not a class or module" do
    lambda { @s.send(@method, "CApiClassSpecs::A::C") }.should raise_error(TypeError)
  end

  ruby_bug '#5691', '1.9.3' do
    it "raises an ArgumentError even if a constant in the path exists on toplevel" do
      lambda { @s.send(@method, "CApiClassSpecs::Object") }.should raise_error(ArgumentError)
    end
  end
end

describe "C-API Class function" do
  before :each do
    @s = CApiClassSpecs.new
  end

  describe "rb_class_new_instance" do
    it "allocates and initializes a new object" do
      o = @s.rb_class_new_instance(0, nil, CApiClassSpecs::Alloc)
      o.class.should == CApiClassSpecs::Alloc
      o.initialized.should be_true
    end

    it "passes arguments to the #initialize method" do
      o = @s.rb_class_new_instance(2, [:one, :two], CApiClassSpecs::Alloc)
      o.arguments.should == [:one, :two]
    end
  end

  describe "rb_include_module" do
    it "includes a module into a class" do
      o = CApiClassSpecs::IncludesM.new
      lambda { o.included? }.should raise_error(NameError)
      @s.rb_include_module(CApiClassSpecs::IncludesM, CApiClassSpecs::M)
      o.included?.should be_true
    end
  end

  describe "rb_define_attr" do
    before :each do
      @a = CApiClassSpecs::Attr.new
    end

    it "defines an attr_reader when passed true, false" do
      @s.rb_define_attr(CApiClassSpecs::Attr, :foo, true, false)
      @a.foo.should == 1
      lambda { @a.foo = 5 }.should raise_error(NameError)
    end

    it "defines an attr_writer when passed false, true" do
      @s.rb_define_attr(CApiClassSpecs::Attr, :bar, false, true)
      lambda { @a.bar }.should raise_error(NameError)
      @a.bar = 5
      @a.instance_variable_get(:@bar).should == 5
    end

    it "defines an attr_accessor when passed true, true" do
      @s.rb_define_attr(CApiClassSpecs::Attr, :baz, true, true)
      @a.baz.should == 3
      @a.baz = 6
      @a.baz.should == 6
    end
  end

  describe "rb_call_super" do
    it "calls the method in the superclass" do
      @s.define_call_super_method CApiClassSpecs::Sub, "call_super_method"
      obj = CApiClassSpecs::Sub.new
      obj.call_super_method.should == :super_method
    end

    it "calls the method in the superclass through two native levels" do
      @s.define_call_super_method CApiClassSpecs::Sub, "call_super_method"
      @s.define_call_super_method CApiClassSpecs::SubSub, "call_super_method"
      obj = CApiClassSpecs::SubSub.new
      obj.call_super_method.should == :super_method
    end
  end

  describe "rb_class2name" do
    it "returns the class name" do
      @s.rb_class2name(CApiClassSpecs).should == "CApiClassSpecs"
    end

    it "returns a string for an anonymous class" do
      @s.rb_class2name(Class.new).should be_kind_of(String)
    end
  end

  describe "rb_class_path" do
    it "returns a String of a class path with no scope modifiers" do
      @s.rb_class_path(Array).should == "Array"
    end

    it "returns a String of a class path with scope modifiers" do
      @s.rb_class_path(File::Stat).should == "File::Stat"
    end
  end

  describe "rb_class_name" do
    it "returns the class name" do
      @s.rb_class_name(CApiClassSpecs).should == "CApiClassSpecs"
    end

    it "returns a string for an anonymous class" do
      @s.rb_class_name(Class.new).should be_kind_of(String)
    end
  end

  describe "rb_path2class" do
    it_behaves_like :rb_path_to_class, :rb_path2class
  end

  describe "rb_path_to_class" do
    it_behaves_like :rb_path_to_class, :rb_path_to_class
  end

  describe "rb_cvar_defined" do
    it "returns false when the class variable is not defined" do
      @s.rb_cvar_defined(CApiClassSpecs::CVars, "@@nocvar").should be_false
    end

    it "returns true when the class variable is defined" do
      @s.rb_cvar_defined(CApiClassSpecs::CVars, "@@cvar").should be_true
    end

    it "returns true if the class instance variable is defined" do
      @s.rb_cvar_defined(CApiClassSpecs::CVars, "@c_ivar").should be_true
    end
  end

  describe "rb_cv_set" do
    it "sets a class variable" do
      o = CApiClassSpecs::CVars.new
      o.new_cv.should be_nil
      @s.rb_cv_set(CApiClassSpecs::CVars, "@@new_cv", 1)
      o.new_cv.should == 1
    end
  end

  describe "rb_cv_get" do
    it "returns the value of the class variable" do
      @s.rb_cvar_get(CApiClassSpecs::CVars, "@@cvar").should == :cvar
    end

    it "raises a NameError if the class variable is not defined" do
      lambda {
        @s.rb_cv_get(CApiClassSpecs::CVars, "@@no_cvar")
      }.should raise_error(NameError)
    end
  end

  describe "rb_cvar_set" do
    it "sets a class variable" do
      o = CApiClassSpecs::CVars.new
      o.new_cvar.should be_nil
      @s.rb_cvar_set(CApiClassSpecs::CVars, "@@new_cvar", 1)
      o.new_cvar.should == 1
    end

  end

  describe "rb_define_class_under" do
    it "creates a subclass of the superclass contained in a module" do
      cls = @s.rb_define_class_under(CApiClassSpecs,
                                     "ClassUnder1",
                                     CApiClassSpecs::Super)
      cls.should be_kind_of(Class)
      CApiClassSpecs::Super.should be_ancestor_of(CApiClassSpecs::ClassUnder1)
    end

    it "uses Object as the superclass if NULL is passed" do
      @s.rb_define_class_under(CApiClassSpecs, "ClassUnder2", nil)
      Object.should be_ancestor_of(CApiClassSpecs::ClassUnder2)
    end

    it "sets the class name" do
      cls = @s.rb_define_class_under(CApiClassSpecs, "ClassUnder3", nil)
      cls.name.should == "CApiClassSpecs::ClassUnder3"
    end

    it "calls #inherited on the superclass" do
      CApiClassSpecs::Super.should_receive(:inherited)
      cls = @s.rb_define_class_under(CApiClassSpecs,
                                     "ClassUnder4", CApiClassSpecs::Super)
    end

    it "defines a class for an existing Autoload" do
      compile_extension("class_under_autoload")

      ClassUnderAutoload.name.should == "ClassUnderAutoload"
    end
  end

  describe "rb_define_class_variable" do
    it "sets a class variable" do
      o = CApiClassSpecs::CVars.new
      o.rbdcv_cvar.should be_nil
      @s.rb_define_class_variable(CApiClassSpecs::CVars, "@@rbdcv_cvar", 1)
      o.rbdcv_cvar.should == 1
    end
  end

  describe "rb_cvar_get" do
    it "returns the value of the class variable" do
      @s.rb_cvar_get(CApiClassSpecs::CVars, "@@cvar").should == :cvar
    end

    it "raises a NameError if the class variable is not defined" do
      lambda {
        @s.rb_cvar_get(CApiClassSpecs::CVars, "@@no_cvar")
      }.should raise_error(NameError)
    end
  end

  describe "rb_class_inherited" do
    before :each do
      @subclass = Class.new
    end

    it "calls superclass.inherited(subclass)" do
      @s.rb_class_inherited(CApiClassSpecs::Inherited, @subclass).should equal(@subclass)
    end

    it "calls Object.inherited(subclass) if superclass is C NULL" do
      Object.should_receive(:inherited).with(@subclass)

      # Pass false to have the specs helper C function pass NULL
      @s.rb_class_inherited(false, @subclass)
    end
  end

  describe "rb_class_new" do
    it "returns an new subclass of the superclass" do
      subclass = @s.rb_class_new(CApiClassSpecs::NewClass)
      CApiClassSpecs::NewClass.should be_ancestor_of(subclass)
    end

    it "raises a TypeError if passed Class as the superclass" do
      lambda { @s.rb_class_new(Class) }.should raise_error(TypeError)
    end

    it "raises a TypeError if passed a singleton class as the superclass" do
      metaclass = Object.new.singleton_class
      lambda { @s.rb_class_new(metaclass) }.should raise_error(TypeError)
    end
  end

  describe "rb_class_superclass" do
    it "returns the superclass of a class" do
      cls = @s.rb_class_superclass(CApiClassSpecs::Sub)
      cls.should == CApiClassSpecs::Super
    end

    it "returns nil if the class has no superclass" do
      @s.rb_class_superclass(BasicObject).should be_nil
    end
  end

  describe "rb_class_real" do
    it "returns the class of an object ignoring the singleton class" do
      obj = CApiClassSpecs::Sub.new
      def obj.some_method() end

      @s.rb_class_real(obj).should == CApiClassSpecs::Sub
    end

    it "returns the class of an object ignoring included modules" do
      obj = CApiClassSpecs::SubM.new
      @s.rb_class_real(obj).should == CApiClassSpecs::SubM
    end

    it "returns 0 if passed 0" do
      @s.rb_class_real(0).should == 0
    end
  end
end
