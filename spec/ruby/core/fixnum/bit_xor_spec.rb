require File.expand_path('../../../spec_helper', __FILE__)

describe "Fixnum#^" do
  it "returns self bitwise EXCLUSIVE OR other" do
    (3 ^ 5).should == 6
    (-2 ^ -255).should == 255
    (5 ^ bignum_value + 0xffff_ffff).should == 0x8000_0000_ffff_fffa
  end

  it "returns self bitwise EXCLUSIVE OR a Bignum" do
    (-1 ^ 2**64).should == -18446744073709551617
  end

  ruby_version_is ""..."1.9" do
    ruby_bug "#", "1.8.6" do
      it "doesn't raise an error if passed a Float out of Fixnum range" do
        lambda { 1 ^ bignum_value(10000).to_f }.should_not raise_error()
        lambda { 1 ^ -bignum_value(10000).to_f }.should_not raise_error()
      end
    end

    ruby_bug "#", "1.8.6" do
      it "coerces arguments correctly even if it is a Bignum" do
        obj = mock("fixnum bit xor large number")
        obj.should_receive(:to_int).and_return(8000_0000_0000_0000_0000)
        (3 ^ obj).should == 80000000000000000003
      end
    end

    it "converts a Float to an Integer" do
      (5 ^ 4.3).should == 1
      (-7 ^ 15.2).should == -10
    end
  end

  ruby_version_is "1.9" do
    it "raises a TypeError when passed a Float" do
      lambda { (3 ^ 3.4) }.should raise_error(TypeError)
    end
  end

  ruby_version_is ""..."1.9.4" do
    it "calls #to_int to convert an object to an Integer" do
      obj = mock("fixnum bit xor")
      obj.should_receive(:to_int).and_return(4)

      (3 ^ obj).should == 7
    end

    it "raises a TypeError if #to_int does not return an Integer" do
      obj = mock("fixnum bit xor")
      obj.should_receive(:to_int).and_return("1")

      lambda { 3 ^ obj }.should raise_error(TypeError)
    end

    it "raises a TypeError if the object does not respond to #to_int" do
      obj = mock("fixnum bit xor")
      lambda { 3 ^ obj }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9.4" do
    it "raises a TypeError and does not call #to_int when defined on an object" do
      obj = mock("fixnum bit xor")
      obj.should_not_receive(:to_int)

      lambda { 3 ^ obj }.should raise_error(TypeError)
    end
  end
end
