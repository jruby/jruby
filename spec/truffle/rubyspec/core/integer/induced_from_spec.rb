require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Integer.induced_from with [Float]" do
    it "returns a Fixnum when the passed Float is in Fixnum's range" do
      Integer.induced_from(2.5).should eql(2)
      Integer.induced_from(-3.14).should eql(-3)
      Integer.induced_from(10 - TOLERANCE).should eql(9)
      Integer.induced_from(TOLERANCE).should eql(0)
    end

    it "returns a Bignum when the passed Float is out of Fixnum's range" do
      Integer.induced_from(bignum_value.to_f).should eql(bignum_value)
      Integer.induced_from(-bignum_value.to_f).should eql(-bignum_value)
    end
  end

  describe "Integer.induced_from" do
    it "returns the passed argument when passed a Bignum or Fixnum" do
      Integer.induced_from(1).should eql(1)
      Integer.induced_from(-10).should eql(-10)
      Integer.induced_from(bignum_value).should eql(bignum_value)
    end

    it "does not try to convert non-Integers to Integers using #to_int" do
      obj = mock("Not converted to Integer")
      obj.should_not_receive(:to_int)
      lambda { Integer.induced_from(obj) }.should raise_error(TypeError)
    end

    it "does not try to convert non-Integers to Integers using #to_i" do
      obj = mock("Not converted to Integer")
      obj.should_not_receive(:to_i)
      lambda { Integer.induced_from(obj) }.should raise_error(TypeError)
    end

    it "raises a TypeError when passed a non-Integer" do
      lambda { Integer.induced_from("2") }.should raise_error(TypeError)
      lambda { Integer.induced_from(:symbol) }.should raise_error(TypeError)
      lambda { Integer.induced_from(Object.new) }.should raise_error(TypeError)
    end
  end
end
