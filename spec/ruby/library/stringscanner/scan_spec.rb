require File.expand_path('../../../spec_helper', __FILE__)
require 'strscan'

describe "StringScanner#scan" do
  context "simple match" do
    before :each do
      @s = StringScanner.new("This is a test")
    end

    it "returns the matched string" do
      @s.scan(/\w+/).should == "This"
      @s.scan(/.../).should == " is"
      @s.scan(//).should == ""
      @s.scan(/\s+/).should == " "
    end

    it "treats ^ as matching from the beginning of the current position" do
      @s.scan(/\w+/).should == "This"
      @s.scan(/^\d/).should be_nil
      @s.scan(/^\s/).should == " "
    end

    it "returns nil if there's no match" do
      @s.scan(/\d/).should == nil
    end

    it "returns nil when there is no more to scan" do
      @s.scan(/[\w\s]+/).should == "This is a test"
      @s.scan(/\w+/).should be_nil
    end

    it "returns an empty string when the pattern matches empty" do
      @s.scan(/.*/).should == "This is a test"
      @s.scan(/.*/).should == ""
      @s.scan(/./).should be_nil
    end

    it "raises a TypeError if pattern isn't a Regexp" do
      lambda { @s.scan("aoeu") }.should raise_error(TypeError)
      lambda { @s.scan(5) }.should raise_error(TypeError)
      lambda { @s.scan(:test) }.should raise_error(TypeError)
      lambda { @s.scan(mock('x')) }.should raise_error(TypeError)
    end
  end

  #tests for jruby 1067
  context "with named parameters" do
    before :each do
      @s = StringScanner.new("Fri Dec 12 1975 14:39")
      @s.scan(/(?<wday>\w+) (?<month>\w+) (?<day>\d+)/)
    end
    it "should extract day of week" do
      @s[:wday].should  == 'Fri'
    end

    it "should extract month abbreviation" do
      @s[:month].should  == 'Dec'
    end

    it "should extract day of month" do
      @s[:day].should  == '12'
    end
  end
end
