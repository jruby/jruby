require File.dirname(__FILE__) + "/../spec_helper"

import "spec.java_integration.fixtures.CoreTypeMethods"

describe "Java core and primitive types" do
  it "should coerce to Ruby types when returned" do 
    CoreTypeMethods.getString.should be_kind_of(String)
    
    CoreTypeMethods.getByte.should be_kind_of(Fixnum)
    CoreTypeMethods.getShort.should be_kind_of(Fixnum)
    CoreTypeMethods.getChar.should be_kind_of(Fixnum)
    CoreTypeMethods.getInt.should be_kind_of(Fixnum)
    CoreTypeMethods.getLong.should be_kind_of(Fixnum)
    
    CoreTypeMethods.getFloat.should be_kind_of(Float)
    CoreTypeMethods.getDouble.should be_kind_of(Float)
    
    CoreTypeMethods.getBooleanTrue.should be_kind_of(TrueClass)
    CoreTypeMethods.getBooleanTrue.should == true
    CoreTypeMethods.getBooleanFalse.should be_kind_of(FalseClass)
    CoreTypeMethods.getBooleanFalse.should == false
    
    CoreTypeMethods.getNull.should be_kind_of(NilClass)
    CoreTypeMethods.getNull.should == nil
  end
end