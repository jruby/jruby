# encoding: utf-8

describe "JRUBY-6860: String#slice" do
  it "checks range properly when given begin or length outside actual" do
    expect('å'.slice(0,16)).to eq("å")
    expect('å'.slice(0,17)).to eq("å")
    expect('å'.slice(1,16)).to eq("")
    expect('å'.slice(1,17)).to eq("")

    expect('1234567890å'.slice(0,17)).to eq("1234567890å")
    expect('1234567890å'.slice(1,17)).to eq("234567890å")
    expect('1234567890å'.slice(9,17)).to eq("0å")
  end
end
