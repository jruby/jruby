require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#*" do
  it "returns a new string containing count copies of self" do
    ("cool" * 0).should == ""
    ("cool" * 1).should == "cool"
    ("cool" * 3).should == "coolcoolcool"
  end

  it "tries to convert the given argument to an integer using to_int" do
    ("cool" * 3.1).should == "coolcoolcool"
    ("a" * 3.999).should == "aaa"

    a = mock('4')
    a.should_receive(:to_int).and_return(4)

    ("a" * a).should == "aaaa"
  end

  it "raises an ArgumentError when given integer is negative" do
    lambda { "cool" * -3    }.should raise_error(ArgumentError)
    lambda { "cool" * -3.14 }.should raise_error(ArgumentError)
  end

  it "raises a RangeError when given integer is a Bignum" do
    lambda { "cool" * 999999999999999999999 }.should raise_error(RangeError)
  end

  it "returns subclass instances" do
    (StringSpecs::MyString.new("cool") * 0).should be_kind_of(StringSpecs::MyString)
    (StringSpecs::MyString.new("cool") * 1).should be_kind_of(StringSpecs::MyString)
    (StringSpecs::MyString.new("cool") * 2).should be_kind_of(StringSpecs::MyString)
  end

  it "always taints the result when self is tainted" do
    ["", "OK", StringSpecs::MyString.new(""), StringSpecs::MyString.new("OK")].each do |str|
      str.taint

      [0, 1, 2].each do |arg|
        (str * arg).tainted?.should == true
      end
    end
  end
end
