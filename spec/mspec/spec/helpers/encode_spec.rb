require 'spec_helper'
require 'mspec/guards'
require 'mspec/helpers'

describe Object, "#encode" do
  it "raises an ArgumentError if the str parameter is not a String" do
    lambda { encode(Object.new, "utf-8") }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError if the encoding parameter is not a String" do
    lambda { encode("some str", Object.new) }.should raise_error(ArgumentError)
  end

  it "calls #force_encoding if the :encoding feature is enabled" do
    FeatureGuard.should_receive(:enabled?).with(:encoding).and_return(true)
    str = "some text"
    str.should_receive(:force_encoding).with("utf-8")
    encode(str, "utf-8")
  end

  it "does not call #force_encoding if the :encoding feature is not enabled" do
    FeatureGuard.should_receive(:enabled?).with(:encoding).and_return(false)
    str = "some text"
    str.should_not_receive(:force_encoding)
    encode(str, "utf-8")
  end
end
