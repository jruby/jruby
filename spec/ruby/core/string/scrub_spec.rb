# -*- encoding: utf-8 -*-
require File.expand_path("../../../spec_helper", __FILE__)

describe "String#scrub with a default replacement" do
  it "returns self for valid strings" do
    input = "foo"

    input.scrub.should == input
  end

  it "replaces invalid byte sequences" do
    "abc\u3042\x81".scrub.should == "abc\u3042\uFFFD"
  end
end

describe "String#scrub with a custom replacement" do
  it "returns self for valid strings" do
    input = "foo"

    input.scrub("*").should == input
  end

  it "replaces invalid byte sequences" do
    "abc\u3042\x81".scrub("*").should == "abc\u3042*"
  end

  it "replaces groups of sequences together with a single replacement" do
    "\xE3\x80".scrub("*").should == "*"
  end

  it "raises ArgumentError for replacements with an invalid encoding" do
    block = lambda { "foo\x81".scrub("\xE4") }

    block.should raise_error(ArgumentError)
  end

  it "raises TypeError when a non String replacement is given" do
    block = lambda { "foo\x81".scrub(1) }

    block.should raise_error(TypeError)
  end
end

describe "String#scrub with a block" do
  it "returns self for valid strings" do
    input = "foo"

    input.scrub { |b| "*" }.should == input
  end

  it "replaces invalid byte sequences" do
    replaced = "abc\u3042\xE3\x80".scrub { |b| "<#{b.unpack("H*")[0]}>" }

    replaced.should == "abc\u3042<e380>"
  end
end
