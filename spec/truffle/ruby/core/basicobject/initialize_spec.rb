require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject#initialize" do
    it "is a private instance method" do
      BasicObject.should have_private_instance_method(:initialize)
    end
  end
end
