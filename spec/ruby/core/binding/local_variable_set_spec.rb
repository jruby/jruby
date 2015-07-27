require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is "2.1" do
  describe "Binding#local_variable_set" do
    it "adds nonexistent variables to the binding's eval scope" do
      obj = BindingSpecs::Demo.new(1)
      bind = obj.get_empty_binding
      bind.eval('local_variables').should == []
      bind.local_variable_set :foo, 1
      bind.eval('local_variables').should == [:foo]
    end
  end
end
