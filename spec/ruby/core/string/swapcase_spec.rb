# -*- encoding: utf-8 -*-
require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

describe "String#swapcase" do
  it "returns a new string with all uppercase chars from self converted to lowercase and vice versa" do
   "Hello".swapcase.should == "hELLO"
   "cYbEr_PuNk11".swapcase.should == "CyBeR_pUnK11"
   "+++---111222???".swapcase.should == "+++---111222???"
  end

  it "taints resulting string when self is tainted" do
    "".taint.swapcase.tainted?.should == true
    "hello".taint.swapcase.tainted?.should == true
  end

  it "is locale insensitive (only upcases a-z and only downcases A-Z)" do
    "ÄÖÜ".swapcase.should == "ÄÖÜ"
    "ärger".swapcase.should == "äRGER"
    "BÄR".swapcase.should == "bÄr"
  end

  it "returns subclass instances when called on a subclass" do
    StringSpecs::MyString.new("").swapcase.should be_kind_of(StringSpecs::MyString)
    StringSpecs::MyString.new("hello").swapcase.should be_kind_of(StringSpecs::MyString)
  end
end

describe "String#swapcase!" do
  it "modifies self in place" do
    a = "cYbEr_PuNk11"
    a.swapcase!.should equal(a)
    a.should == "CyBeR_pUnK11"
  end

  it "returns nil if no modifications were made" do
    a = "+++---111222???"
    a.swapcase!.should == nil
    a.should == "+++---111222???"

    "".swapcase!.should == nil
  end

  ruby_version_is ""..."1.9" do
    it "raises a TypeError when self is frozen" do
      ["", "hello"].each do |a|
        a.freeze
        lambda { a.swapcase! }.should raise_error(TypeError)
      end
    end
  end

  ruby_version_is "1.9" do
    it "raises a RuntimeError when self is frozen" do
      ["", "hello"].each do |a|
        a.freeze
        lambda { a.swapcase! }.should raise_error(RuntimeError)
      end
    end
  end
end
