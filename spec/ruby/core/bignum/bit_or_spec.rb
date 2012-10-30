require File.expand_path('../../../spec_helper', __FILE__)

describe "Bignum#|" do
  before(:each) do
    @bignum = bignum_value(11)
  end

  it "returns self bitwise OR other" do
    (@bignum | 2).should == 9223372036854775819
    (@bignum | 9).should == 9223372036854775819
    (@bignum | bignum_value).should == 9223372036854775819
  end

  it "returns self bitwise OR other when one operand is negative" do
    (@bignum | -0x40000000000000000).should == -64563604257983430645
    (@bignum | -@bignum).should == -1
    (@bignum | -0x8000000000000000).should == -9223372036854775797
  end

  it "returns self bitwise OR other when both operands are negative" do
    (-@bignum | -0x4000000000000005).should == -1
    (-@bignum | -@bignum).should == -9223372036854775819
    (-@bignum | -0x4000000000000000).should == -11
  end

  ruby_version_is ""..."1.9" do
    it "coerces Float arguments to Integers" do
      (bignum_value | bignum_value(0xffff).to_f).should == 9223372036854841344
      (@bignum | 9.9).should == 9223372036854775819
    end
  end

  ruby_version_is "1.9" do
    it "raises a TypeError when passed a Float" do
      lambda {
        bignum_value | bignum_value(0xffff).to_f
      }.should raise_error(TypeError)
      lambda { @bignum | 9.9 }.should raise_error(TypeError)
    end
  end

  ruby_version_is ""..."1.9.4" do
    it "calls #to_int to convert an object to an Integer" do
      obj = mock("bignum bit or")
      obj.should_receive(:to_int).and_return(3)

      (@bignum | obj).should == 9223372036854775819
    end

    it "raises a TypeError if #to_int does not return an Integer" do
      obj = mock("bignum bit or")
      obj.should_receive(:to_int).and_return("3")

      lambda { @bignum | obj }.should raise_error(TypeError)
    end

    it "raises a TypeError if the object does not respond to #to_int" do
      obj = mock("bignum bit or")

      lambda { @bignum | obj }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9.4" do
    it "raises a TypeError and does not call #to_int when defined on an object" do
      obj = mock("bignum bit or")
      obj.should_not_receive(:to_int)

      lambda { @bignum | obj }.should raise_error(TypeError)
    end
  end
end
