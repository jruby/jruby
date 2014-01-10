require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Float.induced_from" do
    it "returns the passed argument when passed a Float" do
      Float.induced_from(5.5).should eql(5.5)
      Float.induced_from(-5.5).should eql(-5.5)
      Float.induced_from(TOLERANCE).should eql(TOLERANCE)
    end

    it "converts passed Fixnums or Bignums to Floats (using #to_f)" do
      Float.induced_from(5).should eql(5.0)
      Float.induced_from(-5).should eql(-5.0)
      Float.induced_from(0).should eql(0.0)

      Float.induced_from(bignum_value).should eql(bignum_value.to_f)
      Float.induced_from(-bignum_value).should eql(-bignum_value.to_f)
    end

    it "does not try to convert non-Integers to Integers using #to_int" do
      obj = mock("Not converted to Integer")
      obj.should_not_receive(:to_int)
      lambda { Float.induced_from(obj) }.should raise_error(TypeError)
    end

    it "does not try to convert non-Integers to Floats using #to_f" do
      obj = mock("Not converted to Float")
      obj.should_not_receive(:to_f)
      lambda { Float.induced_from(obj) }.should raise_error(TypeError)
    end

    it "raises a TypeError when passed a non-Integer" do
      lambda { Float.induced_from("2") }.should raise_error(TypeError)
      lambda { Float.induced_from(:symbol) }.should raise_error(TypeError)
      lambda { Float.induced_from(Object.new) }.should raise_error(TypeError)
    end
  end
end
