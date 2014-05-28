require File.expand_path('../../../spec_helper', __FILE__)

describe "BasicObject#__send__" do
  it "is a public instance method" do
    BasicObject.should have_public_instance_method(:__send__)
  end
end
