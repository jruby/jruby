require File.expand_path('../../../spec_helper', __FILE__)

describe "BasicObject.superclass" do
  it "returns nil" do
    BasicObject.superclass.should be_nil
  end
end
