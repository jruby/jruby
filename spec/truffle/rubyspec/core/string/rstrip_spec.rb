require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#rstrip" do
  it "returns a copy of self with trailing whitespace removed" do
    "  hello  ".rstrip.should == "  hello"
    "  hello world  ".rstrip.should == "  hello world"
    "  hello world \n\r\t\n\v\r".rstrip.should == "  hello world"
    "hello".rstrip.should == "hello"
    "hello\x00".rstrip.should == "hello"
  end

  ruby_version_is ""..."1.9" do
    it "returns a copy of self with trailing NULL bytes and whitespace after a NULL byte removed" do
      "\x00 \x00hello\x00 \x00".rstrip.should == "\x00 \x00hello\x00"
    end
  end

  ruby_version_is "1.9" do
    it "returns a copy of self with all trailing whitespace and NULL bytes removed" do
      "\x00 \x00hello\x00 \x00".rstrip.should == "\x00 \x00hello"
    end
  end

  it "taints the result when self is tainted" do
    "".taint.rstrip.tainted?.should == true
    "ok".taint.rstrip.tainted?.should == true
    "ok    ".taint.rstrip.tainted?.should == true
  end
end

describe "String#rstrip!" do
  it "modifies self in place and returns self" do
    a = "  hello  "
    a.rstrip!.should equal(a)
    a.should == "  hello"
  end

  ruby_version_is ""..."1.9" do
    it "modifies self removing trailing NULL bytes and whitespace after a NULL" do
      a = "\x00 \x00hello\x00 \x00"
      a.rstrip!
      a.should == "\x00 \x00hello\x00"
    end
  end

  ruby_version_is "1.9" do
    it "modifies self removing trailing NULL bytes and whitespace" do
      a = "\x00 \x00hello\x00 \x00"
      a.rstrip!
      a.should == "\x00 \x00hello"
    end
  end

  it "returns nil if no modifications were made" do
    a = "hello"
    a.rstrip!.should == nil
    a.should == "hello"
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError on a frozen instance that is modified" do
      lambda { "  hello  ".freeze.rstrip! }.should raise_error(TypeError)
    end

    it "does not raise an exception on a frozen instance that would not be modified" do
      "hello".freeze.rstrip!.should be_nil # ok, nothing changed
      "".freeze.rstrip!.should be_nil # ok, nothing changed
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError on a frozen instance that is modified" do
      lambda { "  hello  ".freeze.rstrip! }.should raise_error(RuntimeError)
    end

    # see [ruby-core:23666]
    it "raises a RuntimeError on a frozen instance that would not be modified" do
      lambda { "hello".freeze.rstrip! }.should raise_error(RuntimeError)
      lambda { "".freeze.rstrip!      }.should raise_error(RuntimeError)
    end
  end
end
