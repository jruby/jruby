require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject#instance_exec" do
    it "is a public instance method" do
      BasicObject.should have_public_instance_method(:instance_exec)
    end

    it "sets self to the receiver in the context of the passed block" do
      a = BasicObject.new
      a.instance_exec { self }.equal?(a).should be_true
    end

    it "passes arguments to the block" do
      a = BasicObject.new
      a.instance_exec(1) { |b| b }.should equal(1)
    end
  end
end
