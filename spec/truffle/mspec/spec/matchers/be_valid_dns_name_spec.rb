require 'spec_helper'
require 'mspec/expectations/expectations'
require 'mspec/matchers'

describe BeValidDNSName do
  it "matches when actual is 'localhost'" do
    BeValidDNSName.new.matches?("localhost").should be_true
  end

  it "matches when actual is 'localhost.localdomain'" do
    BeValidDNSName.new.matches?("localhost.localdomain").should be_true
  end

  it "matches when actual is hyphenated" do
    BeValidDNSName.new.matches?("local-host").should be_true
  end

  it "matches when actual is 'a.b.c'" do
    BeValidDNSName.new.matches?("a.b.c").should be_true
  end

  it "matches when actual has a trailing '.'" do
    BeValidDNSName.new.matches?("a.com.").should be_true
  end

  it "does not match when actual is not a valid dns name" do
    BeValidDNSName.new.matches?(".").should be_false
  end

  it "does not match when actual contains a hyphen at the beginning" do
    BeValidDNSName.new.matches?("-localhost").should be_false
  end

  it "does not match when actual contains a hyphen at the end" do
    BeValidDNSName.new.matches?("localhost-").should be_false
  end

  it "provides a failure message" do
    matcher = BeValidDNSName.new
    matcher.matches?(".")
    matcher.failure_message.should == ["Expected '.'", "to be a valid DNS name"]
  end

  it "provides a negative failure message" do
    matcher = BeValidDNSName.new
    matcher.matches?("localhost")
    matcher.negative_failure_message.should ==
      ["Expected 'localhost'", "not to be a valid DNS name"]
  end
end
