require File.expand_path('../../../spec_helper', __FILE__)

describe "Fixnum#>> with n >> m" do
  it "returns n shifted right m bits when n > 0, m > 0" do
    (2 >> 1).should == 1
  end

  it "returns n shifted right m bits when n < 0, m > 0" do
    (-2 >> 1).should == -1
  end

  it "returns n shifted left m bits when n > 0, m < 0" do
    (1 >> -1).should == 2
  end

  it "returns n shifted left m bits when n < 0, m < 0" do
    (-1 >> -1).should == -2
  end

  it "returns 0 when n == 0" do
    (0 >> 1).should == 0
  end

  it "returns n when n > 0, m == 0" do
    (1 >> 0).should == 1
  end

  it "returns n when n < 0, m == 0" do
    (-1 >> 0).should == -1
  end

  it "returns 0 when m > 0 and m == p where 2**p > n >= 2**(p-1)" do
    (4 >> 3).should == 0
  end

  fixnum_bits = (Math.log(fixnum_max) / Math.log(2)).to_i

  it "returns 0 when m is outside the available bits and n >= 0" do
    (2 >> (fixnum_bits + 1)).should == 0
  end

  it "returns -1 when m is outside the available bits and n < 0" do
    (-2 >> (fixnum_bits + 1)).should == -1
  end

  not_compliant_on :rubinius do
    it "returns 0 when m is a Bignum" do
      (3 >> bignum_value()).should == 0
    end
  end

  deviates_on :rubinius do
    it "raises a RangeError when m is a Bignum" do
      lambda { 3 >> bignum_value() }.should raise_error(RangeError)
    end
  end

  it "returns a Bignum == fixnum_max() * 2 when fixnum_max() >> -1 and n > 0" do
    result = fixnum_max() >> -1
    result.should be_an_instance_of(Bignum)
    result.should == fixnum_max() * 2
  end

  it "returns a Bignum == fixnum_min() * 2 when fixnum_min() >> -1 and n < 0" do
    result = fixnum_min() >> -1
    result.should be_an_instance_of(Bignum)
    result.should == fixnum_min() * 2
  end

  it "calls #to_int to convert the argument to an Integer" do
    obj = mock("2")
    obj.should_receive(:to_int).and_return(2)

    (8 >> obj).should == 2
  end

  it "raises a TypeError when #to_int does not return an Integer" do
    obj = mock("a string")
    obj.should_receive(:to_int).and_return("asdf")

    lambda { 3 >> obj }.should raise_error(TypeError)
  end

  it "raises a TypeError when passed nil" do
    lambda { 3 >> nil }.should raise_error(TypeError)
  end

  it "raises a TypeError when passed a String" do
    lambda { 3 >> "4" }.should raise_error(TypeError)
  end
end
