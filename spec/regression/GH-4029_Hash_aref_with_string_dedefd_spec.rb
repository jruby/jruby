class GH4029Hash < Hash
  undef :[]
end

describe "Hash" do
  it "will raise a NoMethodError and not crash" do
    expect { GH4029Hash.new["o"] }.to raise_error(NoMethodError)
  end
end
