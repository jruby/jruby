# -*- encoding: utf-8 -*-

require 'strscan'

describe "StringScanner#scan" do
  before :each do
    @s = StringScanner.new("This is a test")
    @kcode = $KCODE
  end

  after :each do
    $KCODE = @kcode
  end

  it "returns the matched string" do
    @s.scan(/\w+/).should == "This"
    @s.scan(/.../).should == " is"
    @s.scan(//).should == ""
    @s.scan(/\s+/).should == " "
  end

  ruby_version_is ""..."1.9" do
    it "returns the first character for a multi byte string with no KCODE" do
      $KCODE = 'NONE'
      m = StringScanner.new("Привет!")
      m.scan(/[А-Яа-я]+/).should == "\320\237\321"
      m.rest.should == "\200\320\270\320\262\320\265\321\202!"
    end

    it "returns the matched string for a multi byte string with KCODE" do
      $KCODE = 'UTF-8'
      m = StringScanner.new("Привет!")
      m.scan(/[А-Яа-я]+/).should == "Привет"
      m.rest.should == "!"
    end

    it "returns the matched string for a multi byte string with unicode regexp" do
      $KCODE = 'NONE'
      m = StringScanner.new("Привет!")
      m.scan(/[А-Яа-я]+/u).should == "Привет"
      m.rest.should == "!"
    end
  end

  ruby_version_is "1.9" do
    it "returns the matched string for a multi byte string" do
      m = StringScanner.new("Привет!")
      m.scan(/[А-Яа-я]+/).should == "Привет"
      m.rest.should == "!"
    end
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
    lambda { @s.scan("aoeu")    }.should raise_error(TypeError)
    lambda { @s.scan(5)         }.should raise_error(TypeError)
    lambda { @s.scan(:test)     }.should raise_error(TypeError)
    lambda { @s.scan(mock('x')) }.should raise_error(TypeError)
  end
end
