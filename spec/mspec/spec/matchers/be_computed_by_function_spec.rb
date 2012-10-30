require 'spec_helper'
require 'mspec/matchers'

describe BeComputedByFunctionMatcher do
  it "matches when all entries in the Array compute" do
    array = [ ["%2d",  65, "65"],
              ["%04d", 90, "0090"] ]
    BeComputedByFunctionMatcher.new(:sprintf).matches?(array).should be_true
  end

  it "matches when all entries in the Array with arguments compute" do
    array = [ ["%2d",  "65"],
              ["%04d", "0065"] ]
    BeComputedByFunctionMatcher.new(:sprintf, 65).matches?(array).should be_true
  end

  it "does not match when any entry in the Array does not compute" do
    array = [ ["%2d",  65, "65"],
              ["%04d", 90, "00090"] ]
    BeComputedByFunctionMatcher.new(:sprintf).matches?(array).should be_false
  end

  it "does not match when any entry in the Array with arguments does not compute" do
    array = [ ["%2d",  "65"],
              ["%04d", "0065"] ]
    BeComputedByFunctionMatcher.new(:sprintf, 91).matches?(array).should be_false
  end

  it "provides a useful failure message" do
    array = [ ["%2d",  90, "65"],
              ["%04d", 90, "00090"] ]
    matcher = BeComputedByFunctionMatcher.new(:sprintf)
    matcher.matches?(array)
    matcher.failure_message.should == ["Expected \"65\"", "to be computed by sprintf(\"%2d\", 90)"]
  end
end
