require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject.class" do
    it "returns Class" do
      BasicObject.class.should equal(Class)
    end
  end
end
