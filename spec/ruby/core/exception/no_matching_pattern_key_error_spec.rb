require_relative '../../spec_helper'

describe "NoMatchingPatternKeyError#dup" do
  it "copies the matchee and key" do
    error = NoMatchingPatternKeyError.new("pattern", matchee: :matchee, key: :key).dup
    error.matchee.should == :matchee
    error.key.should == :key
  end
end
