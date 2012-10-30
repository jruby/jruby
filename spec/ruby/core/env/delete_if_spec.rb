require File.expand_path('../../../spec_helper', __FILE__)

describe "ENV.delete_if" do

  it "deletes pairs if the block returns true" do
    ENV["foo"] = "bar"
    ENV.delete_if { |k, v| k == "foo" }
    ENV["foo"].should == nil
  end

  it "returns ENV even if nothing deleted" do
    ENV.delete_if { false }.should_not == nil
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises LocalJumpError if no block given" do
      lambda { ENV.delete_if }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator if no block given" do
      ENV.delete_if.should be_an_instance_of(enumerator_class)
    end

    it "deletes pairs through enumerator" do
      ENV["foo"] = "bar"
      enum = ENV.delete_if
      enum.each { |k, v| k == "foo" }
      ENV["foo"].should == nil
    end
  end

end
