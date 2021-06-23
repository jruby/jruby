require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.CoreTypeMethods"
java_import "java_integration.fixtures.JavaFields"
java_import "java_integration.fixtures.ValueReceivingInterface"
java_import "java_integration.fixtures.ValueReceivingInterfaceHandler"

java_import "java_integration.fixtures.PackageConstructor"
java_import "java_integration.fixtures.ProtectedConstructor"
java_import "java_integration.fixtures.PrivateConstructor"

describe "Java String and primitive-typed methods" do
  it "should coerce to Ruby types when returned" do
    expect(CoreTypeMethods.getString).to be_kind_of(String)
    expect(CoreTypeMethods.getString).to eq("foo")

    expect(CoreTypeMethods.getByte).to be_kind_of(Integer)
    expect(CoreTypeMethods.getByte).to eq(1)
    expect(CoreTypeMethods.getShort).to be_kind_of(Integer)
    expect(CoreTypeMethods.getShort).to eq(2)
    expect(CoreTypeMethods.getChar).to be_kind_of(Integer)
    expect(CoreTypeMethods.getChar).to eq(2)
    expect(CoreTypeMethods.getInt).to be_kind_of(Integer)
    expect(CoreTypeMethods.getInt).to eq(4)
    expect(CoreTypeMethods.getLong).to be_kind_of(Integer)
    expect(CoreTypeMethods.getLong).to eq(8)

    expect(CoreTypeMethods.getFloat).to be_kind_of(Float)
    expect(CoreTypeMethods.getFloat).to eq(4.5)
    expect(CoreTypeMethods.getDouble).to be_kind_of(Float)
    expect(CoreTypeMethods.getDouble).to eq(8.5)

    expect(CoreTypeMethods.getBooleanTrue).to be_kind_of(TrueClass)
    expect(CoreTypeMethods.getBooleanTrue).to eq(true)
    expect(CoreTypeMethods.getBooleanFalse).to be_kind_of(FalseClass)
    expect(CoreTypeMethods.getBooleanFalse).to eq(false)

    expect(CoreTypeMethods.getNull).to be_kind_of(NilClass)
    expect(CoreTypeMethods.getNull).to eq(nil)

    expect(CoreTypeMethods.getVoid).to eq(nil)

    expect(CoreTypeMethods.getBigInteger).to eq(1234567890_1234567890_1234567890_1234567890)
  end

  it "should be coerced from Ruby types when passing parameters" do
    expect(CoreTypeMethods.setString("string")).to eq("string")

    expect(CoreTypeMethods.setByte(1)).to eq("1")
    expect(CoreTypeMethods.setShort(1)).to eq("1")
    expect(CoreTypeMethods.setChar(1)).to eq("\001")
    expect(CoreTypeMethods.setInt(1)).to eq("1")
    expect(CoreTypeMethods.setLong(1)).to eq("1")

    expect(CoreTypeMethods.setFloat(1)).to eq("1.0")
    expect(CoreTypeMethods.setDouble(1)).to eq("1.0")

    expect(CoreTypeMethods.setByte(1.5)).to eq("1")
    expect(CoreTypeMethods.setShort(1.5)).to eq("1")
    expect(CoreTypeMethods.setChar(1.5)).to eq("\001")
    expect(CoreTypeMethods.setInt(1.5)).to eq("1")
    expect(CoreTypeMethods.setLong(1.5)).to eq("1")

    expect(CoreTypeMethods.setFloat(1.5)).to eq("1.5")
    expect(CoreTypeMethods.setDouble(1.5)).to eq("1.5")

    expect(CoreTypeMethods.setBooleanTrue(true)).to eq("true")
    expect(CoreTypeMethods.setBooleanFalse(false)).to eq("false")

    expect(CoreTypeMethods.setByte(nil)).to eq("0")
    expect(CoreTypeMethods.setShort(nil)).to eq("0")
    expect(CoreTypeMethods.setChar(nil)).to eq("\000")
    expect(CoreTypeMethods.setInt(nil)).to eq("0")
    expect(CoreTypeMethods.setLong(nil)).to eq("0")

    expect(CoreTypeMethods.setFloat(nil)).to eq("0.0")
    expect(CoreTypeMethods.setDouble(nil)).to eq("0.0")

    expect(CoreTypeMethods.setBooleanTrue(nil)).to eq("false")
    expect(CoreTypeMethods.setBooleanFalse(nil)).to eq("false")

    expect(CoreTypeMethods.setBigInteger(1234567890123456789012345678901234567890)).to eq(
      "1234567890123456789012345678901234567890"
    )

    expect(CoreTypeMethods.setByteObj(1)).to eq("1")
    expect(CoreTypeMethods.setShortObj(1)).to eq("1")
    expect(CoreTypeMethods.setCharObj(1)).to eq("\001")
    expect(CoreTypeMethods.setIntObj(1)).to eq("1")
    expect(CoreTypeMethods.setLongObj(1)).to eq("1")

    expect(CoreTypeMethods.setFloatObj(1)).to eq("1.0")
    expect(CoreTypeMethods.setDoubleObj(1)).to eq("1.0")
    expect(CoreTypeMethods.setNumber(1)).to eq("java.lang.Long")
    expect(CoreTypeMethods.setSerializable(1)).to eq("java.lang.Long")

    expect(CoreTypeMethods.setByteObj(1.5)).to eq("1")
    expect(CoreTypeMethods.setShortObj(1.5)).to eq("1")
    expect(CoreTypeMethods.setCharObj(1.5)).to eq("\001")
    expect(CoreTypeMethods.setIntObj(1.5)).to eq("1")
    expect(CoreTypeMethods.setLongObj(1.5)).to eq("1")

    expect(CoreTypeMethods.setFloatObj(1.5)).to eq("1.5")
    expect(CoreTypeMethods.setDoubleObj(1.5)).to eq("1.5")
    expect(CoreTypeMethods.setNumber(1.5)).to eq("java.lang.Double")
    expect(CoreTypeMethods.setSerializable(1.5)).to eq("java.lang.Double")

    expect(CoreTypeMethods.setBooleanTrueObj(true)).to eq("true")
    expect(CoreTypeMethods.setBooleanFalseObj(false)).to eq("false")

    expect(CoreTypeMethods.setByteObj(nil)).to eq("null")
    expect(CoreTypeMethods.setShortObj(nil)).to eq("null")
    expect(CoreTypeMethods.setCharObj(nil)).to eq("null")
    expect(CoreTypeMethods.setIntObj(nil)).to eq("null")
    expect(CoreTypeMethods.setLongObj(nil)).to eq("null")

    expect(CoreTypeMethods.setFloatObj(nil)).to eq("null")
    expect(CoreTypeMethods.setDoubleObj(nil)).to eq("null")

    expect(CoreTypeMethods.setBooleanTrueObj(nil)).to eq("null")
    expect(CoreTypeMethods.setBooleanFalseObj(nil)).to eq("null")

    expect(CoreTypeMethods.setNull(nil)).to eq("null")
  end

  it "coerce from boxed Java types to primitives when passing parameters" do
    expect(CoreTypeMethods.setString("string".to_java(:string))).to eq("string")

    expect(CoreTypeMethods.setByte(1.to_java(:byte))).to eq("1")
    expect(CoreTypeMethods.setShort(1.to_java(:short))).to eq("1")
    expect(CoreTypeMethods.setChar(1.to_java(:char))).to eq("\001")
    expect(CoreTypeMethods.setInt(1.to_java(:int))).to eq("1")
    expect(CoreTypeMethods.setLong(1.to_java(:long))).to eq("1")

    expect(CoreTypeMethods.setFloat(1.to_java(:float))).to eq("1.0")
    expect(CoreTypeMethods.setDouble(1.to_java(:double))).to eq("1.0")

    expect(CoreTypeMethods.setByte(1.5.to_java(:byte))).to eq("1")
    expect(CoreTypeMethods.setShort(1.5.to_java(:short))).to eq("1")
    expect(CoreTypeMethods.setChar(1.5.to_java(:char))).to eq("\001")
    expect(CoreTypeMethods.setInt(1.5.to_java(:int))).to eq("1")
    expect(CoreTypeMethods.setLong(1.5.to_java(:long))).to eq("1")

    expect(CoreTypeMethods.setFloat(1.5.to_java(:float))).to eq("1.5")
    expect(CoreTypeMethods.setDouble(1.5.to_java(:double))).to eq("1.5")

    expect(CoreTypeMethods.setBooleanTrue(true.to_java(:boolean))).to eq("true")
    expect(CoreTypeMethods.setBooleanFalse(false.to_java(:boolean))).to eq("false")
  end

  it "should raise errors when passed values can not be precisely coerced" do
    expect { CoreTypeMethods.setByte(1 << 8) }.to raise_error(RangeError)
    expect { CoreTypeMethods.setShort(1 << 16) }.to raise_error(RangeError)
    expect { CoreTypeMethods.setChar(1 << 16) }.to raise_error(RangeError)
    expect { CoreTypeMethods.setInt(1 << 32) }.to raise_error(RangeError)
    expect { CoreTypeMethods.setLong(1 << 64) }.to raise_error(RangeError)
  end

  it "should select the method that matches precision of the incoming value" do
    expect(CoreTypeMethods.getType(1 << 32)).to eq("long")

    expect(CoreTypeMethods.getType(2.0 ** 128)).to eq("double")

    expect(CoreTypeMethods.getType("foo")).to eq("String")
  end
