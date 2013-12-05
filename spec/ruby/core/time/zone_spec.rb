require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/methods', __FILE__)

describe "Time#zone" do
  it "returns the time zone used for time" do
    # Testing with Asia/Kuwait here because it doesn't have DST.
    with_timezone("Asia/Kuwait") do
      Time.now.zone.should == "AST"
    end
  end

  ruby_version_is "1.9" do
    it "returns nil for a Time with a fixed offset" do
      Time.new(2001, 1, 1, 0, 0, 0, "+05:00").zone.should == nil
    end
  end

  it "returns UTC when called on a UTC time" do
    Time.now.utc.zone.should == "UTC"
  end
end
