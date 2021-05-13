require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ArrayReceiver"
java_import "java_integration.fixtures.ArrayReturningInterface"
java_import "java_integration.fixtures.ArrayReturningInterfaceConsumer"
java_import "java_integration.fixtures.PublicConstructor"
java_import "java_integration.fixtures.ProtectedConstructor"
java_import "java_integration.fixtures.PackageConstructor"
java_import "java_integration.fixtures.PrivateConstructor"

describe "A Java primitive Array of type" do
  describe "boolean" do 
    it "should be possible to create empty array" do 
      arr = Java::boolean[0].new
      expect(arr.java_class.name).to eq("[Z")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::boolean[10].new
      expect(arr.java_class.name).to eq("[Z")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::boolean[10,10].new
      expect(arr.java_class.name).to eq("[[Z")
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [true, false].to_java :boolean
      expect(arr.java_class.name).to eq("[Z")

      expect(arr.length).to eq(2)

      expect(arr[0]).to be true
      expect(arr[1]).to be_falsey


      # Check with type
      arr = [true, false].to_java Java::boolean
      expect(arr.java_class.name).to eq("[Z")

      expect(arr.length).to eq(2)

      expect(arr[0]).to be true
      expect(arr[1]).to be_falsey
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::boolean[5].new
      arr[3] = true
      
      expect(arr[0]).to be_falsey
      expect(arr[1]).to be_falsey
      expect(arr[2]).to be_falsey
      expect(arr[3]).to be true
      expect(arr[4]).to be_falsey
    end

    it "should be possible to get values from primitive array" do 
      arr = [false, true, false].to_java :boolean
      expect(arr[0]).to be_falsey
      expect(arr[1]).to be true
      expect(arr[2]).to be_falsey
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [false, true, false].to_java :boolean
      ret = ArrayReceiver::call_with_boolean(arr)
      expect(ret.to_a).to eq([false, true, false])
    end
  end

  describe "byte" do 
    it "should be possible to create empty array" do 
      arr = Java::byte[0].new
      expect(arr.java_class.name).to eq("[B")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::byte[10].new
      expect(arr.java_class.name).to eq("[B")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::byte[10,10].new
      expect(arr.java_class.name).to eq("[[B")
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1,2].to_java :byte
      expect(arr.java_class.name).to eq("[B")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::byte
      expect(arr.java_class.name).to eq("[B")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::byte[5].new
      arr[0] = 12
      arr[1] = 20
      arr[2] = 42
      
      expect(arr[0]).to eq(12)
      expect(arr[1]).to eq(20)
      expect(arr[2]).to eq(42)
      expect(arr[3]).to eq(0)
      expect(arr[4]).to eq(0)
    end

    it "should be possible to get values from primitive array" do 
      arr = [13, 42, 120].to_java :byte
      expect(arr[0]).to eq(13)
      expect(arr[1]).to eq(42)
      expect(arr[2]).to eq(120)
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13, 42, 120].to_java :byte
      ret = ArrayReceiver::call_with_byte(arr)
      expect(ret.to_a).to eq([13, 42, 120])
    end
  end

  describe "char" do 
    it "should be possible to create empty array" do 
      arr = Java::char[0].new
      expect(arr.java_class.name).to eq("[C")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::char[10].new
      expect(arr.java_class.name).to eq("[C")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::char[10,10].new
      expect(arr.java_class.name).to eq("[[C")
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1,2].to_java :char
      expect(arr.java_class.name).to eq("[C")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::char
      expect(arr.java_class.name).to eq("[C")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::char[5].new
      arr[0] = 12
      arr[1] = 20
      arr[2] = 42
      
      expect(arr[0]).to eq(12)
      expect(arr[1]).to eq(20)
      expect(arr[2]).to eq(42)
      expect(arr[3]).to eq(0)
      expect(arr[4]).to eq(0)
    end

    it "should be possible to get values from primitive array" do 
      arr = [13, 42, 120].to_java :char
      expect(arr[0]).to eq(13)
      expect(arr[1]).to eq(42)
      expect(arr[2]).to eq(120)
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13, 42, 120].to_java :char
      ret = ArrayReceiver::call_with_char(arr)
      expect(ret.to_a).to eq([13, 42, 120])
    end
  end

  describe "double" do 
    it "should be possible to create empty array" do 
      arr = Java::double[0].new
      expect(arr.java_class.name).to eq("[D")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::double[10].new
      expect(arr.java_class.name).to eq("[D")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::double[10,10].new
      expect(arr.java_class.name).to eq("[[D")
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1.2,2.3].to_java :double
      expect(arr.java_class.name).to eq("[D")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1.2)
      expect(arr[1]).to eq(2.3)


      # Check with type
      arr = [1.2,2.3].to_java Java::double
      expect(arr.java_class.name).to eq("[D")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1.2)
      expect(arr[1]).to eq(2.3)
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::double[5].new
      arr[0] = 12.2
      arr[1] = 20.3
      arr[2] = 42.4
      
      expect(arr[0]).to eq(12.2)
      expect(arr[1]).to eq(20.3)
      expect(arr[2]).to eq(42.4)
      expect(arr[3]).to eq(0.0)
      expect(arr[4]).to eq(0.0)
    end

    it "should be possible to get values from primitive array" do 
      arr = [13.2, 42.3, 120.4].to_java :double
      expect(arr[0]).to eq(13.2)
      expect(arr[1]).to eq(42.3)
      expect(arr[2]).to eq(120.4)
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13.2, 42.3, 120.4].to_java :double
      ret = ArrayReceiver::call_with_double(arr)
      expect(ret.to_a).to eq([13.2, 42.3, 120.4])
    end
  end

  describe "float" do 
    it "should be possible to create empty array" do 
      arr = Java::float[0].new
      expect(arr.java_class.name).to eq("[F")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::float[10].new
      expect(arr.java_class.name).to eq("[F")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::float[10,10].new
      expect(arr.java_class.name).to eq("[[F")
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1.2,2.3].to_java :float
      expect(arr.java_class.name).to eq("[F")

      expect(arr.length).to eq(2)

      expect(arr[0]).to be_within(0.00001).of(1.2)
      expect(arr[1]).to be_within(0.00001).of(2.3)


      # Check with type
      arr = [1.2,2.3].to_java Java::float
      expect(arr.java_class.name).to eq("[F")

      expect(arr.length).to eq(2)

      expect(arr[0]).to be_within(0.00001).of(1.2)
      expect(arr[1]).to be_within(0.00001).of(2.3)
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::float[5].new
      arr[0] = 12.2
      arr[1] = 20.3
      arr[2] = 42.4
      
      expect(arr[0]).to be_within(0.00001).of(12.2)
      expect(arr[1]).to be_within(0.00001).of(20.3)
      expect(arr[2]).to be_within(0.00001).of(42.4)
      expect(arr[3]).to eq(0.0)
      expect(arr[4]).to eq(0.0)
    end

    it "should be possible to get values from primitive array" do 
      arr = [13.2, 42.3, 120.4].to_java :float

      expect(arr[0]).to be_within(0.00001).of(13.2)
      expect(arr[1]).to be_within(0.00001).of(42.3)
      expect(arr[2]).to be_within(0.00001).of(120.4)
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13.2, 42.3, 120.4].to_java :float
      ret = ArrayReceiver::call_with_float(arr)
      expect(ret.length).to eq(3)
      expect(ret[0]).to be_within(0.00001).of(13.2)
      expect(ret[1]).to be_within(0.00001).of(42.3)
      expect(ret[2]).to be_within(0.00001).of(120.4)
    end
  end

  describe "int" do 
    it "should be possible to create empty array" do 
      arr = Java::int[0].new
      expect(arr.java_class.name).to eq("[I")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::int[10].new
      expect(arr.java_class.name).to eq("[I")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::int[10,10].new
      expect(arr.java_class.name).to eq("[[I")
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1,2].to_java :int
      expect(arr.java_class.name).to eq("[I")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::int
      expect(arr.java_class.name).to eq("[I")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::int[5].new
      arr[0] = 12
      arr[1] = 20
      arr[2] = 42
      
      expect(arr[0]).to eq(12)
      expect(arr[1]).to eq(20)
      expect(arr[2]).to eq(42)
      expect(arr[3]).to eq(0)
      expect(arr[4]).to eq(0)
    end

    it "should be possible to get values from primitive array" do 
      arr = [13, 42, 120].to_java :int
      expect(arr[0]).to eq(13)
      expect(arr[1]).to eq(42)
      expect(arr[2]).to eq(120)
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13, 42, 120].to_java :int
      ret = ArrayReceiver::call_with_int(arr)
      expect(ret.to_a).to eq([13, 42, 120])
    end
  end

  describe "long" do 
    it "should be possible to create empty array" do 
      arr = Java::long[0].new
      expect(arr.java_class.name).to eq("[J")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::long[10].new
      expect(arr.java_class.name).to eq("[J")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::long[10,10].new
      expect(arr.java_class.name).to eq("[[J")
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1,2].to_java :long
      expect(arr.java_class.name).to eq("[J")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::long
      expect(arr.java_class.name).to eq("[J")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::long[5].new
      arr[0] = 12
      arr[1] = 20
      arr[2] = 42
      
      expect(arr[0]).to eq(12)
      expect(arr[1]).to eq(20)
      expect(arr[2]).to eq(42)
      expect(arr[3]).to eq(0)
      expect(arr[4]).to eq(0)
    end

    it "should be possible to get values from primitive array" do 
      arr = [13, 42, 120].to_java :long
      expect(arr[0]).to eq(13)
      expect(arr[1]).to eq(42)
      expect(arr[2]).to eq(120)
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13, 42, 120].to_java :long
      ret = ArrayReceiver::call_with_long(arr)
      expect(ret.to_a).to eq([13, 42, 120])
    end
  end

  describe "short" do 
    it "should be possible to create empty array" do 
      arr = Java::short[0].new
      expect(arr.java_class.name).to eq("[S")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::short[10].new
      expect(arr.java_class.name).to eq("[S")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::short[10,10].new
      expect(arr.java_class.name).to eq("[[S")
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1,2].to_java :short
      expect(arr.java_class.name).to eq("[S")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::short
      expect(arr.java_class.name).to eq("[S")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::short[5].new
      arr[0] = 12
      arr[1] = 20
      arr[2] = 42
      
      expect(arr[0]).to eq(12)
      expect(arr[1]).to eq(20)
      expect(arr[2]).to eq(42)
      expect(arr[3]).to eq(0)
      expect(arr[4]).to eq(0)
    end

    it "should be possible to get values from primitive array" do 
      arr = [13, 42, 120].to_java :short
      expect(arr[0]).to eq(13)
      expect(arr[1]).to eq(42)
      expect(arr[2]).to eq(120)
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13, 42, 120].to_java :short
      ret = ArrayReceiver::call_with_short(arr)
      expect(ret.to_a).to eq([13, 42, 120])
    end
  end

  describe "string" do 
    it "should be possible to create empty array" do 
      arr = java.lang.String[0].new
      expect(arr.java_class.name).to eq("[Ljava.lang.String;")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = java.lang.String[10].new
      expect(arr.java_class.name).to eq("[Ljava.lang.String;")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = java.lang.String[10,10].new
      expect(arr.java_class.name).to eq("[[Ljava.lang.String;")
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = ["foo","bar"].to_java :string
      expect(arr.java_class.name).to eq("[Ljava.lang.String;")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq("foo")
      expect(arr[1]).to eq("bar")


      # Check with type
      arr = ["foo","bar"].to_java java.lang.String
      expect(arr.java_class.name).to eq("[Ljava.lang.String;")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq("foo")
      expect(arr[1]).to eq("bar")
    end
    
    it "should be possible to set values in primitive array" do 
      arr = java.lang.String[5].new
      arr[0] = "12"
      arr[1] = "20"
      arr[2] = "42"
      
      expect(arr[0]).to eq("12")
      expect(arr[1]).to eq("20")
      expect(arr[2]).to eq("42")
      expect(arr[3]).to be_nil
      expect(arr[4]).to be_nil
    end

    it "should be possible to get values from primitive array" do 
      arr = ["flurg", "glax", "morg"].to_java :string
      expect(arr[0]).to eq("flurg")
      expect(arr[1]).to eq("glax")
      expect(arr[2]).to eq("morg")
    end

    it "should be possible to call methods that take primitive array" do 
      arr = ["flurg", "glax", "morg"].to_java :string
      ret = ArrayReceiver::call_with_string(arr)
      expect(ret.to_a).to eq(["flurg", "glax", "morg"])
    end
  end

  describe "Object ref" do 
    it "should be possible to create empty array" do 
      arr = java.util.HashMap[0].new
      expect(arr.java_class.name).to eq("[Ljava.util.HashMap;")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = java.util.HashMap[10].new
      expect(arr.java_class.name).to eq("[Ljava.util.HashMap;")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = java.util.HashMap[10,10].new
      expect(arr.java_class.name).to eq("[[Ljava.util.HashMap;")
    end

    it "should be possible to create primitive array from Ruby array" do
      h1 = java.util.HashMap.new
      h1["foo"] = "max"

      h2 = java.util.HashMap.new
      h2["max"] = "foo"

      arr = [h1, h2].to_java java.util.HashMap
      expect(arr.java_class.name).to eq("[Ljava.util.HashMap;")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(h1)
      expect(arr[1]).to eq(h2)
    end
    
    it "should be possible to set values in primitive array" do 
      h1 = java.util.HashMap.new
      h1["foo"] = "max"

      h2 = java.util.HashMap.new
      h2["max"] = "foo"

      h3 = java.util.HashMap.new
      h3["flix"] = "mux"

      arr = java.util.HashMap[5].new
      arr[0] = h1
      arr[1] = h2
      arr[2] = h3
      
      expect(arr[0]).to eq(h1)
      expect(arr[1]).to eq(h2)
      expect(arr[2]).to eq(h3)
      expect(arr[3]).to be_nil
      expect(arr[4]).to be_nil
    end

    it "should be possible to get values from primitive array" do
      h1 = java.util.HashMap.new
      h1["foo"] = "max"

      h2 = java.util.HashMap.new
      h2["max"] = "foo"

      h3 = java.util.HashMap.new
      h3["flix"] = "mux"

      arr = [h1, h2, h3].to_java java.util.HashMap
      expect(arr[0]).to eq(h1)
      expect(arr[1]).to eq(h2)
      expect(arr[2]).to eq(h3)
    end

    it "should be possible to call methods that take primitive array" do
      h1 = java.util.HashMap.new
      h1["foo"] = "max"

      h2 = java.util.HashMap.new
      h2["max"] = "foo"

      h3 = java.util.HashMap.new
      h3["flix"] = "mux"

      arr = [h1, h2, h3].to_java java.util.HashMap
      ret = ArrayReceiver::call_with_object(arr)
      expect(ret.to_a).to eq([h1, h2, h3])
    end
  end

  describe "Class ref" do 
    it "should be possible to create empty array" do 
      arr = java.lang.Class[0].new
      expect(arr.java_class.name).to eq("[Ljava.lang.Class;")
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = java.lang.Class[10].new
      expect(arr.java_class.name).to eq("[Ljava.lang.Class;")
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = java.lang.Class[10,10].new
      expect(arr.java_class.name).to eq("[[Ljava.lang.Class;")
    end

    it "should be possible to create primitive array from Ruby array" do
        h1 = java.lang.String.java_class
        h2 = java.util.HashMap.java_class

        arr = [h1, h2].to_java java.lang.Class
        expect(arr.java_class.name).to eq("[Ljava.lang.Class;")

        expect(arr.length).to eq(2)

        expect(arr[0]).to eq(h1)
        expect(arr[1]).to eq(h2)
    end
    
    it "should be possible to set values in primitive array" do 
        h1 = java.util.Set.java_class
        h2 = java.util.HashMap.java_class
        h3 = java.lang.ref.SoftReference.java_class

        arr = java.lang.Class[5].new
        arr[0] = h1
        arr[1] = h2
        arr[2] = h3
        
        expect(arr[0]).to eq(h1)
        expect(arr[1]).to eq(h2)
        expect(arr[2]).to eq(h3)
        expect(arr[3]).to be_nil
        expect(arr[4]).to be_nil
    end

    it "should be possible to get values from primitive array" do
        h1 = java.util.Set.java_class
        h2 = java.util.HashMap.java_class
        h3 = java.lang.ref.SoftReference.java_class

        arr = [h1, h2, h3].to_java java.lang.Class
        expect(arr[0]).to eq(h1)
        expect(arr[1]).to eq(h2)
        expect(arr[2]).to eq(h3)
    end

    it "should be possible to call methods that take primitive array" do
        h1 = java.util.Set.java_class
        h2 = java.util.HashMap.java_class
        h3 = java.lang.ref.SoftReference.java_class

        arr = [h1, h2, h3].to_java java.lang.Class
        ret = ArrayReceiver::call_with_object(arr)
        expect(ret.to_a).to eq([h1, h2, h3])
    end
  end