end

describe "Java Object-typed methods" do
  it "should coerce primitive Ruby types to a single, specific Java type" do
    expect(CoreTypeMethods.getObjectType("foo")).to eq("class java.lang.String")

    expect(CoreTypeMethods.getObjectType(0)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Byte::MAX_VALUE)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Byte::MIN_VALUE)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Byte::MAX_VALUE + 1)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Byte::MIN_VALUE - 1)).to eq("class java.lang.Long")

    expect(CoreTypeMethods.getObjectType(java::lang::Short::MAX_VALUE)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Short::MIN_VALUE)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Short::MAX_VALUE + 1)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Short::MIN_VALUE - 1)).to eq("class java.lang.Long")

    expect(CoreTypeMethods.getObjectType(java::lang::Integer::MAX_VALUE)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Integer::MIN_VALUE)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Integer::MAX_VALUE + 1)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Integer::MIN_VALUE - 1)).to eq("class java.lang.Long")

    expect(CoreTypeMethods.getObjectType(java::lang::Long::MAX_VALUE)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Long::MIN_VALUE)).to eq("class java.lang.Long")
    expect(CoreTypeMethods.getObjectType(java::lang::Long::MAX_VALUE + 1)).to eq("class java.math.BigInteger")
    expect(CoreTypeMethods.getObjectType(java::lang::Long::MIN_VALUE - 1)).to eq("class java.math.BigInteger")

    expect(CoreTypeMethods.getObjectType(java::lang::Float::MAX_VALUE)).to eq("class java.lang.Double")
    expect(CoreTypeMethods.getObjectType(java::lang::Float::MIN_VALUE)).to eq("class java.lang.Double")
    expect(CoreTypeMethods.getObjectType(-java::lang::Float::MAX_VALUE)).to eq("class java.lang.Double")
    expect(CoreTypeMethods.getObjectType(-java::lang::Float::MIN_VALUE)).to eq("class java.lang.Double")

    expect(CoreTypeMethods.getObjectType(java::lang::Float::NaN)).to eq("class java.lang.Double")
    expect(CoreTypeMethods.getObjectType(0.0)).to eq("class java.lang.Double")

    expect(CoreTypeMethods.getObjectType(java::lang::Double::MAX_VALUE)).to eq("class java.lang.Double")
    expect(CoreTypeMethods.getObjectType(java::lang::Double::MIN_VALUE)).to eq("class java.lang.Double")
    expect(CoreTypeMethods.getObjectType(-java::lang::Double::MAX_VALUE)).to eq("class java.lang.Double")
    expect(CoreTypeMethods.getObjectType(-java::lang::Double::MIN_VALUE)).to eq("class java.lang.Double")

    expect(CoreTypeMethods.getObjectType(true)).to eq("class java.lang.Boolean")
    expect(CoreTypeMethods.getObjectType(1 << 128)).to eq("class java.math.BigInteger")
  end

  it "passes coerced to_java values - keeping the Java type" do
    expect(CoreTypeMethods.getObjectType('foo'.to_java)).to eq "class java.lang.String"
    expect(CoreTypeMethods.getObjectType(0.to_java)).to eq "class java.lang.Long"
    expect(CoreTypeMethods.getObjectType(1.to_java(:int))).to eq "class java.lang.Integer"
    expect(CoreTypeMethods.getObjectType(1.to_java(:byte))).to eq "class java.lang.Byte"
    expect(CoreTypeMethods.getObjectType(0.1.to_java(:double))).to eq "class java.lang.Double"
    expect(CoreTypeMethods.getObjectType(0.1.to_java('java.lang.Float'))).to eq "class java.lang.Float"
    expect(CoreTypeMethods.getObjectType(false.to_java)).to eq "class java.lang.Boolean"
  end
