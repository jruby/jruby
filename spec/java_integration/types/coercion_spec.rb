require File.dirname(__FILE__) + "/../spec_helper"

import "java_integration.fixtures.CoreTypeMethods"
import "java_integration.fixtures.JavaFields"
import "java_integration.fixtures.ValueReceivingInterface"
import "java_integration.fixtures.ValueReceivingInterfaceHandler"

import "java_integration.fixtures.PackageConstructor"
import "java_integration.fixtures.ProtectedConstructor"
import "java_integration.fixtures.PrivateConstructor"

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
    CoreTypeMethods.getFloat.should == 4.5
    CoreTypeMethods.getDouble.should be_kind_of(Float)
    CoreTypeMethods.getDouble.should == 8.5
    
    CoreTypeMethods.getBooleanTrue.should be_kind_of(TrueClass)
    CoreTypeMethods.getBooleanTrue.should == true
    CoreTypeMethods.getBooleanFalse.should be_kind_of(FalseClass)
    CoreTypeMethods.getBooleanFalse.should == false
    
    CoreTypeMethods.getNull.should be_kind_of(NilClass)
    CoreTypeMethods.getNull.should == nil
    
    CoreTypeMethods.getVoid.should == nil

    CoreTypeMethods.getBigInteger.should == 1234567890123456789012345678901234567890
  end
  
  it "should be coerced from Ruby types when passing parameters" do
    CoreTypeMethods.setString("string").should == "string"
    
    CoreTypeMethods.setByte(1).should == "1"
    CoreTypeMethods.setShort(1).should == "1"
    CoreTypeMethods.setChar(1).should == "\001"
    CoreTypeMethods.setInt(1).should == "1"
    CoreTypeMethods.setLong(1).should == "1"
    
    CoreTypeMethods.setFloat(1).should == "1.0"
    CoreTypeMethods.setDouble(1).should == "1.0"

    CoreTypeMethods.setByte(1.5).should == "1"
    CoreTypeMethods.setShort(1.5).should == "1"
    CoreTypeMethods.setChar(1.5).should == "\001"
    CoreTypeMethods.setInt(1.5).should == "1"
    CoreTypeMethods.setLong(1.5).should == "1"

    CoreTypeMethods.setFloat(1.5).should == "1.5"
    CoreTypeMethods.setDouble(1.5).should == "1.5"
    
    CoreTypeMethods.setBooleanTrue(true).should == "true"
    CoreTypeMethods.setBooleanFalse(false).should == "false"

    CoreTypeMethods.setByte(nil).should == "0"
    CoreTypeMethods.setShort(nil).should == "0"
    CoreTypeMethods.setChar(nil).should == "\000"
    CoreTypeMethods.setInt(nil).should == "0"
    CoreTypeMethods.setLong(nil).should == "0"

    CoreTypeMethods.setFloat(nil).should == "0.0"
    CoreTypeMethods.setDouble(nil).should == "0.0"

    CoreTypeMethods.setBooleanTrue(nil).should == "false"
    CoreTypeMethods.setBooleanFalse(nil).should == "false"

    CoreTypeMethods.setBigInteger(1234567890123456789012345678901234567890).should ==
      "1234567890123456789012345678901234567890"

    CoreTypeMethods.setByteObj(1).should == "1"
    CoreTypeMethods.setShortObj(1).should == "1"
    CoreTypeMethods.setCharObj(1).should == "\001"
    CoreTypeMethods.setIntObj(1).should == "1"
    CoreTypeMethods.setLongObj(1).should == "1"

    CoreTypeMethods.setFloatObj(1).should == "1.0"
    CoreTypeMethods.setDoubleObj(1).should == "1.0"

    CoreTypeMethods.setByteObj(1.5).should == "1"
    CoreTypeMethods.setShortObj(1.5).should == "1"
    CoreTypeMethods.setCharObj(1.5).should == "\001"
    CoreTypeMethods.setIntObj(1.5).should == "1"
    CoreTypeMethods.setLongObj(1.5).should == "1"

    CoreTypeMethods.setFloatObj(1.5).should == "1.5"
    CoreTypeMethods.setDoubleObj(1.5).should == "1.5"

    CoreTypeMethods.setBooleanTrueObj(true).should == "true"
    CoreTypeMethods.setBooleanFalseObj(false).should == "false"

    CoreTypeMethods.setByteObj(nil).should == "null"
    CoreTypeMethods.setShortObj(nil).should == "null"
    CoreTypeMethods.setCharObj(nil).should == "null"
    CoreTypeMethods.setIntObj(nil).should == "null"
    CoreTypeMethods.setLongObj(nil).should == "null"

    CoreTypeMethods.setFloatObj(nil).should == "null"
    CoreTypeMethods.setDoubleObj(nil).should == "null"

    CoreTypeMethods.setBooleanTrueObj(nil).should == "null"
    CoreTypeMethods.setBooleanFalseObj(nil).should == "null"
    
    CoreTypeMethods.setNull(nil).should == "null"
  end
  
  it "should raise errors when passed values can not be precisely coerced" do
    lambda { CoreTypeMethods.setByte(1 << 8) }.should raise_error(RangeError)
    lambda { CoreTypeMethods.setShort(1 << 16) }.should raise_error(RangeError)
    lambda { CoreTypeMethods.setChar(1 << 16) }.should raise_error(RangeError)
    lambda { CoreTypeMethods.setInt(1 << 32) }.should raise_error(RangeError)
    lambda { CoreTypeMethods.setLong(1 << 64) }.should raise_error(RangeError)
  end
  
  it "should select the most narrow and precise overloaded method" do
    pending "selection based on precision is not supported yet" do
      CoreTypeMethods.getType(1).should == "byte"
      CoreTypeMethods.getType(1 << 8).should == "short"
      CoreTypeMethods.getType(1 << 16).should == "int"
      CoreTypeMethods.getType(1.0).should == "float"
    end
    CoreTypeMethods.getType(1 << 32).should == "long"
    
    CoreTypeMethods.getType(2.0 ** 128).should == "double"
    
    CoreTypeMethods.getType("foo").should == "String"
    pending "passing null to overloaded methods randomly selects from them" do
      CoreTypeMethods.getType(nil).should == "CharSequence"
    end

    CoreTypeMethods.getType(BigDecimal.new('1.1')).should == "BigDecimal"
  end