end

describe "A Ruby array with a nil element" do
  it "can be coerced to an array of objects" do
    ary = [nil]
    result = ary.to_java java.lang.Runnable
    expect(result[0]).to be_nil
  end

  it "can be coerced to an array of classes" do
    ary = [nil]
    result = ary.to_java java.lang.Class
    expect(result[0]).to be_nil
  end
end

describe "A multi-dimensional Ruby array" do
  it "can be coerced to a multi-dimensional Java array" do
    ary = [[1,2],[3,4],[5,6],[7,8],[9,0]]
    java_ary = ary.to_java(Java::long[])
    expect(java_ary.class).to eq(Java::long[][])
    expect(java_ary[0].class).to eq(Java::long[])

    java_ary = ary.to_java(Java::double[])
    expect(java_ary.class).to eq(Java::double[][])
    expect(java_ary[0].class).to eq(Java::double[])

    ary = [[[1]]]
    java_ary = ary.to_java(Java::long[][])
    expect(java_ary.class).to eq(Java::long[][][])
    expect(java_ary[0].class).to eq(Java::long[][])
    expect(java_ary[0][0].class).to eq(Java::long[])
  end
end

# From JRUBY-2944; probably could be reduced a bit more.
describe "A Ruby class implementing an interface returning a Java Object[]" do
  it "should return an Object[]" do
    class MyHash < Hash; end

    class Bar
      include ArrayReturningInterface
      def blah()
        a = []
        a << MyHash.new
        return a.to_java
      end
    end
    expect(ArrayReturningInterfaceConsumer.new.eat(Bar.new)).not_to eq(nil)
    expect(ArrayReturningInterfaceConsumer.new.eat(Bar.new).java_object.class.name).to eq('Java::JavaArray')
    expect(ArrayReturningInterfaceConsumer.new.eat(Bar.new).java_object.class).to eq(Java::JavaArray)
  end
