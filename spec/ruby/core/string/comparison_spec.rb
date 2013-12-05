# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#<=> with String" do
  it "compares individual characters based on their ascii value" do
    ascii_order = Array.new(256) { |x| x.chr }
    sort_order = ascii_order.sort
    sort_order.should == ascii_order
  end

  it "returns -1 when self is less than other" do
    ("this" <=> "those").should == -1
  end

  it "returns 0 when self is equal to other" do
    ("yep" <=> "yep").should == 0
  end

  it "returns 1 when self is greater than other" do
    ("yoddle" <=> "griddle").should == 1
  end

  it "considers string that comes lexicographically first to be less if strings have same size" do
    ("aba" <=> "abc").should == -1
    ("abc" <=> "aba").should == 1
  end

  it "doesn't consider shorter string to be less if longer string starts with shorter one" do
    ("abc" <=> "abcd").should == -1
    ("abcd" <=> "abc").should == 1
  end

  it "compares shorter string with corresponding number of first chars of longer string" do
    ("abx" <=> "abcd").should == 1
    ("abcd" <=> "abx").should == -1
  end

  it "ignores subclass differences" do
    a = "hello"
    b = StringSpecs::MyString.new("hello")

    (a <=> b).should == 0
    (b <=> a).should == 0
  end

  ruby_version_is "1.9" do
    it "returns 0 if self and other are bytewise identical and have the same encoding" do
      ("ÄÖÜ" <=> "ÄÖÜ").should == 0
    end

    it "returns 0 if self and other are bytewise identical and have the same encoding" do
      ("ÄÖÜ" <=> "ÄÖÜ").should == 0
    end

    it "returns -1 if self is bytewise less than other" do
      ("ÄÖÛ" <=> "ÄÖÜ").should == -1
    end

    it "returns 1 if self is bytewise greater than other" do
      ("ÄÖÜ" <=> "ÄÖÛ").should == 1
    end

    it "returns 0 if self and other contain identical ASCII-compatible bytes in different encodings" do
      ("abc".force_encoding("utf-8") <=> "abc".force_encoding("iso-8859-1")).should == 0
    end

    it "does not return 0 if self and other contain identical non-ASCII-compatible bytes in different encodings" do
      ("\xff".force_encoding("utf-8") <=> "\xff".force_encoding("iso-8859-1")).should_not == 0
    end
  end
end

# Note: This is inconsistent with Array#<=> which calls #to_ary instead of
# just using it as an indicator.
describe "String#<=>" do
  ruby_version_is ""..."2.0" do
    it "returns nil if its argument does not provide #to_str" do
      ("abc" <=> mock('x')).should be_nil
    end

    it "returns nil if its argument does not provide #<=>" do
      obj = mock('x')
      ("abc" <=> obj).should be_nil
    end

    it "calls #to_str to convert the argument to a String and calls #<=> to compare with self" do
      obj = mock('x')

      # String#<=> merely checks if #to_str is defined on the object. It
      # does not call the method.
      obj.stub!(:to_str)
      obj.should_not_receive(:to_str)
      obj.should_receive(:<=>).with("abc").and_return(1)

      ("abc" <=> obj).should == -1
    end
  end

  ruby_version_is "2.0" do
    it "returns nil if its argument provides neither #to_str nor #<=>" do
      ("abc" <=> mock('x')).should be_nil
    end

    it "uses the result of calling #to_str for comparison when #to_str is defined" do
      obj = mock('x')
      obj.should_receive(:to_str).and_return("aaa")

      ("abc" <=> obj).should == 1
    end

    it "uses the result of calling #<=> on its argument when #<=> is defined but #to_str is not" do
      obj = mock('x')
      obj.should_receive(:<=>).and_return(-1)

      ("abc" <=> obj).should == 1
    end
  end
end