end

describe "Java Object-typed methods" do
  it "should coerce primitive Ruby types to a single, specific Java type" do
    CoreTypeMethods.getObjectType("foo").should == "class java.lang.String"

    CoreTypeMethods.getObjectType(0).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Byte::MAX_VALUE).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Byte::MIN_VALUE).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Byte::MAX_VALUE + 1).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Byte::MIN_VALUE - 1).should == "class java.lang.Long"

    CoreTypeMethods.getObjectType(java::lang::Short::MAX_VALUE).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Short::MIN_VALUE).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Short::MAX_VALUE + 1).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Short::MIN_VALUE - 1).should == "class java.lang.Long"

    CoreTypeMethods.getObjectType(java::lang::Integer::MAX_VALUE).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Integer::MIN_VALUE).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Integer::MAX_VALUE + 1).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Integer::MIN_VALUE - 1).should == "class java.lang.Long"

    CoreTypeMethods.getObjectType(java::lang::Long::MAX_VALUE).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Long::MIN_VALUE).should == "class java.lang.Long"
    CoreTypeMethods.getObjectType(java::lang::Long::MAX_VALUE + 1).should == "class java.math.BigInteger"
    CoreTypeMethods.getObjectType(java::lang::Long::MIN_VALUE - 1).should == "class java.math.BigInteger"

    CoreTypeMethods.getObjectType(java::lang::Float::MAX_VALUE).should == "class java.lang.Double"
    CoreTypeMethods.getObjectType(java::lang::Float::MIN_VALUE).should == "class java.lang.Double"
    CoreTypeMethods.getObjectType(-java::lang::Float::MAX_VALUE).should == "class java.lang.Double"
    CoreTypeMethods.getObjectType(-java::lang::Float::MIN_VALUE).should == "class java.lang.Double"

    CoreTypeMethods.getObjectType(java::lang::Float::NaN).should == "class java.lang.Double"
    CoreTypeMethods.getObjectType(0.0).should == "class java.lang.Double"

    CoreTypeMethods.getObjectType(java::lang::Double::MAX_VALUE).should == "class java.lang.Double"
    CoreTypeMethods.getObjectType(java::lang::Double::MIN_VALUE).should == "class java.lang.Double"
    CoreTypeMethods.getObjectType(-java::lang::Double::MAX_VALUE).should == "class java.lang.Double"
    CoreTypeMethods.getObjectType(-java::lang::Double::MIN_VALUE).should == "class java.lang.Double"

    CoreTypeMethods.getObjectType(true).should == "class java.lang.Boolean"
    CoreTypeMethods.getObjectType(1 << 128).should == "class java.math.BigInteger"
  end
