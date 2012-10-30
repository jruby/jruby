require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Math.hypot" do
  it "returns a float" do
    Math.hypot(3, 4).should be_kind_of(Float)
  end

  it "returns the length of the hypotenuse of a right triangle with legs given by the arguments" do
    Math.hypot(0, 0).should be_close(0.0, TOLERANCE)
    Math.hypot(2, 10).should be_close( 10.1980390271856, TOLERANCE)
    Math.hypot(5000, 5000).should be_close(7071.06781186548, TOLERANCE)
    Math.hypot(0.0001, 0.0002).should be_close(0.000223606797749979, TOLERANCE)
    Math.hypot(-2, -10).should be_close(10.1980390271856, TOLERANCE)
    Math.hypot(2, 10).should be_close(10.1980390271856, TOLERANCE)
  end

  ruby_version_is ""..."1.9" do
    it "raises an ArgumentError if the argument cannot be coerced with Float()" do
      lambda { Math.hypot("test", "this") }.should raise_error(ArgumentError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a TypeError if the argument cannot be coerced with Float()" do
      lambda { Math.hypot("test", "this") }.should raise_error(TypeError)
    end
  end

  it "raises a ArgumentError if the argument is nil" do
    lambda { Math.hypot(nil) }.should raise_error(ArgumentError)
  end

  it "accepts any argument that can be coerced with Float()" do
    Math.hypot(MathSpecs::Float.new, MathSpecs::Float.new).should be_close(1.4142135623731, TOLERANCE)
  end
end

describe "Math#hypot" do
  it "is accessible as a private instance method" do
    IncludesMath.new.send(:hypot, 2, 3.1415).should be_close(3.72411361937307, TOLERANCE)
  end
end
