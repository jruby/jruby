require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject#__send__" do
    it "is a public instance method" do
      BasicObject.should have_public_instance_method(:__send__)
    end
  end
end
