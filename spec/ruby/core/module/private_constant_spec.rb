require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9.3" do
  require File.expand_path('../fixtures/classes19', __FILE__)
  
  describe "Module#private_constant" do
    it "can only be passed constant names defined in the target (self) module" do
      cls1 = Class.new
      cls1.const_set :Foo, true
      cls2 = Class.new(cls1)
      
      lambda do
        cls2.send :private_constant, :Foo
      end.should raise_error(NameError)
    end
    
    ruby_bug "[ruby-list:48559]", "1.9.3" do
      it "accepts multiple names" do
        mod = Module.new
        mod.const_set :Foo, true
        mod.const_set :Bar, true
        
        mod.send :private_constant, :Foo, :Bar
        
        lambda {mod::Foo}.should raise_error(NameError)
        lambda {mod::Bar}.should raise_error(NameError)
      end
    end
  end
  
  describe "Module#private_constant marked constants" do
    
    it "remain private even when updated" do
      mod = Module.new
      mod.const_set :Foo, true
      mod.send :private_constant, :Foo
      mod.const_set :Foo, false
      
      lambda {mod::Foo}.should raise_error(NameError)
    end
  
    describe "in a module" do
      it "cannot be accessed from outside the module" do
        lambda do
          ModuleSpecs::PrivConstModule::PRIVATE_CONSTANT_MODULE
        end.should raise_error(NameError)
      end
      
      it "cannot be reopened as a module" do
        lambda do
          module ModuleSpecs::PrivConstModule::PRIVATE_CONSTANT_MODULE; end
        end.should raise_error(NameError)
      end

      it "cannot be reopened as a class" do
        lambda do
          class ModuleSpecs::PrivConstModule::PRIVATE_CONSTANT_MODULE; end
        end.should raise_error(NameError)
      end
      
      it "is not defined? with A::B form" do
        defined?(ModuleSpecs::PrivConstModule::PRIVATE_CONSTANT_MODULE).should == nil
      end
      
      it "can be accessed from the module itself" do
        ModuleSpecs::PrivConstModule.private_constant_from_self.should be_true
      end
      
      it "is defined? from the module itself" do
        ModuleSpecs::PrivConstModule.defined_from_self.should == "constant"
      end
      
      it "can be accessed from lexical scope" do
        ModuleSpecs::PrivConstModule::Nested.private_constant_from_scope.should be_true
      end
      
      it "is defined? from lexical scope" do
        ModuleSpecs::PrivConstModule::Nested.defined_from_scope.should == "constant"
      end
      
      it "can be accessed from classes that include the module" do
        ModuleSpecs::PrivConstModuleChild.new.private_constant_from_include.should be_true
      end
      
      it "is defined? from classes that include the module" do
        ModuleSpecs::PrivConstModuleChild.new.defined_from_include.should == "constant"
      end
    end
    
    describe "in a class" do
      it "cannot be accessed from outside the class" do
        lambda do
          ModuleSpecs::PrivConstClass::PRIVATE_CONSTANT_CLASS
        end.should raise_error(NameError)
      end
      
      it "cannot be reopened as a module" do
        lambda do
          module ModuleSpecs::PrivConstClass::PRIVATE_CONSTANT_CLASS; end
        end.should raise_error(NameError)
      end

      it "cannot be reopened as a class" do
        lambda do
          class ModuleSpecs::PrivConstClass::PRIVATE_CONSTANT_CLASS; end
        end.should raise_error(NameError)
      end

      
      it "is not defined? with A::B form" do
        defined?(ModuleSpecs::PrivConstClass::PRIVATE_CONSTANT_CLASS).should == nil
      end
      
      it "can be accessed from the class itself" do
        ModuleSpecs::PrivConstClass.private_constant_from_self.should be_true
      end
      
      it "is defined? from the class itself" do
        ModuleSpecs::PrivConstClass.defined_from_self.should == "constant"
      end
      
      it "can be accessed from lexical scope" do
        ModuleSpecs::PrivConstClass::Nested.private_constant_from_scope.should be_true
      end
      
      it "is defined? from lexical scope" do
        ModuleSpecs::PrivConstClass::Nested.defined_from_scope.should == "constant"
      end
      
      it "can be accessed from subclasses" do
        ModuleSpecs::PrivConstClassChild.new.private_constant_from_subclass.should be_true
      end
      
      it "is defined? from subclasses" do
        ModuleSpecs::PrivConstClassChild.new.defined_from_subclass.should == "constant"
      end
    end
    
    describe "in Object" do
      it "cannot be accessed using ::Const form" do
        lambda do
          ::PRIVATE_CONSTANT_IN_OBJECT
        end.should raise_error(NameError)
      end
      
      it "is not defined? using ::Const form" do
        defined?(::PRIVATE_CONSTANT_IN_OBJECT).should == nil
      end
      
      it "can be accessed through the normal search" do
        PRIVATE_CONSTANT_IN_OBJECT.should == true
      end
      
      it "is defined? through the normal search" do
        defined?(PRIVATE_CONSTANT_IN_OBJECT).should == "constant"
      end
    end
  end
end
