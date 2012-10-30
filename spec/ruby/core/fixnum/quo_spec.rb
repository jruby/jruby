require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9" do
  describe "Fixnum#quo" do
    conflicts_with :Rational do
      it "returns the result of self divided by the given Float as a Float" do
        2.quo(2.5).should eql(0.8)
      end

      it "returns the result of self divided by the given Bignum as a Float" do
        45.quo(bignum_value).should be_close(1.04773789668636e-08, TOLERANCE)
      end

      it "returns the result of self divided by the given Integer as a Float" do
        5.quo(2).should eql(2.5)
      end

      it "does not raise a ZeroDivisionError when the given Integer is 0" do
        0.quo(0).to_s.should == "NaN"
        10.quo(0).to_s.should == "Infinity"
        -10.quo(0).to_s.should == "-Infinity"
      end

      it "does not raise a FloatDomainError when the given Integer is 0 and a Float" do
        0.quo(0.0).to_s.should == "NaN"
        10.quo(0.0).to_s.should == "Infinity"
        -10.quo(0.0).to_s.should == "-Infinity"
      end

      it "raises a TypeError when given a non-Integer" do
        lambda {
          (obj = mock('x')).should_not_receive(:to_int)
          13.quo(obj)
        }.should raise_error(TypeError)
        lambda { 13.quo("10")    }.should raise_error(TypeError)
        lambda { 13.quo(:symbol) }.should raise_error(TypeError)
      end
    end
  end
end
