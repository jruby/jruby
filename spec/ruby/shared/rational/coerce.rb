require File.expand_path('../../../spec_helper', __FILE__)
require 'rational'

describe :rational_coerce, :shared => true do
  it "returns the passed argument, self as Float, when given a Float" do
    result = Rational(3, 4).coerce(1.0)
    result.should == [1.0, 0.75]
    result.first.is_a?(Float).should be_true
    result.last.is_a?(Float).should be_true
  end

  it "returns the passed argument, self as Rational, when given an Integer" do
    result = Rational(3, 4).coerce(10)
    result.should == [Rational(10, 1), Rational(3, 4)]
    result.first.is_a?(Rational).should be_true
    result.last.is_a?(Rational).should be_true
  end

  # This failed on 1.9 as reported in bug #1636. It was fixed in revision 23718
  it "returns [argument, self] when given a Rational" do
    Rational(3, 7).coerce(Rational(9, 2)).should == [Rational(9, 2), Rational(3, 7)]
  end

  ruby_version_is ""..."1.9" do
    it "tries to convert the passed argument into a Float (using #to_f)" do
      obj = mock("something")
      obj.should_receive(:to_f).and_return(1.1)
      Rational(3, 4).coerce(obj)
    end

    it "returns the passed argument, self converted to Float, when given object with #to_f" do
      obj = mock("something")
      obj.should_receive(:to_f).and_return(1.1)

      result = Rational(3, 4).coerce(obj)
      result.should == [1.1, 0.75]
      result.first.is_a?(Float).should be_true
      result.last.is_a?(Float).should be_true
    end
  end
end
