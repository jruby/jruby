require_relative '../../spec_helper'
require_relative 'fixtures/classes'

describe "String#hash" do
  it "returns a hash based on a string's length and content" do
    "abc".hash.should == "abc".hash
    "abc".hash.should_not == "cba".hash
  end

  it "returns the same hash for byte-identical Strings built by interpolation" do
    interpolated = "abc#{1}def"
    literal = "abc1def"
    interpolated.should eql(literal)
    interpolated.hash.should == literal.hash
    [interpolated, literal].uniq.size.should == 1
  end
end
