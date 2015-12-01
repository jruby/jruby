require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is "2.1" do
  describe "Binding#local_variable_get" do
    it "reads local variables captured in the binding" do
      a = 42
      bind = binding
      bind.local_variable_get(:a).should == 42
    end

    it "raises a NameError for missing variables" do
      bind = BindingSpecs::Demo.new(1).get_empty_binding

      lambda {
        bind.local_variable_get(:no_such_variable)
      }.should raise_error(NameError)
    end

    it "reads variables added later to the binding" do
      bind = BindingSpecs::Demo.new(1).get_empty_binding

      lambda {
        bind.local_variable_get(:a)
      }.should raise_error(NameError)

      bind.local_variable_set(:a, 42)

      bind.local_variable_get(:a).should == 42
    end
  end
end
