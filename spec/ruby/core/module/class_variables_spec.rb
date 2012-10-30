require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Module#class_variables" do
  ruby_version_is ""..."1.9" do
    it "returns an Array with the names of class variables of self and self's ancestors" do
      ModuleSpecs::ClassVars::A.class_variables.should include("@@a_cvar")
      ModuleSpecs::ClassVars::M.class_variables.should include("@@m_cvar")
      ModuleSpecs::ClassVars::B.class_variables.should include("@@a_cvar", "@@b_cvar", "@@m_cvar")
    end

    it "returns an Array with names of class variables defined in metaclasses" do
      ModuleSpecs::CVars.class_variables.should include("@@cls", "@@meta")
    end

    it "returns an Array of Strings of class variable names defined in a metaclass" do
      obj = mock("metaclass class variable")
      meta = obj.singleton_class
      meta.send :class_variable_set, :@@var, :cvar_value
      meta.class_variables.should == ["@@var"]
    end

    it "returns an Array with names of class variables defined in included modules" do
      c = Class.new { include ModuleSpecs::MVars }
      c.class_variables.should include("@@mvar")
    end

    it "does not return class variables defined in extended modules" do
      c = Class.new
      c.extend ModuleSpecs::MVars
      c.class_variables.should_not include("@@mvar")
    end
  end

  ruby_version_is "1.9" do
    it "returns an Array with the names of class variables of self" do
      ModuleSpecs::ClassVars::A.class_variables.should include(:@@a_cvar)
      ModuleSpecs::ClassVars::M.class_variables.should include(:@@m_cvar)
    end

    it "returns an Array of Symbols of class variable names defined in a metaclass" do
      obj = mock("metaclass class variable")
      meta = obj.singleton_class
      meta.send :class_variable_set, :@@var, :cvar_value
      meta.class_variables.should == [:@@var]
    end

    it "returns an Array with names of class variables defined in metaclasses" do
      ModuleSpecs::CVars.class_variables.should include(:@@cls, :@@meta)
    end

    it "does not return class variables defined in extended modules" do
      c = Class.new
      c.extend ModuleSpecs::MVars
      c.class_variables.should_not include(:@@mvar)
    end
  end
end
