require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject#!" do
    it "is a public instance method" do
      BasicObject.should have_public_instance_method(:'!')
    end

    it "returns false" do
      (!BasicObject.new).should be_false
    end
  end
end
