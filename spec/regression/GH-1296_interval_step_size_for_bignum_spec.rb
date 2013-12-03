describe "Numeric#step.size" do
  it "does not result in a ClassCastException when passed a bignum" do
    5.step(2**64).size.should == 18446744073709551612
  end
end