end

describe "Java String and primitive-typed fields" do
  it "coerce to Ruby types when retrieved" do
    # static
    expect(JavaFields.stringStaticField).to be_kind_of(String)
    expect(JavaFields.stringStaticField).to eq('000');

    expect(JavaFields.byteStaticField).to be_kind_of(Integer)
    expect(JavaFields.byteStaticField).to eq(1)
    expect(JavaFields.shortStaticField).to be_kind_of(Integer)
    expect(JavaFields.shortStaticField).to eq(2)
    expect(JavaFields.charStaticField).to be_kind_of(Integer)
    expect(JavaFields.charStaticField).to eq('3'.ord)
    expect(JavaFields.intStaticField).to be_kind_of(Integer)
    expect(JavaFields.intStaticField).to eq(4)
    expect(JavaFields.longStaticField).to be_kind_of(Integer)
    expect(JavaFields.longStaticField).to eq(5)

    expect(JavaFields.floatStaticField).to be_kind_of(Float)
    expect(JavaFields.floatStaticField).to eq(6.0)
    expect(JavaFields.doubleStaticField).to be_kind_of(Float)
    expect(JavaFields.doubleStaticField).to eq(7.2)

    expect(JavaFields.trueStaticField).to be_kind_of(TrueClass)
    expect(JavaFields.trueStaticField).to eq(true)
    expect(JavaFields.falseStaticField).to be_kind_of(FalseClass)
    expect(JavaFields.falseStaticField).to eq(false)

    expect(JavaFields.nullStaticField).to be_kind_of(NilClass)
    expect(JavaFields.nullStaticField).to eq(nil)

    expect(JavaFields.bigIntegerStaticField).to be_kind_of(Integer) # Bignum
    expect(JavaFields.bigIntegerStaticField).to eq(111_111_111_111_111_111_110)

    # instance
    jf = JavaFields.new
    expect(jf.stringField).to be_kind_of(String)
    expect(jf.stringField).to eq("000");

    expect(jf.byteField).to be_kind_of(Integer)
    expect(jf.byteField).to eq(1)
    expect(jf.shortField).to be_kind_of(Integer)
    expect(jf.shortField).to eq(2)
    expect(jf.charField).to be_kind_of(Integer)
    expect(jf.charField).to eq(84)
    expect(jf.intField).to be_kind_of(Integer)
    expect(jf.intField).to eq(4)
    expect(jf.longField).to be_kind_of(Integer)
    expect(jf.longField).to eq(5)

    expect(jf.floatField).to be_kind_of(Float)
    expect(jf.floatField).to eq(6.0)
    expect(jf.doubleField).to be_kind_of(Float)
    expect(jf.doubleField).to eq(7.2)

    expect(jf.trueField).to be_kind_of(TrueClass)
    expect(jf.trueField).to eq(true)
    expect(jf.falseField).to be_kind_of(FalseClass)
    expect(jf.falseField).to eq(false)

    expect(jf.nullField).to be_kind_of(NilClass)
    expect(jf.nullField).to eq(nil)

    expect(jf.bigIntegerField).to be_kind_of(Integer) # Bignum
    expect(jf.bigIntegerField).to eq(111_111_111_111_111_111_111)
  end
