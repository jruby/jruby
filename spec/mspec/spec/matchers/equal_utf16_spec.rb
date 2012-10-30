require 'spec_helper'
require 'mspec/expectations/expectations'
require 'mspec/matchers'

describe EqualUtf16Matcher do
  before :all do
    # this is a neutral way to covert a NULL character to a
    # string representation on 1.8 (\000) and 1.9 (\x00)
    @null = "\0".inspect[1..-2]
  end

  it "when given strings, matches when actual == expected" do
    EqualUtf16Matcher.new("abcd").matches?("abcd").should == true
  end

  it "when given strings, matches when actual == expected, with byte order reversed" do
    EqualUtf16Matcher.new("abcd").matches?("badc").should == true
  end

  it "when given arrays, matches when actual == expected" do
    EqualUtf16Matcher.new(["abcd"]).matches?(["abcd"]).should == true
    EqualUtf16Matcher.new(["abcd", "efgh"]).matches?(["abcd", "efgh"]).should == true
  end

  it "when given arrays, matches when actual == a version of expected with byte order reversed for all strings contained" do
    EqualUtf16Matcher.new(["abcd"]).matches?(["badc"]).should == true
    EqualUtf16Matcher.new(["abcd", "efgh"]).matches?(["badc", "fehg"]).should == true
  end

  it "when given strings, does not match when actual != expected AND != expected with byte order reversed" do
    EqualUtf16Matcher.new("abcd").matches?("").should == false
    EqualUtf16Matcher.new("abcd").matches?(nil).should == false
    EqualUtf16Matcher.new("abcd").matches?("acbd").should == false
  end

  it "when given arrays, does not match when actual is not == expected or == a version of expected with byte order reversed for all strings contained simultaneously" do
    EqualUtf16Matcher.new(["abcd"]).matches?([]).should == false
    EqualUtf16Matcher.new(["abcd"]).matches?(["dcba"]).should == false
    EqualUtf16Matcher.new(["abcd", "efgh"]).matches?(["abcd", "fehg"]).should == false
  end

  it "provides a useful failure message" do
    matcher = EqualUtf16Matcher.new("a\0b\0")
    matcher.matches?("a\0b\0c\0")
    matcher.failure_message.should == [
      "Expected [\"a#{@null}b#{@null}c#{@null}\"]\n",
      "to equal [\"a#{@null}b#{@null}\"]\n or [\"#{@null}a#{@null}b\"]\n"]
  end

  it "provides a useful negative failure message" do
    matcher = EqualUtf16Matcher.new("a\0b\0")
    matcher.matches?("\0a\0b")
    matcher.negative_failure_message.should == [
      "Expected [\"#{@null}a#{@null}b\"]\n",
      "not to equal [\"a#{@null}b#{@null}\"]\n nor [\"#{@null}a#{@null}b\"]\n"]
  end
end