end

describe "A Ruby class extending a Java class" do
  it "should fail when constructing through private superclass constructor" do
    cls = Class.new(PrivateConstructor) do
      def initialize
        super()
      end
    end

    begin
      cls.new
    rescue TypeError => e
      expect( e.message ).to match /class .*PrivateConstructor doesn't have .* constructor/
    end
  end

  it "should fail when constructing through package superclass constructor" do
    cls = Class.new(PackageConstructor) do
      def initialize
        super()
      end
    end

    expect {cls.new}.to raise_error(TypeError)
  end

  it "should succeed when constructing through public superclass constructor" do
    cls = Class.new(PublicConstructor) do
      def initialize
        super()
      end
    end

    expect {cls.new}.not_to raise_error
  end

  it "should succeed when constructing through protected superclass constructor" do
    cls = Class.new(ProtectedConstructor) do
      def initialize
        super()
      end
    end

    expect {cls.new}.not_to raise_error
  end

  it 'resolves expected constructor' do
    cls = Class.new(java.lang.Thread) # plenty of constructors

    expect { cls.new }.not_to raise_error # Thread()
    expect { cls.new('test') }.not_to raise_error # Thread(String)

    cls = Class.new(java.io.CharArrayWriter) {} # has only 2 constructors
    expect { cls.new }.not_to raise_error # ()
    expect { cls.new(42) }.not_to raise_error # (int initialSize)
    expect { cls.new('xxx') }.to raise_error(ArgumentError) # wrong number of arguments for constructor
  end

  class RubyFilterWriter < java.io.FilterWriter
    # protected FilterWriter(Writer out)
    def flush; end
  end

  it 'resolves expected constructor (1)' do # optimized _case_
    writer = java.io.CharArrayWriter.new
    # protected FilterWriter(Writer out)
    expect { RubyFilterWriter.new(writer) }.not_to raise_error
    expect { RubyFilterWriter.new }.to raise_error(ArgumentError)
  end

  it 'resolves expected constructor (0)' do # optimized _case_
    klass = Class.new(java.util.concurrent.RecursiveTask) do
      # public RecursiveTask()
      def compute; return 42 end
    end
    expect { klass.new }.not_to raise_error
  end

end

