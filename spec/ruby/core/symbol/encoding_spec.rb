require File.expand_path('../../../spec_helper', __FILE__)

describe "Symbol#encoding for ASCII symbols" do
  it "is US-ASCII" do
    :foo.encoding.name.should == "US-ASCII"
  end

  it "is US-ASCII after converting to string" do
    :foo.to_s.encoding.name.should == "US-ASCII"
  end
end
