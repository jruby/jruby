require File.dirname(__FILE__) + "/../spec_helper"

import "java_integration.fixtures.PrivateField"
import "java_integration.fixtures.ProtectedField"
import "java_integration.fixtures.PublicField"
import "java_integration.fixtures.PackageField"

describe "JRuby-wrapped Java Objects" do
  it "should expose private Java fields" do
    lambda {
      PrivateField.new.strField.should == "1764"
    }.should_not raise_error
    
    pending "Private fields can't be directly mutated" do
      lambda {
        PrivateField.new.strField = "foo"
      }.should_not raise_error
    end
  end
  
  it "should expose protected Java fields" do
    lambda {
      ProtectedField.new.strField.should == "1765"
    }.should_not raise_error
    
    pending "Protected fields can't be directly mutated" do
      lambda {
        ProtectedField.new.strField = "foo"
      }.should_not raise_error
    end
  end
  
  it "should expose public-visible fields" do
    lambda {
      PublicField.new.strField.should == "1767"
    }.should_not raise_error

    year = java.util.Date.new.year

    lambda {
      PublicField.new.dateField.year.should == year
    }.should_not raise_error
  end

  it "should expose package-visible fields" do
    lambda {
      PackageField.new.strField.should == "1766"
    }.should_not raise_error
    
    pending "Package fields can't be directly mutated" do
      lambda {
        PackageField.new.strField = "foo"
      }.should_not raise_error
    end
  end
end
