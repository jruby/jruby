require 'rspec'

describe "symbol encoding" do
  it "should be US-ASCII" do
    :foo.encoding.name.should == "US-ASCII"
  end

  it "should be US-ASCII after converting to string" do
    :foo.to_s.encoding.name.should == "US-ASCII"
  end
end