end

describe "Java String and primitive-typed fields" do
  it "coerce to Ruby types when retrieved" do
    # static
    JavaFields.stringStaticField.should be_kind_of(String)
    JavaFields.stringStaticField.should == "foo";
    
    JavaFields.byteStaticField.should be_kind_of(Fixnum)
    JavaFields.byteStaticField.should == 1
    JavaFields.shortStaticField.should be_kind_of(Fixnum)
    JavaFields.shortStaticField.should == 2
    JavaFields.charStaticField.should be_kind_of(Fixnum)
    JavaFields.charStaticField.should == 2
    JavaFields.intStaticField.should be_kind_of(Fixnum)
    JavaFields.intStaticField.should == 4
    JavaFields.longStaticField.should be_kind_of(Fixnum)
    JavaFields.longStaticField.should == 8
    
    JavaFields.floatStaticField.should be_kind_of(Float)
    JavaFields.floatStaticField.should == 4.5
    JavaFields.doubleStaticField.should be_kind_of(Float)
    JavaFields.doubleStaticField.should == 8.5
    
    JavaFields.trueStaticField.should be_kind_of(TrueClass)
    JavaFields.trueStaticField.should == true
    JavaFields.falseStaticField.should be_kind_of(FalseClass)
    JavaFields.falseStaticField.should == false
    
    JavaFields.nullStaticField.should be_kind_of(NilClass)
    JavaFields.nullStaticField.should == nil

    JavaFields.bigIntegerStaticField.should be_kind_of(Bignum)
    JavaFields.bigIntegerStaticField.should ==
      1234567890123456789012345678901234567890
    
    # instance
    jf = JavaFields.new
    jf.stringField.should be_kind_of(String)
    jf.stringField.should == "foo";
    
    jf.byteField.should be_kind_of(Fixnum)
    jf.byteField.should == 1
    jf.shortField.should be_kind_of(Fixnum)
    jf.shortField.should == 2
    jf.charField.should be_kind_of(Fixnum)
    jf.charField.should == 2
    jf.intField.should be_kind_of(Fixnum)
    jf.intField.should == 4
    jf.longField.should be_kind_of(Fixnum)
    jf.longField.should == 8
    
    jf.floatField.should be_kind_of(Float)
    jf.floatField.should == 4.5
    jf.doubleField.should be_kind_of(Float)
    jf.doubleField.should == 8.5
    
    jf.trueField.should be_kind_of(TrueClass)
    jf.trueField.should == true
    jf.falseField.should be_kind_of(FalseClass)
    jf.falseField.should == false
    
    jf.nullField.should be_kind_of(NilClass)
    jf.nullField.should == nil

    jf.bigIntegerField.should be_kind_of(Bignum)
    jf.bigIntegerField.should ==
      1234567890123456789012345678901234567890
  end
end

describe "Java primitive-box-typed fields" do
  it "coerce to Ruby types when retrieved" do
    # static
    JavaFields.byteObjStaticField.should be_kind_of(Fixnum)
    JavaFields.byteObjStaticField.should == 1
    JavaFields.shortObjStaticField.should be_kind_of(Fixnum)
    JavaFields.shortObjStaticField.should == 2
    JavaFields.charObjStaticField.should be_kind_of(Fixnum)
    JavaFields.charObjStaticField.should == 2
    JavaFields.intObjStaticField.should be_kind_of(Fixnum)
    JavaFields.intObjStaticField.should == 4
    JavaFields.longObjStaticField.should be_kind_of(Fixnum)
    JavaFields.longObjStaticField.should == 8
    
    JavaFields.floatObjStaticField.should be_kind_of(Float)
    JavaFields.floatObjStaticField.should == 4.5
    JavaFields.doubleObjStaticField.should be_kind_of(Float)
    JavaFields.doubleObjStaticField.should == 8.5
    
    JavaFields.trueObjStaticField.should be_kind_of(TrueClass)
    JavaFields.trueObjStaticField.should == true
    JavaFields.falseObjStaticField.should be_kind_of(FalseClass)
    JavaFields.falseObjStaticField.should == false
    
    # instance
    jf = JavaFields.new
    jf.byteObjField.should be_kind_of(Fixnum)
    jf.byteObjField.should == 1
    jf.shortObjField.should be_kind_of(Fixnum)
    jf.shortObjField.should == 2
    jf.charObjField.should be_kind_of(Fixnum)
    jf.charObjField.should == 2
    jf.intObjField.should be_kind_of(Fixnum)
    jf.intObjField.should == 4
    jf.longObjField.should be_kind_of(Fixnum)
    jf.longObjField.should == 8
    
    jf.floatObjField.should be_kind_of(Float)
    jf.floatObjField.should == 4.5
    jf.doubleObjField.should be_kind_of(Float)
    jf.doubleObjField.should == 8.5
    
    jf.trueObjField.should be_kind_of(TrueClass)
    jf.trueObjField.should == true
    jf.falseObjField.should be_kind_of(FalseClass)
    jf.falseObjField.should == false
  end
