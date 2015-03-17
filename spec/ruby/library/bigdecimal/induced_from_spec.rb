require File.expand_path('../../../spec_helper', __FILE__)
require 'bigdecimal'

ruby_version_is "" ... "1.9" do
  describe "BigDecimal.induced_from" do
    it "returns the passed argument when passed a BigDecimal" do
      BigDecimal.induced_from(BigDecimal("5")).should == BigDecimal("5")
      BigDecimal.induced_from(BigDecimal("-5")).should == BigDecimal("-5")
      BigDecimal.induced_from(BigDecimal("Infinity")).should == BigDecimal("Infinity")
    end

    it "converts passed Fixnums to BigDecimal" do
      BigDecimal.induced_from(5).should == BigDecimal("5")
      BigDecimal.induced_from(-5).should == BigDecimal("-5")
      BigDecimal.induced_from(0).should == BigDecimal("0")
    end

    it "converts passed Bignums to BigDecimal" do
      BigDecimal.induced_from(bignum_value).should == BigDecimal(bignum_value.to_s)
      BigDecimal.induced_from(-bignum_value).should == BigDecimal((-bignum_value).to_s)
    end

    it "does not try to convert non-Integers to Integer using #to_i" do
      obj = mock("Not converted to Integer")
      obj.should_not_receive(:to_i)
      lambda { BigDecimal.induced_from(obj) }.should raise_error(TypeError)
    end

    it "raises a TypeError when passed a non-Integer" do
      lambda { BigDecimal.induced_from(2.0) }.should raise_error(TypeError)
      lambda { BigDecimal.induced_from("2") }.should raise_error(TypeError)
      lambda { BigDecimal.induced_from(:symbol) }.should raise_error(TypeError)
      lambda { BigDecimal.induced_from(nil) }.should raise_error(TypeError)
      lambda { BigDecimal.induced_from(Object.new) }.should raise_error(TypeError)
    end
  end
end
