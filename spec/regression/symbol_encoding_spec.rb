require 'rspec'

describe "symbol encoding" do
  it "should be US-ASCII" do
    expect(:foo.encoding.name).to eq("US-ASCII")
  end

  it "should be US-ASCII after converting to string" do
    expect(:foo.to_s.encoding.name).to eq("US-ASCII")
  end

  it "symbol with accents should preserve accents when converted to string" do
    expect(:Renè.to_s).to eq("Renè")
  end

  it "symbol with accents should inspect to appropriate string" do
    expect(:Renè.inspect).to eq(":Renè")
  end

  it "symbol of lambda character should convert to string" do
    expect(:λ.to_s).to eq("λ")
  end

  it "symbol of lambda character should inspect properly" do
    expect(:λ.inspect).to eq(":λ")
  end
  
end
