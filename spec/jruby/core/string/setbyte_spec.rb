require 'rspec'

describe "String#setbyte" do
  it "forces the string to unshare, and does not modify other sharers" do
    s = "test"
    t = String.new(s) # depends on this sharing the backing store
    t.setbyte(0, "r".ord)
    expect(t).to eq(eval('"rest"')) # eval these, so compiler/ast does not reuse ByteList
    expect(s).to eq(eval('"test"'))
  end
end
