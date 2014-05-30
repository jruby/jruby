require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject#==" do
    it "is a public instance method" do
      BasicObject.should have_public_instance_method(:==)
    end

    it "returns true if other is identical to self" do
      a = BasicObject.new
      (a == a).should be_true
    end

    it "returns false if other is a BasicObject not identical to self" do
      a = BasicObject.new
      b = BasicObject.new
      (a == b).should be_false
    end

    it "returns false if other is an Object" do
      a = BasicObject.new
      b = Object.new
      (a == b).should be_false
    end
  end
end