end

describe "Java primitive-box-typed fields" do
  it "coerce to Ruby types when retrieved" do
    # static
    expect(JavaFields.byteObjStaticField).to be_kind_of(Integer)
    expect(JavaFields.byteObjStaticField).to eq(1)
    expect(JavaFields.shortObjStaticField).to be_kind_of(Integer)
    expect(JavaFields.shortObjStaticField).to eq(2)
    expect(JavaFields.charObjStaticField).to be_kind_of(Integer)
    expect(JavaFields.charObjStaticField).to eq('3'.ord)
    expect(JavaFields.intObjStaticField).to be_kind_of(Integer)
    expect(JavaFields.intObjStaticField).to eq(4)
    expect(JavaFields.longObjStaticField).to be_kind_of(Integer)
    expect(JavaFields.longObjStaticField).to eq(5)

    expect(JavaFields.floatObjStaticField).to be_kind_of(Float)
    expect(JavaFields.floatObjStaticField).to eq(6.0)
    expect(JavaFields.doubleObjStaticField).to be_kind_of(Float)
    expect(JavaFields.doubleObjStaticField).to eq(7.2)

    expect(JavaFields.trueObjStaticField).to be_kind_of(TrueClass)
    expect(JavaFields.trueObjStaticField).to eq(true)
    expect(JavaFields.falseObjStaticField).to be_kind_of(FalseClass)
    expect(JavaFields.falseObjStaticField).to eq(false)

    # instance
    jf = JavaFields.new
    expect(jf.byteObjField).to be_kind_of(Integer)
    expect(jf.byteObjField).to eq(1)
    expect(jf.shortObjField).to be_kind_of(Integer)
    expect(jf.shortObjField).to eq(2)
    expect(jf.charObjField).to be_kind_of(Integer)
    expect(jf.charObjField).to eq('T'.ord)
    expect(jf.intObjField).to be_kind_of(Integer)
    expect(jf.intObjField).to eq(4)
    expect(jf.longObjField).to be_kind_of(Integer)
    expect(jf.longObjField).to eq(5)

    expect(jf.floatObjField).to be_kind_of(Float)
    expect(jf.floatObjField).to eq(6.0)
    expect(jf.doubleObjField).to be_kind_of(Float)
    expect(jf.doubleObjField).to eq(7.2)

    expect(jf.trueObjField).to be_kind_of(TrueClass)
    expect(jf.trueObjField).to eq(true)
    expect(jf.falseObjField).to be_kind_of(FalseClass)
    expect(jf.falseObjField).to eq(false)
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
    expect(vri_handler.receiveObject(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(java.lang.Object)

    obj = "foo"
    expect(vri_handler.receiveString(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(String)

    obj = 1

    expect(vri_handler.receiveByte(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveShort(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveChar(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveInt(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveLong(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveFloat(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Float)

    expect(vri_handler.receiveDouble(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Float)

    expect(vri_handler.receiveNull(nil)).to eq(nil)
    expect(vri.result).to eq(nil)
    expect(vri.result.class).to eq(NilClass)

    expect(vri_handler.receiveTrue(true)).to eq(true)
    expect(vri.result).to eq(true)
    expect(vri.result.class).to eq(TrueClass)

    expect(vri_handler.receiveFalse(false)).to eq(false)
    expect(vri.result).to eq(false)
    expect(vri.result.class).to eq(FalseClass)

    expect(vri_handler.receiveLongAndDouble(1, 1.0)).to eq("2.0")
    expect(vri.result).to eq("2.0")
    expect(vri.result.class).to eq(String)
  end
end

describe "Java primitive-typed interface methods" do
  it "should coerce nil to zero-magnitude primitives" do
    impl = Class.new {
      attr_accessor :result
      include ValueReceivingInterface

      def receive_primitive(obj)
        self.result = obj
        nil
      end

      %w[Byte Short Char Int Long Float Double Null True False].each do |type|
        alias_method "receive#{type}".intern, :receive_primitive
      end
    }

    vri = impl.new
    vri_handler = ValueReceivingInterfaceHandler.new(vri);

    expect(vri_handler.receiveByte(nil)).to eq(0)
    expect(vri.result).to eq(0)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveShort(nil)).to eq(0)
    expect(vri.result).to eq(0)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveChar(nil)).to eq(0)
    expect(vri.result).to eq(0)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveInt(nil)).to eq(0)
    expect(vri.result).to eq(0)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveLong(nil)).to eq(0)
    expect(vri.result).to eq(0)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveFloat(nil)).to eq(0.0)
    expect(vri.result).to eq(0.0)
    expect(vri.result.class).to eq(Float)

    expect(vri_handler.receiveDouble(nil)).to eq(0.0)
    expect(vri.result).to eq(0.0)
    expect(vri.result.class).to eq(Float)

    expect(vri_handler.receiveTrue(nil)).to eq(false)
    expect(vri.result).to eq(false)
    expect(vri.result.class).to eq(FalseClass)

    expect(vri_handler.receiveFalse(nil)).to eq(false)
    expect(vri.result).to eq(false)
    expect(vri.result.class).to eq(FalseClass)
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

    expect(vri_handler.receiveByteObj(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveShortObj(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveCharObj(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveIntObj(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveLongObj(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Integer)

    expect(vri_handler.receiveFloatObj(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Float)

    expect(vri_handler.receiveDoubleObj(obj)).to eq(obj)
    expect(vri.result).to eq(obj)
    expect(vri.result.class).to eq(Float)

    expect(vri_handler.receiveTrueObj(true)).to eq(true)
    expect(vri.result).to eq(true)
    expect(vri.result.class).to eq(TrueClass)

    expect(vri_handler.receiveFalseObj(false)).to eq(false)
    expect(vri.result).to eq(false)
    expect(vri.result.class).to eq(FalseClass)
  end

  it "should coerce to null" do
    impl = Class.new {
      attr_accessor :result
      include ValueReceivingInterface

      def receive_primitive_box(obj)
        self.result = obj
        nil
      end

      %w[Byte Short Char Int Long Float Double True False].each do |type|
        alias_method :"receive#{type}Obj", :receive_primitive_box
      end
    }

    vri = impl.new
    vri_handler = ValueReceivingInterfaceHandler.new(vri);

    obj = 1

    expect(vri_handler.receiveByteObj(nil)).to eq(nil)
    expect(vri.result).to eq(nil)
    expect(vri.result.class).to eq(NilClass)

    expect(vri_handler.receiveShortObj(nil)).to eq(nil)
    expect(vri.result).to eq(nil)
    expect(vri.result.class).to eq(NilClass)

    expect(vri_handler.receiveCharObj(nil)).to eq(nil)
    expect(vri.result).to eq(nil)
    expect(vri.result.class).to eq(NilClass)

    expect(vri_handler.receiveIntObj(nil)).to eq(nil)
    expect(vri.result).to eq(nil)
    expect(vri.result.class).to eq(NilClass)

    expect(vri_handler.receiveLongObj(nil)).to eq(nil)
    expect(vri.result).to eq(nil)
    expect(vri.result.class).to eq(NilClass)

    expect(vri_handler.receiveFloatObj(nil)).to eq(nil)
    expect(vri.result).to eq(nil)
    expect(vri.result.class).to eq(NilClass)

    expect(vri_handler.receiveDoubleObj(nil)).to eq(nil)
    expect(vri.result).to eq(nil)
    expect(vri.result.class).to eq(NilClass)

    expect(vri_handler.receiveTrueObj(nil)).to eq(nil)
    expect(vri.result).to eq(nil)
    expect(vri.result.class).to eq(NilClass)

    expect(vri_handler.receiveFalseObj(nil)).to eq(nil)
    expect(vri.result).to eq(nil)
    expect(vri.result.class).to eq(NilClass)
  end
end

describe "Java types with package or private constructors" do
  it "should not be constructible" do
    expect { PackageConstructor.new }.to raise_error(TypeError)
    expect { PrivateConstructor.new }.to raise_error(TypeError)
  end
end

describe "Java types with protected constructors" do
  it "should not be constructible" do
    expect { ProtectedConstructor.new }.to raise_error(TypeError)
  end
end

describe "Fixnum\#to_java" do
  it "should coerce to java.lang.Long by default" do
    long = 123.to_java
    expect(long.class).to eq(java.lang.Long)
  end

  it "should allow coercing to other primitive types using symbolic names" do
    byte = 123.to_java :byte
    short = 123.to_java :short
    char = 123.to_java :char
    int = 123.to_java :int
    long = 123.to_java :long
    float = 123.to_java :float
    double = 123.to_java :double

    expect(byte.class).to eq(java.lang.Byte)
    expect(short.class).to eq(java.lang.Short)
    expect(char.class).to eq(java.lang.Character)
    expect(int.class).to eq(java.lang.Integer)
    expect(long.class).to eq(java.lang.Long)
    expect(float.class).to eq(java.lang.Float)
    expect(double.class).to eq(java.lang.Double)
  end

  it "coerces to java.lang.Long when asked to coerce to java.lang.Object" do
    obj = 123.to_java java.lang.Object
    obj2 = 123.to_java :object

    expect(obj.class).to eq(java.lang.Long)
    expect(obj2.class).to eq(java.lang.Long)
  end

  it "should allow coercing to other primitive types using boxed classes" do
    byte = 123.to_java java.lang.Byte
    short = 123.to_java java.lang.Short
    char = 123.to_java java.lang.Character
    int = 123.to_java java.lang.Integer
    long = 123.to_java java.lang.Long
    float = 123.to_java java.lang.Float
    double = 123.to_java java.lang.Double

    expect(byte.class).to eq(java.lang.Byte)
    expect(short.class).to eq(java.lang.Short)
    expect(char.class).to eq(java.lang.Character)
    expect(int.class).to eq(java.lang.Integer)
    expect(long.class).to eq(java.lang.Long)
    expect(float.class).to eq(java.lang.Float)
    expect(double.class).to eq(java.lang.Double)
  end

  it "should allow coercing to other primitive types using boxed classes" do
    byte = 123.to_java Java::byte
    short = 123.to_java Java::short
    char = 123.to_java Java::char
    int = 123.to_java Java::int
    long = 123.to_java Java::long
    float = 123.to_java Java::float
    double = 123.to_java Java::double

    expect(byte.class).to eq(java.lang.Byte)
    expect(short.class).to eq(java.lang.Short)
    expect(char.class).to eq(java.lang.Character)
    expect(int.class).to eq(java.lang.Integer)
    expect(long.class).to eq(java.lang.Long)
    expect(float.class).to eq(java.lang.Float)
    expect(double.class).to eq(java.lang.Double)
  end
end

describe "String#to_java" do
  it "coerces to java.lang.String by default" do
    str = "123".to_java
    expect(str.class).to eq(java.lang.String)
  end

  describe "when passed java.lang.String" do
    it "coerces to java.lang.String" do
      cs = "123".to_java java.lang.String

      expect(cs.class).to eq(java.lang.String)
    end
  end

  describe "when passed java.lang.CharSequence" do
    it "returns a RubyString" do
      cs = "123".to_java java.lang.CharSequence

      expect(cs.class).to eq(org.jruby.RubyString)
    end
  end

  describe "when passed java.lang.Object" do
    it "coerces to java.lang.String" do
      cs = "123".to_java java.lang.Object

      expect(cs.class).to eq(java.lang.String)
    end
  end

  describe "when passed org.jruby.util.ByteList" do
    it "coerces to java.lang.String" do
      cs = "123".to_java 'org.jruby.util.ByteList'

      expect(cs.class).to eq(org.jruby.util.ByteList)
      expect(cs.toString).to eq('123')
    end
  end

  describe "when passed void (java.lang.Void.TYPE)" do
    it "coerces to null" do
      cs = "123".to_java Java::java.lang.Void::TYPE

      expect(cs.class).to eq(NilClass)
    end
  end
end

describe "Class#to_java" do
  describe "when passed java.lang.Class.class" do
    cls = java.lang.Class

    # NOTE: moved to a Java test @see org.jruby.javasupport.TestJavaClass#testToJava
    # it "coerces core classes to their Java class object" do
    #   [Object, Array, String, Hash, File, IO].each do |rubycls|
    #     expect(rubycls.to_java(cls)).to eq(eval("cls.forName('org.jruby.Ruby#{rubycls}')"))
    #   end
    # end

    class UserKlass < Object; end

    it "reifies user class on-demand" do
      expect(klass = UserKlass.to_java(cls)).to be_a java.lang.Class
      expect( klass.getSuperclass ).to be cls.forName('org.jruby.RubyObject')
    end

    it "returns reified class for reified used classes" do
      rubycls = Class.new; require 'jruby/core_ext'
      rubycls.become_java!
      expect(rubycls.to_java(cls)).to be JRuby.reference(rubycls).getReifiedRubyClass
    end

    it "converts Java proxy classes to their JavaClass/java.lang.Class equivalent" do
      expect(java.util.ArrayList.to_java(cls)).to eq(java.util.ArrayList.java_class)
    end
  end

  describe "when passed java.lang.Object.class" do

    # NOTE: moved to a Java test @see org.jruby.javasupport.TestJavaClass#testToJavaObject
    # it "coerces core classes to their Ruby class object" do
    #   [Object, Array, String, Hash, File, IO].each do |rubycls|
    #     expect(rubycls.to_java(java.lang.Object)).to eq(rubycls)
    #   end
    #   BasicObject.to_java(java.lang.Object).should == BasicObject if defined? BasicObject
    #   [Bignum, Dir, ENV, FalseClass, Integer, Float, Kernel, Struct, Symbol, Thread].each do |clazz|
    #     expect(clazz.to_java(java.lang.Object)).to eq(clazz)
    #   end
    #   expect(Exception.to_java(java.lang.Object)).to eq Exception
    #   expect(StandardError.to_java(java.lang.Object)).to eq StandardError
    # end

    it "coerces user classes/modules to their Ruby class object" do
      clazz = Class.new
      expect(clazz.to_java(java.lang.Object)).to eq(clazz)
      clazz = Module.new
      expect(clazz.to_java(java.lang.Object)).to eq(clazz)
    end

    it "converts Java proxy classes to their proxy class (Ruby class) equivalent" do
      expect(java.util.ArrayList.to_java(java.lang.Object)).to eq(java.util.ArrayList)
    end
  end
end

describe "Time#to_java" do
  describe "when passed java.util.Date" do
    it "coerces to java.util.Date" do
      t = Time.now
      d = t.to_java(java.util.Date)
      expect(d.class).to eq(java.util.Date)
    end
  end

  describe "when passed java.util.Calendar" do
    it "coerces to java.util.Calendar" do
      t = Time.now
      d = t.to_java(java.util.Calendar)
      expect(d.class).to be < java.util.Calendar
    end
  end

  describe 'java.sql date types' do
    it "coerces to java.sql.Date" do
      t = Time.now
      d = t.to_java(java.sql.Date)
      expect(d.class).to eq(java.sql.Date)
    end

    it "coerces to java.sql.Time" do
      t = Time.now
      d = t.to_java(java.sql.Time)
      expect(d.class).to eq(java.sql.Time)
    end

    it "coerces to java.sql.Timestamp" do
      t = Time.now
      d = t.to_java(java.sql.Timestamp)
      expect(d.class).to eq(java.sql.Timestamp)
    end
  end

  describe "when passed java.lang.Object" do
    it "coerces to java.util.Date" do
      t = Time.now
      d = t.to_java(java.lang.Object)
      expect(d.class).to eq(java.util.Date)
    end
  end

  describe "when passed java.io.Serializable" do
    it "returns RubyTime instance" do
      t = Time.at(0)
      expect(t.to_java('java.io.Serializable').class).to eq(org.jruby.RubyTime)
    end
  end

  describe 'joda types' do
    it "coerces to org.joda.time.DateTime" do
      t = Time.at(0)
      d = t.to_java(org.joda.time.DateTime)
      expect(d.class).to eq(org.joda.time.DateTime)
    end

    it "coerces to DateTime from ReadableDateTime interface" do
      t = Time.now
      d = t.to_java(org.joda.time.ReadableDateTime)
      expect(d.class).to eq(org.joda.time.DateTime)
    end
  end

  describe 'java 8 types' do
    it "coerces to Instant" do
      t = Time.at(0)
      expect(t.to_java(java.time.Instant).class).to eq(java.time.Instant)

      t = Time.new(2019, 04, 18, 14, 30, (50 * 1_000_000_000 + 123456780) / 1_000_000_000r, '-03:00')
      j = java.time.ZonedDateTime.of(2019, 04, 18, 14, 30, 50, 123456780, java.time.ZoneId.of('-03:00'))
      expect(t.to_java(java.time.Instant)).to eq(j.toInstant)
    end

    it "coerces a Temporal to Instant" do
      t = Time.at(0, 123456.789)
      d = t.to_java(java.time.temporal.Temporal)
      expect(d.class).to eq(java.time.Instant)
      expect(d.to_s).to eq('1970-01-01T00:00:00.123456789Z')
    end

    it "coerces to LocalDateTime" do
      t = Time.new(2002, 10, 31, 12, 24, 48)
      d = t.to_java(java.time.LocalDateTime)
      expect(d.class).to eq(java.time.LocalDateTime)
      expect(d).to eq(java.time.LocalDateTime.of(2002, 10, 31, 12, 24, 48))
    end

    it "coerces to ZonedDateTime" do
      t = Time.new(2018, 10, 31, 10, 20, 50.123456, '+02:00')
      d = t.to_java(java.time.chrono.ChronoZonedDateTime)
      expect(d.class).to eq(java.time.ZonedDateTime)
      expect(d).to eq(java.time.ZonedDateTime.of(2018, 10, 31, 10, 20, 50, 123456 * 1000, java.time.ZoneId.of('+02:00')))
    end
  end
end

describe "Date/DateTime#to_java" do

  before(:all) { require 'date' }

  describe "when passed java.util.Date" do
    it "coerces to java.util.Date" do
      t = Date.today
      d = t.to_java(java.util.Date)
      expect(d.class).to eq(java.util.Date)
    end
  end

  describe "when passed java.util.Calendar" do
    it "coerces to java.util.Calendar" do
      t = Date.today
      d = t.to_java(java.util.Calendar)
      expect(d.class).to be < java.util.Calendar
    end
  end

  describe 'java.sql date types' do
    it "coerces to java.sql.Date" do
      t = Date.today
      d = t.to_java(java.sql.Date)
      expect(d.class).to eq(java.sql.Date)
    end

    it "coerces to java.sql.Time" do
      t = Date.today
      d = t.to_java(java.sql.Time)
      expect(d.class).to eq(java.sql.Time)
    end

    it "coerces to java.sql.Timestamp" do
      t = Date.today
      d = t.to_java(java.sql.Timestamp)
      expect(d.class).to eq(java.sql.Timestamp)
    end
  end

  describe "when passed java.lang.Object" do
    it "coerces to java.util.Date" do
      t = Date.today
      d = t.to_java(java.lang.Object)
      expect(d.class).to eq(java.util.Date)
    end
  end

  describe "when passed java.io.Serializable" do
    it "returns RubyTime instance" do
      t = Date.new
      expect(t.to_java('java.io.Serializable').class.to_java.name).to eq('Java::OrgJrubyExtDate::RubyDate')
    end
  end

  describe 'joda types' do
    it "coerces to org.joda.time.DateTime" do
      t = Date.new(0)
      d = t.to_java(org.joda.time.DateTime)
      expect(d.class).to eq(org.joda.time.DateTime)
    end

    it "coerces to DateTime from ReadableDateTime interface" do
      t = Date.today
      d = t.to_java(org.joda.time.ReadableDateTime)
      expect(d.class).to eq(org.joda.time.DateTime)

      t = DateTime.now
      d = t.to_java(org.joda.time.ReadableDateTime)
      expect(d.class).to eq(org.joda.time.DateTime)
    end
  end

  describe 'java 8 types' do
    it "coerces to Instant" do
      t = Date.new(0)
      expect(t.to_java(java.time.Instant).class).to eq(java.time.Instant)
      local_date = java.time.LocalDate.of(-1, 12, 30) # joda-time vs ruby-date rules
      expect(t.to_java(java.time.Instant)).to eq(local_date.atTime(0, 0).toInstant(java.time.ZoneOffset::UTC))

      t = Time.new(1970, 1, 1, 00, 00, 42, '+00:00').to_datetime
      expect(t.to_java(java.time.Instant).class).to eq(java.time.Instant)
      expect(t.to_java(java.time.Instant)).to eq(java.time.Instant::EPOCH.plusSeconds(42))
    end

    it "coerces a Temporal to Instant" do
      t = Time.at(0, 123456.789).to_datetime
      d = t.to_java(java.time.temporal.Temporal)
      expect(d.class).to eq(java.time.Instant)
      expect(d.to_s).to eq('1970-01-01T00:00:00.123456789Z')
    end

    it "coerces to LocalDate" do
      t = Time.new(2002, 10, 31, 12, 24, 48).to_date
      d = t.to_java(java.time.LocalDate)
      expect(d.class).to eq(java.time.LocalDate)
      expect(d).to eq(java.time.LocalDate.of(2002, 10, 31))
    end

    it "coerces to LocalDateTime" do
      t = Time.new(2002, 10, 31, 12, 24, 48).to_datetime
      d = t.to_java(java.time.LocalDateTime)
      expect(d.class).to eq(java.time.LocalDateTime)
      expect(d).to eq(java.time.LocalDateTime.of(2002, 10, 31, 12, 24, 48))
    end
  end
end

describe "java.time.Instant#to_time" do
  it 'works' do # NOTE: write a better spec
    t = Time.now
    expect(t.to_java('java.time.Instant').to_time).to eq t
  end
end

describe "java.time.LocalDateTime#to_time" do
  it 'works' do # NOTE: write a better spec
    t = Time.now
    expect(t.to_java('java.time.LocalDateTime').to_time).to eq t
  end
end

describe "java.time.OffsetDateTime#to_time" do
  it 'works' do # NOTE: write a better spec
    t = Time.now
    expect(t.to_java('java.time.OffsetDateTime').to_time).to eq t
  end
end

describe "java.time.ZonedDateTime#to_time" do
  it 'works' do # NOTE: write a better spec
    t = Time.now
    expect(t.to_java('java.time.ZonedDateTime').to_time).to eq t
  end
end

describe "A Rational object" do
  before :each do
    @rational = Rational(1,2)
  end

  it "is left uncoerced with to_java" do
    expect(@rational.to_java).to eq(@rational)
  end

  it "fails to coerce to types not assignable from the given type" do
    expect do
      @rational.to_java(java.lang.String)
    end.to raise_error(TypeError)
  end
end

describe "A Complex object" do
  before :each do
    @complex = Complex(1,2)
  end

  it "is left uncoerced with to_java" do
    expect(@complex.to_java).to eq(@complex)
  end

  it "fails to coerce to types not assignable from the given type" do
    expect do
      @complex.to_java(java.lang.String)
    end.to raise_error(TypeError)
  end
end
