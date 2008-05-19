require File.dirname(__FILE__) + "/../spec_helper"

import "java_integration.fixtures.CoreTypeMethods"

describe "Java String and primitive-typed methods" do
  it "should coerce to Ruby types when returned" do 
    CoreTypeMethods.getString.should be_kind_of(String)
    CoreTypeMethods.getString.should == "foo";
    
    CoreTypeMethods.getByte.should be_kind_of(Fixnum)
    CoreTypeMethods.getByte.should == 1
    CoreTypeMethods.getShort.should be_kind_of(Fixnum)
    CoreTypeMethods.getShort.should == 2
    CoreTypeMethods.getChar.should be_kind_of(Fixnum)
    CoreTypeMethods.getChar.should == 2
    CoreTypeMethods.getInt.should be_kind_of(Fixnum)
    CoreTypeMethods.getInt.should == 4
    CoreTypeMethods.getLong.should be_kind_of(Fixnum)
    CoreTypeMethods.getLong.should == 8
    
    CoreTypeMethods.getFloat.should be_kind_of(Float)
    CoreTypeMethods.getFloat.should == 4
    CoreTypeMethods.getDouble.should be_kind_of(Float)
    CoreTypeMethods.getDouble.should == 8
    
    CoreTypeMethods.getBooleanTrue.should be_kind_of(TrueClass)
    CoreTypeMethods.getBooleanTrue.should == true
    CoreTypeMethods.getBooleanFalse.should be_kind_of(FalseClass)
    CoreTypeMethods.getBooleanFalse.should == false
    
    CoreTypeMethods.getNull.should be_kind_of(NilClass)
    CoreTypeMethods.getNull.should == nil
    
    CoreTypeMethods.getVoid.should == nil
  end
  
  it "should be coerced from Ruby types when passing parameters" do
    CoreTypeMethods.setString("string").should == "string"
    
    CoreTypeMethods.setByte(1).should == "1"
    CoreTypeMethods.setShort(1).should == "1"
    CoreTypeMethods.setChar(1).should == "\001"
    CoreTypeMethods.setInt(1).should == "1"
    CoreTypeMethods.setLong(1).should == "1"
    
    CoreTypeMethods.setFloat(1.0).should == "1.0"
    CoreTypeMethods.setDouble(1.0).should == "1.0"
    
    CoreTypeMethods.setBooleanTrue(true).should == "true"
    CoreTypeMethods.setBooleanFalse(false).should == "false"
    
    CoreTypeMethods.setNull(nil).should == "null"
  end
  
  it "should raise errors when passed values can not be precisely coerced" do
    pending("precision failure does not raise error") do
      lambda { CoreTypeMethods.setByte(1 << 8) }.should raise_error(TypeError)
      lambda { CoreTypeMethods.setShort(1 << 16) }.should raise_error(TypeError)
      lambda { CoreTypeMethods.setChar(1 << 16) }.should raise_error(TypeError)
      lambda { CoreTypeMethods.setInt(1 << 32) }.should raise_error(TypeError)
      lambda { CoreTypeMethods.setLong(1 << 64) }.should raise_error(TypeError)
    end
  end
  
  it "should select the most narrow and precise overloaded method" do
    pending("Fixnum always selects long method") do
      CoreTypeMethods.getType(1).should == "byte"
      CoreTypeMethods.getType(1 << 8).should == "short"
      CoreTypeMethods.getType(1 << 16).should == "int"
    end
    CoreTypeMethods.getType(1 << 32).should == "long"
    
    CoreTypeMethods.getType(1.0).should == "double"
    
    CoreTypeMethods.getType("foo").should == "String"
    CoreTypeMethods.getType(nil).should == "CharSequence"
  end
end