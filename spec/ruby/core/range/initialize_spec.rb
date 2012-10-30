require File.expand_path('../../../spec_helper', __FILE__)

describe "Range#initialize" do
  it "is private" do
    Range.should have_private_instance_method("initialize")
  end

  it "raises an ArgumentError if passed without or with only an argument" do
    lambda { (1..3).__send__(:initialize) }.
      should raise_error(ArgumentError)
    lambda { (1..3).__send__(:initialize, 1) }.
      should raise_error(ArgumentError)
  end

  it "raises a NameError if passed with two or three arguments" do
    lambda { (1..3).__send__(:initialize, 1, 3) }.
      should raise_error(NameError)
    lambda { (1..3).__send__(:initialize, 1, 3, 5) }.
      should raise_error(NameError)
  end

  it "raises an ArgumentError if passed with four or more arguments" do
    lambda { (1..3).__send__(:initialize, 1, 3, 5, 7) }.
      should raise_error(ArgumentError)
    lambda { (1..3).__send__(:initialize, 1, 3, 5, 7, 9) }.
      should raise_error(ArgumentError)
  end
end
