require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject.ancestors" do
    it "returns only BasicObject" do
      BasicObject.ancestors.should == [BasicObject]
    end
  end
end
