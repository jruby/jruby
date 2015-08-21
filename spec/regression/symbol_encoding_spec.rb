require 'rspec'

describe "symbol encoding" do
  it "should be US-ASCII" do
    expect(:foo.encoding.name).to eq("US-ASCII")
  end

  it "should be US-ASCII after converting to string" do
    expect(:foo.to_s.encoding.name).to eq("US-ASCII")
  end
end