end

describe "Java String, primitive, and object-typed interface methods" do
  it "should coerce or wrap to usable Ruby types for the implementer" do
    impl = Class.new {
      attr_accessor :result
      include ValueReceivingInterface
      
      def receiveObject(obj)
        self.result = obj
        obj
      end
      
      def receiveLongAndDouble(l, d)
        str = (l + d).to_s
        self.result = str
        str
      end
      
      %w[String Byte Short Char Int Long Float Double Null True False].each do |type|
        alias_method "receive#{type}".intern, :receiveObject
      end
    }
    
    vri = impl.new
    vri_handler = ValueReceivingInterfaceHandler.new(vri);
    
    obj = java.lang.Object.new
    vri_handler.receiveObject(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == java.lang.Object
    
    obj = "foo"
    vri_handler.receiveString(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == String
    
    obj = 1
    
    vri_handler.receiveByte(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Fixnum
    
    vri_handler.receiveShort(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Fixnum
    
    pending "char appears to be getting signed/unsigned-garbled" do
      vri_handler.receiveChar(obj).should == obj
      vri.result.should == obj
      vri.result.class.should == Fixnum
    end
    
    vri_handler.receiveInt(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Fixnum
    
    vri_handler.receiveLong(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Fixnum
    
    vri_handler.receiveFloat(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Float
    
    vri_handler.receiveDouble(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Float
    
    vri_handler.receiveNull(nil).should == nil
    vri.result.should == nil
    vri.result.class.should == NilClass
    
    vri_handler.receiveTrue(true).should == true
    vri.result.should == true
    vri.result.class.should == TrueClass
    
    vri_handler.receiveFalse(false).should == false
    vri.result.should == false
    vri.result.class.should == FalseClass
    
    vri_handler.receiveLongAndDouble(1, 1.0).should == "2.0"
    vri.result.should == "2.0"
    vri.result.class.should == String
  end
end

describe "Java primitive-box-typed interface methods" do
  it "should coerce to Ruby types for the implementer" do
    impl = Class.new {
      attr_accessor :result
      include ValueReceivingInterface

      def receiveByte(obj)
        self.result = obj
        obj
      end

      alias_method :receiveByteObj, :receiveByte

      %w[Short Char Int Long Float Double True False].each do |type|
        alias_method "receive#{type}".intern, :receiveByte
        alias_method "receive#{type}Obj".intern, :receiveByte
      end
    }

    vri = impl.new
    vri_handler = ValueReceivingInterfaceHandler.new(vri);

    obj = 1

    vri_handler.receiveByteObj(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Fixnum

    vri_handler.receiveShortObj(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Fixnum

    pending "char appears to be getting signed/unsigned-garbled" do
      vri_handler.receiveCharObj(obj).should == obj
      vri.result.should == obj
      vri.result.class.should == Fixnum
    end

    vri_handler.receiveIntObj(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Fixnum

    vri_handler.receiveLongObj(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Fixnum

    vri_handler.receiveFloatObj(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Float

    vri_handler.receiveDoubleObj(obj).should == obj
    vri.result.should == obj
    vri.result.class.should == Float

    vri_handler.receiveTrueObj(true).should == true
    vri.result.should == true
    vri.result.class.should == TrueClass

    vri_handler.receiveFalseObj(false).should == false
    vri.result.should == false
    vri.result.class.should == FalseClass
  end
end

describe "Java types with package or private constructors" do
  it "should not be constructible" do
    lambda { PackageConstructor.new }.should raise_error(TypeError)
    lambda { PrivateConstructor.new }.should raise_error(TypeError)
  end
end

describe "Java types with protected constructors" do
  it "should not be constructible" do
    lambda { ProtectedConstructor.new }.should raise_error(TypeError)
  end
end
