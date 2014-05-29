require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9.3" do
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
end
