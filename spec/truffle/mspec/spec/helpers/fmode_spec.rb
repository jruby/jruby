require 'spec_helper'
require 'mspec/guards'
require 'mspec/helpers'

describe Object, "#fmode" do
  it "returns the argument unmodified if :encoding feature is enabled" do
    FeatureGuard.should_receive(:enabled?).with(:encoding).and_return(true)
    fmode("rb:binary:utf-8").should == "rb:binary:utf-8"
  end

  it "returns only the file access mode if :encoding feature is not enabled" do
    FeatureGuard.should_receive(:enabled?).with(:encoding).and_return(false)
    fmode("rb:binary:utf-8").should == "rb"
  end
end
