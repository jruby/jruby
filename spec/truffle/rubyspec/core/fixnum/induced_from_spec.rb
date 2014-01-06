require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Fixnum.induced_from with [Float]" do
    it "returns a Fixnum when the passed Float is in Fixnum's range" do
      Fixnum.induced_from(2.5).should eql(2)
      Fixnum.induced_from(-3.14).should eql(-3)
      Fixnum.induced_from(10 - TOLERANCE).should eql(9)
      Fixnum.induced_from(TOLERANCE).should eql(0)
    end

    it "raises a RangeError when the passed Float is out of Fixnum's range" do
      lambda { Fixnum.induced_from((2**64).to_f) }.should raise_error(RangeError)
      lambda { Fixnum.induced_from(-(2**64).to_f) }.should raise_error(RangeError)
    end
  end

  describe "Fixnum.induced_from" do
    it "returns the passed argument when passed a Fixnum" do
      Fixnum.induced_from(3).should eql(3)
      Fixnum.induced_from(-10).should eql(-10)
    end

    it "tries to convert non-Integers to a Integers using #to_int" do
      obj = mock("Converted to Integer")
      obj.should_receive(:to_int).and_return(10)
      Fixnum.induced_from(obj)
    end

    it "raises a TypeError when conversion to Integer returns a Bignum" do
      obj = mock("Not converted to Integer")
      obj.should_receive(:to_int).and_return(bignum_value)
      lambda { Fixnum.induced_from(obj) }.should raise_error(RangeError)
    end
  end
end
