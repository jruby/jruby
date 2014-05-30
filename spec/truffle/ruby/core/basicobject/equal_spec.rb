require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject#equal?" do
    it "is a public instance method" do
      BasicObject.should have_public_instance_method(:equal?)
    end

    it "returns true if other is identical to self" do
      obj = BasicObject.new
      obj.equal?(obj).should be_true
    end

    it "returns false if other is not identical to self" do
      a = BasicObject.new
      b = BasicObject.new
      a.equal?(b).should be_false
    end
  end
end
