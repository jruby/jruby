require File.expand_path('../../../spec_helper', __FILE__)

describe "ENV.shift" do

  it "returns a pair and deletes it" do
    ENV.empty?.should == false
    orig = ENV.to_hash
    begin
      pair = ENV.shift
      ENV.has_key?(pair.first).should == false
    ensure
      ENV.replace orig
    end
    ENV.has_key?(pair.first).should == true
  end

  it "returns nil if ENV.empty?" do
    orig = ENV.to_hash
    begin
      ENV.clear
      ENV.shift.should == nil
    ensure
      ENV.replace orig
    end
  end

end
