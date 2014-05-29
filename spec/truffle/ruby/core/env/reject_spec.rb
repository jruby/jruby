require File.expand_path('../../../spec_helper', __FILE__)

describe "ENV.reject!" do
  it "rejects entries based on key" do
    ENV["foo"] = "bar"
    ENV.reject! { |k, v| k == "foo" }
    ENV["foo"].should == nil
  end

  it "rejects entries based on value" do
    ENV["foo"] = "bar"
    ENV.reject! { |k, v| v == "bar" }
    ENV["foo"].should == nil
  end

  it "returns itself or nil" do
    ENV.reject! { false }.should == nil
    ENV["foo"] = "bar"
    ENV.reject! { |k, v| k == "foo" }.should == ENV
    ENV["foo"].should == nil
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises on no block given" do
      lambda { ENV.reject! }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator if called without a block" do
      ENV.reject!.should be_an_instance_of(enumerator_class)
    end
  end

  it "doesn't raise if empty" do
    orig = ENV.to_hash
    begin
      ENV.clear
      lambda { ENV.reject! }.should_not raise_error(LocalJumpError)
    ensure
      ENV.replace orig
    end
  end
end

describe "ENV.reject" do
  it "rejects entries based on key" do
    ENV["foo"] = "bar"
    e = ENV.reject { |k, v| k == "foo" }
    e["foo"].should == nil
    ENV["foo"].should == "bar"
    ENV["foo"] = nil
  end

  it "rejects entries based on value" do
    ENV["foo"] = "bar"
    e = ENV.reject { |k, v| v == "bar" }
    e["foo"].should == nil
    ENV["foo"].should == "bar"
    ENV["foo"] = nil
  end

  it "returns a Hash" do
    ENV.reject { false }.should be_kind_of(Hash)
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises on no block given" do
      lambda { ENV.reject }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator if called without a block" do
      ENV.reject.should be_an_instance_of(enumerator_class)
    end
  end

  it "doesn't raise if empty" do
    orig = ENV.to_hash
    begin
      ENV.clear
      lambda { ENV.reject }.should_not raise_error(LocalJumpError)
    ensure
      ENV.replace orig
    end
  end
end
