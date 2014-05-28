require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

script_binding = binding

describe "main#def" do
  after :each do
    Object.send(:remove_method, :foo)
  end

  it "sets the visibility of the given method to private for TOPLEVEL_BINDING" do
    eval "def foo; end", TOPLEVEL_BINDING
    Object.should have_private_method(:foo)
  end

  it "sets the visibility of the given method to private for the script binding" do
    eval "def foo; end", script_binding
    Object.should have_private_method(:foo)
  end

  it "sets the visibility of the given method to private when defined in a block" do
    eval "1.times { def foo; end }", script_binding
    Object.should have_private_method(:foo)
  end

end

