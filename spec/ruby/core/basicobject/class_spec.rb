require File.expand_path('../../../spec_helper', __FILE__)

describe "BasicObject.class" do
  it "returns Class" do
    BasicObject.class.should equal(Class)
  end
end
