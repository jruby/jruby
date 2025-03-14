require 'bigdecimal'

describe "BigDecimal#round" do
  it "returns fixnum if no args are passed" do
    expect(BigDecimal('1').round).to be_a(Integer)
  end
end

describe "BigDecimal#truncate" do
  it "returns fixnum if no args are passed" do
    expect(BigDecimal('1').truncate).to be_a(Integer)
  end
end

describe "BigDecimal#floor" do
  it "returns fixnum if no args are passed" do
    expect(BigDecimal('1').floor).to be_a(Integer)
  end
end

describe "BigDecimal#ceil" do
  it "returns fixnum if no args are passed" do
    expect(BigDecimal('1').ceil).to be_a(Integer)
  end
end
