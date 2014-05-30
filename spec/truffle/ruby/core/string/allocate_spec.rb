require File.expand_path('../../../spec_helper', __FILE__)

describe "String.allocate" do
  it "returns an instance of String" do
    str = String.allocate
    str.should be_kind_of(String)
  end

  it "returns a fully-formed String" do
    str = String.allocate
    str.size.should == 0
    str << "more"
    str.should == "more"
  end

  ruby_version_is "1.9" do
    it "returns a binary String" do
      String.new.encoding.should == Encoding::BINARY
    end
  end
end
