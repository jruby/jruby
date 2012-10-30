require File.expand_path('../../../spec_helper', __FILE__)

describe "Regexp#source" do
  it "returns the original string of the pattern" do
    /ab+c/ix.source.should == "ab+c"
    /x(.)xz/.source.should == "x(.)xz"
  end
end
