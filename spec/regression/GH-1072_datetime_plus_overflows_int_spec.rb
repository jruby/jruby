describe "DateTime plus a numeric value larger than int range" do
  it "correctly uses long logic to do the addition" do
    date = DateTime.new(2011,1,1)
    (date + Rational(365,1)).should == DateTime.new(2012,1,1)
  end
end if RUBY_VERSION >= "1.9"