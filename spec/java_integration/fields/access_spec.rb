require File.dirname(__FILE__) + "/../spec_helper"

import "java_integration.fixtures.PrivateField"
import "java_integration.fixtures.ProtectedField"
import "java_integration.fixtures.PublicField"
import "java_integration.fixtures.PackageField"

class PrivateField
  field_accessor :strField => :field
end

class ProtectedField
  field_accessor :strField => :field
end

class PackageField
  field_accessor :strField => :field

  def existing_method
    "meth"
  end

  field_reader :strField => :existing_method
end

describe "JRuby-wrapped Java Objects" do
  it "should expose private Java fields when field_accessor used" do
    lambda {
      PrivateField.new.field.should == "1764"
    }.should_not raise_error
    
    lambda {
      obj = PrivateField.new
      obj.field = "foo"
      obj.field.should == "foo"
    }.should_not raise_error
  end
  
  it "should expose protected Java fields when field_accessor used" do
    lambda {
      ProtectedField.new.field.should == "1765"
    }.should_not raise_error
    
    lambda {
      obj = ProtectedField.new
      obj.field = "foo"
      obj.field.should == "foo"
    }.should_not raise_error
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
      PackageField.new.field.should == "1766"
    }.should_not raise_error
    
    lambda {
      obj = PackageField.new
      obj.field = "foo"
      obj.field.should == "foo"
    }.should_not raise_error
  end

  it "should throw an error for a field which does not exist" do
    lambda {
      class PackageField
        field_accessor(:totallyBogus).should raise_error
      end
    }
  end

  it "should throw an error for one field which does not exist of two" do
    lambda {
      class PackageField
        field_accessor(:strField, :totallyBogus).should raise_error
      end
    }
  end

  it "should not allow field_accessor to work on final field" do
    lambda {
      class PrivateField
        field_accessor(:finalStrField).should raise_error
      end
    }
  end    
end
