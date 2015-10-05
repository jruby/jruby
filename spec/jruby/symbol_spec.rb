describe "#inspect" do
  it "returns :$ruby" do
    expect(:$ruby.inspect).to eq(":$ruby")
  end
  
  it "returns :$ruby" do
    expect(:$_.inspect).to eq(":$_")
  end
end

describe "#inject" do
  context "with &:+ symbol" do
     it "returns 15" do
       expect([1,2,3,4,5].inject(&:+)).to eq(15)
     end
   end
end
