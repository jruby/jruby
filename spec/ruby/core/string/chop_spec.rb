require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#chop" do
  it "returns a new string with the last character removed" do
    "hello\n".chop.should == "hello"
    "hello\x00".chop.should == "hello"
    "hello".chop.should == "hell"

    ori_str = encode("", "utf-8")
    256.times { |i| ori_str << i }

    str = ori_str
    256.times do |i|
      str = str.chop
      str.should == ori_str[0, 255 - i]
    end
  end

  it "removes both characters if the string ends with \\r\\n" do
    "hello\r\n".chop.should == "hello"
    "hello\r\n\r\n".chop.should == "hello\r\n"
    "hello\n\r".chop.should == "hello\n"
    "hello\n\n".chop.should == "hello\n"
    "hello\r\r".chop.should == "hello\r"

    "\r\n".chop.should == ""
  end

  it "returns an empty string when applied to an empty string" do
    "".chop.should == ""
  end

  it "returns a new string when applied to an empty string" do
    s = ""
    s.chop.should_not equal(s)
  end

  it "taints result when self is tainted" do
    "hello".taint.chop.tainted?.should == true
    "".taint.chop.tainted?.should == true
  end

  ruby_version_is "1.9" do
    it "untrusts result when self is untrusted" do
      "hello".untrust.chop.untrusted?.should == true
      "".untrust.chop.untrusted?.should == true
    end
  end

  it "returns subclass instances when called on a subclass" do
    StringSpecs::MyString.new("hello\n").chop.should be_kind_of(StringSpecs::MyString)
    StringSpecs::MyString.new("hello").chop.should be_kind_of(StringSpecs::MyString)
    StringSpecs::MyString.new("").chop.should be_kind_of(StringSpecs::MyString)
  end
end

describe "String#chop!" do
  it "behaves just like chop, but in-place" do
    ["hello\n", "hello\r\n", "hello", ""].each do |base|
      str = base.dup
      str.chop!

      str.should == base.chop
    end
  end

  it "returns self if modifications were made" do
    ["hello", "hello\r\n"].each do |s|
      s.chop!.should equal(s)
    end
  end

  it "returns nil when called on an empty string" do
    "".chop!.should == nil
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError when self is frozen" do
      lambda { "string\n\r".freeze.chop! }.should raise_error(TypeError)
    end

    it "does not raise an exception on a frozen instance that would not be modified" do
      "".freeze.chop!.should be_nil
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError on a frozen instance that is modified" do
      lambda { "string\n\r".freeze.chop! }.should raise_error(RuntimeError)
    end

    # see [ruby-core:23666]
    it "raises a RuntimeError on a frozen instance that would not be modified" do
      a = ""
      a.freeze
      lambda { a.chop! }.should raise_error(RuntimeError)
    end
  end
end
