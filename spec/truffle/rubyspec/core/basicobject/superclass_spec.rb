require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject.superclass" do
    it "returns nil" do
      BasicObject.superclass.should be_nil
    end
  end
end
