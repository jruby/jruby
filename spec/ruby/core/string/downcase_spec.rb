# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#downcase" do
  it "returns a copy of self with all uppercase letters downcased" do
    "hELLO".downcase.should == "hello"
    "hello".downcase.should == "hello"
  end

  it "is locale insensitive (only replaces A-Z)" do
    "ÄÖÜ".downcase.should == "ÄÖÜ"

    str = Array.new(256) { |c| c.chr }.join
    expected = Array.new(256) do |i|
      c = i.chr
      c.between?("A", "Z") ? c.downcase : c
    end.join

    str.downcase.should == expected
  end

  it "taints result when self is tainted" do
    "".taint.downcase.tainted?.should == true
    "x".taint.downcase.tainted?.should == true
    "X".taint.downcase.tainted?.should == true
  end

  it "returns a subclass instance for subclasses" do
    StringSpecs::MyString.new("FOObar").downcase.should be_kind_of(StringSpecs::MyString)
  end
end

describe "String#downcase!" do
  it "modifies self in place" do
    a = "HeLlO"
    a.downcase!.should equal(a)
    a.should == "hello"
  end

  it "returns nil if no modifications were made" do
    a = "hello"
    a.downcase!.should == nil
    a.should == "hello"
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError when self is frozen" do
      lambda { "HeLlo".freeze.downcase! }.should raise_error(TypeError)
      lambda { "hello".freeze.downcase! }.should raise_error(TypeError)
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError when self is frozen" do
      lambda { "HeLlo".freeze.downcase! }.should raise_error(RuntimeError)
      lambda { "hello".freeze.downcase! }.should raise_error(RuntimeError)
    end
  end
end
