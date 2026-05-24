# GH-1072
describe "DateTime plus a numeric value larger than int range" do
  it "correctly uses long logic to do the addition" do
    date = DateTime.new(2011,1,1)
    expect(date + Rational(365,1)).to eq(DateTime.new(2012,1,1))
  end
end
