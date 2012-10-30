require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Kernel#to_a" do
    it "is defined on Kernel" do
      Kernel.should have_instance_method('to_a')
    end
  end

  describe "Kernel#to_a when the receiver is an Array" do
    it "returns self" do
      array = [1, 2]
      array.to_a.should equal(array)
    end
  end

  describe "Kernel#to_a when the receiver is not an Array" do
    it "returns an Array containing self" do
      object = "I am not an array"
      object.to_a.should == [object]
    end
  end
end
