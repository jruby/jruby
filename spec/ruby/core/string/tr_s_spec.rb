require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#tr_s" do
  it "returns a string processed according to tr with newly duplicate characters removed" do
    "hello".tr_s('l', 'r').should == "hero"
    "hello".tr_s('el', '*').should == "h*o"
    "hello".tr_s('el', 'hx').should == "hhxo"
    "hello".tr_s('o', '.').should == "hell."
  end

  it "accepts c1-c2 notation to denote ranges of characters" do
    "hello".tr_s('a-y', 'b-z').should == "ifmp"
    "123456789".tr_s("2-5", "abcdefg").should == "1abcd6789"
    "hello ^--^".tr_s("e-", "__").should == "h_llo ^_^"
    "hello ^--^".tr_s("---", "_").should == "hello ^_^"
  end

  it "pads to_str with its last char if it is shorter than from_string" do
    "this".tr_s("this", "x").should == "x"
  end

  it "translates chars not in from_string when it starts with a ^" do
    "hello".tr_s('^aeiou', '*').should == "*e*o"
    "123456789".tr_s("^345", "abc").should == "c345c"
    "abcdefghijk".tr_s("^d-g", "9131").should == "1defg1"

    "hello ^_^".tr_s("a-e^e", ".").should == "h.llo ._."
    "hello ^_^".tr_s("^^", ".").should == ".^.^"
    "hello ^_^".tr_s("^", "x").should == "hello x_x"
    "hello ^-^".tr_s("^-^", "x").should == "x^-^"
    "hello ^-^".tr_s("^^-^", "x").should == "x^x^"
    "hello ^-^".tr_s("^---", "x").should == "x-x"
    "hello ^-^".tr_s("^---l-o", "x").should == "xllox-x"
  end

  it "tries to convert from_str and to_str to strings using to_str" do
    from_str = mock('ab')
    from_str.should_receive(:to_str).and_return("ab")

    to_str = mock('AB')
    to_str.should_receive(:to_str).and_return("AB")

    "bla".tr_s(from_str, to_str).should == "BlA"
  end

  it "returns subclass instances when called on a subclass" do
    StringSpecs::MyString.new("hello").tr_s("e", "a").should be_kind_of(StringSpecs::MyString)
  end

  it "taints the result when self is tainted" do
    ["h", "hello"].each do |str|
      tainted_str = str.dup.taint

      tainted_str.tr_s("e", "a").tainted?.should == true

      str.tr_s("e".taint, "a").tainted?.should == false
      str.tr_s("e", "a".taint).tainted?.should == false
    end
  end
end

describe "String#tr_s!" do
  it "modifies self in place" do
    s = "hello"
    s.tr_s!("l", "r").should == "hero"
    s.should == "hero"
  end

  it "returns nil if no modification was made" do
    s = "hello"
    s.tr_s!("za", "yb").should == nil
    s.tr_s!("", "").should == nil
    s.should == "hello"
  end

  it "does not modify self if from_str is empty" do
    s = "hello"
    s.tr_s!("", "").should == nil
    s.should == "hello"
    s.tr_s!("", "yb").should == nil
    s.should == "hello"
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError if self is frozen" do
      s = "hello".freeze
      lambda { s.tr_s!("el", "ar") }.should raise_error(TypeError)
      lambda { s.tr_s!("l", "r")   }.should raise_error(TypeError)
      lambda { s.tr_s!("", "")     }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError if self is frozen" do
      s = "hello".freeze
      lambda { s.tr_s!("el", "ar") }.should raise_error(RuntimeError)
      lambda { s.tr_s!("l", "r")   }.should raise_error(RuntimeError)
      lambda { s.tr_s!("", "")     }.should raise_error(RuntimeError)
    end
  end
end
