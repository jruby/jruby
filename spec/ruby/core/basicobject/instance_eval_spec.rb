require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "BasicObject#instance_eval" do
    it "is a public instance method" do
      BasicObject.should have_public_instance_method(:instance_eval)
    end

    it "sets self to the receiver in the context of the passed block" do
      a = BasicObject.new
      a.instance_eval { self }.equal?(a).should be_true
    end

    it "evaluates strings" do
      a = BasicObject.new
      a.instance_eval('self').equal?(a).should be_true
    end
  end
end
