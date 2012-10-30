require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#strip" do
  it "returns a new string with leading and trailing whitespace removed" do
    "   hello   ".strip.should == "hello"
    "   hello world   ".strip.should == "hello world"
    "\tgoodbye\r\v\n".strip.should == "goodbye"
    "\x00 goodbye \x00".strip.should == "\x00 goodbye"
  end

  ruby_version_is ""..."1.9" do
    it "returns a copy of self with trailing NULL bytes and whitespace after a NULL byte removed" do
      " \x00 goodbye \x00 ".strip.should == "\x00 goodbye \x00"
    end
  end

  ruby_version_is "1.9" do
    it "returns a copy of self with trailing NULL bytes and whitespace" do
      " \x00 goodbye \x00 ".strip.should == "\x00 goodbye"
    end
  end

  it "taints the result when self is tainted" do
    "".taint.strip.tainted?.should == true
    "ok".taint.strip.tainted?.should == true
    "  ok  ".taint.strip.tainted?.should == true
  end
end

describe "String#strip!" do
  it "modifies self in place and returns self" do
    a = "   hello   "
    a.strip!.should equal(a)
    a.should == "hello"

    a = "\tgoodbye\r\v\n"
    a.strip!
    a.should == "goodbye"

    a = "\000 goodbye \000"
    a.strip!
    a.should == "\000 goodbye"

  end

  it "returns nil if no modifications where made" do
    a = "hello"
    a.strip!.should == nil
    a.should == "hello"
  end

  ruby_version_is ""..."1.9" do
    it "modifies self removing trailing NULL bytes and whitespace after a NULL byte" do
      a = " \x00 goodbye \x00 "
      a.strip!
      a.should == "\x00 goodbye \x00"
    end

    it "raises a TypeError on a frozen instance that is modified" do
      lambda { "  hello  ".freeze.strip! }.should raise_error(TypeError)
    end

    it "does not raise an exception on a frozen instance that would not be modified" do
      "hello".freeze.strip!.should be_nil
      "".freeze.strip!.should be_nil
    end
  end

  ruby_version_is "1.9" do
    it "modifies self removing trailing NULL bytes and whitespace" do
      a = " \x00 goodbye \x00 "
      a.strip!
      a.should == "\x00 goodbye"
    end

    it "raises a RuntimeError on a frozen instance that is modified" do
      lambda { "  hello  ".freeze.strip! }.should raise_error(RuntimeError)
    end

    # see #1552
    it "raises a RuntimeError on a frozen instance that would not be modified" do
      lambda {"hello".freeze.strip! }.should raise_error(RuntimeError)
      lambda {"".freeze.strip!      }.should raise_error(RuntimeError)
    end
  end
end
