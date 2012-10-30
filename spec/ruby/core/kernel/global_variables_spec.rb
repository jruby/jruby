require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Kernel.global_variables" do
  it "is a private method" do
    Kernel.should have_private_instance_method(:global_variables)
  end

  ruby_version_is ""..."1.9" do
    it "finds subset starting with std" do
      global_variables.grep(/std/).should include("$stderr", "$stdin", "$stdout")
      a = global_variables.size
      global_variables.include?("$foolish_global_var").should == false
      eval("$foolish_global_var = 1")
      global_variables.size.should == a+1
      global_variables.should include("$foolish_global_var")
    end
  end

  ruby_version_is "1.9" do
    it "finds subset starting with std" do
      global_variables.grep(/std/).should include(:$stderr, :$stdin, :$stdout)
      a = global_variables.size
      global_variables.include?(:$foolish_global_var).should == false
      eval("$foolish_global_var = 1")
      global_variables.size.should == a+1
      global_variables.should include(:$foolish_global_var)
    end
  end
end

describe "Kernel#global_variables" do
  it "needs to be reviewed for spec completeness"
end
