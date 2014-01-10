require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "main#def" do
  after :each do
    Object.send(:remove_method, :foo)
  end

  it "sets the visibility of the given method to private" do
    eval "def foo; end", TOPLEVEL_BINDING
    Object.should have_private_method(:foo)
  end
end

