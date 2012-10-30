require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9.3" do
  require File.expand_path('../fixtures/classes19', __FILE__)
  
  describe "Module#public_constant" do
    it "can only be passed constant names defined in the target (self) module" do
      cls1 = Class.new
      cls1.const_set :Foo, true
      cls2 = Class.new(cls1)
      
      lambda do
        cls2.send :public_constant, :Foo
      end.should raise_error(NameError)
    end
    
    ruby_bug "[ruby-list:48558]", "1.9.3" do
      it "accepts multiple names" do
        mod = Module.new
        mod.const_set :Foo, true
        mod.const_set :Bar, true
        
        mod.send :private_constant, :Foo
        mod.send :private_constant, :Bar
        
        mod.send :public_constant, :Foo, :Bar
        
        mod::Foo.should == true
        mod::Bar.should == true
      end
    end
  end
  
  describe "Module#public_constant marked constants" do
    before :each do
      @module = ModuleSpecs::PrivConstModule.dup
    end
    
    describe "in a module" do
      it "can be accessed from outside the module" do
        @module.send :public_constant, :PRIVATE_CONSTANT_MODULE
        @module::PRIVATE_CONSTANT_MODULE.should == true
      end
      
      it "is defined? with A::B form" do
        @module.send :public_constant, :PRIVATE_CONSTANT_MODULE
        defined?(@module::PRIVATE_CONSTANT_MODULE).should == "constant"
      end
    end
    
    describe "in a class" do
      before :each do
        @class = ModuleSpecs::PrivConstClass.dup
      end
      
      it "can be accessed from outside the class" do
        @class.send :public_constant, :PRIVATE_CONSTANT_CLASS
        @class::PRIVATE_CONSTANT_CLASS.should == true
      end
      
      it "is defined? with A::B form" do
        @class.send :public_constant, :PRIVATE_CONSTANT_CLASS
        defined?(@class::PRIVATE_CONSTANT_CLASS).should == "constant"
      end
    end
    
    describe "in Object" do
      after :each do
        ModuleSpecs.reset_private_constants
      end
      
      it "can be accessed using ::Const form" do
        Object.send :public_constant, :PRIVATE_CONSTANT_IN_OBJECT
        ::PRIVATE_CONSTANT_IN_OBJECT.should == true
      end
      
      it "is defined? using ::Const form" do
        Object.send :public_constant, :PRIVATE_CONSTANT_IN_OBJECT
        defined?(::PRIVATE_CONSTANT_IN_OBJECT).should == "constant"
      end
    end
  end
end
