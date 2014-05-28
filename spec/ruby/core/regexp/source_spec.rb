# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)

describe "Regexp#source" do
  it "returns the original string of the pattern" do
    /ab+c/ix.source.should == "ab+c"
    /x(.)xz/.source.should == "x(.)xz"
  end

  it "has US-ASCII encoding when created from an ASCII-only \\u{} literal" do
    re = /[\u{20}-\u{7E}]/
    re.source.encoding.should equal(Encoding::US_ASCII)
  end

  it "has UTF-8 encoding when created from a non-ASCII-only \\u{} literal" do
    re = /[\u{20}-\u{7EE}]/
    re.source.encoding.should equal(Encoding::UTF_8)
  end
end
