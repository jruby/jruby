require File.dirname(__FILE__) + '/../spec_helper'

context "String instance method" do
  specify "lstrip should return a string with all leading \\000 and whitespace characters removed" do
    "".lstrip.should == ""
    " hello ".lstrip.should == "hello "
    "\000  hello ".lstrip.should == "hello "
    "\000\t \000hello ".lstrip.should == "hello "
    "hello".lstrip.should == "hello"
  end

  specify "lstrip! should modify self by removing all leading \\000 and whitespace characters" do
    "\n\t This \000".lstrip!.should == "This \000"
    "\000 another one".lstrip!.should == "another one"
    "\000  \000\t\v\000two  ".lstrip!.should == "two  "
  end
  
  specify "rstrip should return a string with all trailing \\000 and whitespace characters removed" do
    " \t\n ".rstrip.should == ""
    "\t".rstrip.rstrip.should == ""
    "".rstrip.rstrip.should == ""
    " hello ".rstrip.rstrip.should == " hello"
    "\tgoodbye\r\n".rstrip.rstrip.should == "\tgoodbye"
    "goodbye \000".rstrip.rstrip.should == "goodbye"
    "goodbye \000\t \f  \000".rstrip.rstrip.should == "goodbye" 
  end

  specify "rstrip! should modify self by removing all trailing \\000 and whitespace characters" do
    " hello ".rstrip!.should == " hello"
    "\tgoodbye\r\n".rstrip!.should == "\tgoodbye"
    "goodbye \000".rstrip!.should == "goodbye"
    "goodbye \000 ".rstrip!.should == "goodbye"
    "".rstrip!.should == nil
    " \n \000\v\000".rstrip!.should == ""
  end
end

describe "String instance method %" do
  it "format float values Infinity and Nan" do
    ("%E" % 1e1020).should == "Inf"
    ("%E" % -1e1020).should == "-Inf"
    ("%-10E" % 1e1020).should == "Inf       "
    ("%-010E" % 1e1020).should == "0000000Inf"
    ("%010E" % 1e1020).should == "0000000Inf"
    ("%+E" % 1e1020).should == "+Inf"
    ("% E" % 1e1020).should == " Inf"
    ("%E" % (0.0/0)).should == "NAN"
    ("%E" % (-0e0/0)).should == "NAN"
  end
  
  it "format float (e) values should return a string resulting from applying the format" do  
    ("%e" % 1e1020).should == "inf"
    ("%e" % -1e1020).should == "-inf"
  end
end
