require File.expand_path('../../../spec_helper', __FILE__)

describe "BasicObject#initialize" do
  it "is a private instance method" do
    BasicObject.should have_private_instance_method(:initialize)
  end
end
