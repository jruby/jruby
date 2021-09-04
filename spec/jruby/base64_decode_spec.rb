require "rspec"
require "base64"

describe "Base64#decode64" do
  it "return the correct result when missing paddings" do
    expect(Base64.decode64("YQ")).to eq "a"
    expect(Base64.decode64("YWI")).to eq "ab"
    expect(Base64.decode64("YWJj")).to eq "abc"
  end
end

describe "Base64#strict_decode64" do
  it "raise ArgumentError when missing paddings" do
    expect{ Base64.strict_decode64("YQ") }.to raise_error(ArgumentError)
  end
end
