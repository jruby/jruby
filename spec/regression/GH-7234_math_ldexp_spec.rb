describe "Math.ldexp" do

  it "returns correct value that closes to the max value of double type" do
    Math.ldexp(0.5122058490966879, 1024).should == 9.207889385574391e+307
    Math.ldexp(0.9999999999999999, 1024).should == 1.7976931348623157e+308
    Math.ldexp(0.99999999999999999, 1024).should == Float::INFINITY
  end
end