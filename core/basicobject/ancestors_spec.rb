require File.expand_path('../../../spec_helper', __FILE__)

describe "BasicObject.ancestors" do
  it "returns only BasicObject" do
    BasicObject.ancestors.should == [BasicObject]
  end
end
