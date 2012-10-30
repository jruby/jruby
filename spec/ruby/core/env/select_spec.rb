require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "1.9" do
  describe "ENV.select!" do
    it "removes environment variables for which the block returns true" do
      ENV["foo"] = "bar"
      ENV.select! { |k, v| k != "foo" }
      ENV["foo"].should == nil
    end

    it "returns self if any changes were made" do
      ENV["foo"] = "bar"
      ENV.select! { |k, v| k != "foo" }.should == ENV
    end

    it "returns nil if no changes were made" do
      ENV.select! { true }.should == nil
    end

    it "returns an Enumerator if called without a block" do
      ENV.select!.should be_an_instance_of(enumerator_class)
    end
  end
end

describe "ENV.select" do
  ruby_version_is ""..."1.9" do
    it "returns an Array of names and value for which block returns true" do
      ENV["foo"] = "bar"
      ENV.select { |k, v| k == "foo" }.should == [["foo", "bar"]]
      ENV.delete "foo"
    end
  end

  ruby_version_is "1.9" do
    it "returns a Hash of names and values for which block return true" do
      ENV["foo"] = "bar"
      ENV.select { |k, v| k == "foo" }.should == {"foo" => "bar"}
      ENV.delete "foo"
    end
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises when no block is given" do
      lambda { ENV.select }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator when no block is given" do
      ENV.select.should be_an_instance_of(enumerator_class)
    end
  end
end
