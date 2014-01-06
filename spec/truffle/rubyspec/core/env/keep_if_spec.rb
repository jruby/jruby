require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "ENV.keep_if" do
    it "deletes pairs if the block returns false" do
      ENV["foo"] = "bar"
      ENV.keep_if { |k, v| k != "foo" }
      ENV["foo"].should == nil
    end

    it "returns ENV even if nothing deleted" do
      ENV.keep_if { true }.should_not == nil
    end

    it "returns an Enumerator if no block given" do
      ENV.keep_if.should be_an_instance_of(enumerator_class)
    end

    it "deletes pairs through enumerator" do
      ENV["foo"] = "bar"
      enum = ENV.keep_if
      enum.each { |k, v| k != "foo" }
      ENV["foo"].should == nil
    end
  end
end
