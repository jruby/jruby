require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ArrayReceiver"
java_import "java_integration.fixtures.ArrayReturningInterface"
java_import "java_integration.fixtures.ArrayReturningInterfaceConsumer"

describe "A Java primitive Array of type" do
  describe "boolean" do 
    it "should be possible to create empty array" do 
      arr = Java::boolean[0].new
      arr.java_class.to_s.should == "[Z"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::boolean[10].new
      arr.java_class.to_s.should == "[Z"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::boolean[10,10].new
      arr.java_class.to_s.should == "[[Z"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [true, false].to_java :boolean
      arr.java_class.to_s.should == "[Z"

      arr.length.should == 2

      arr[0].should be_true
      arr[1].should be_false


      # Check with type
      arr = [true, false].to_java Java::boolean
      arr.java_class.to_s.should == "[Z"

      arr.length.should == 2

      arr[0].should be_true
      arr[1].should be_false
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::boolean[5].new
      arr[3] = true
      
      arr[0].should be_false
      arr[1].should be_false
      arr[2].should be_false
      arr[3].should be_true
      arr[4].should be_false
    end

    it "should be possible to get values from primitive array" do 
      arr = [false, true, false].to_java :boolean
      arr[0].should be_false
      arr[1].should be_true
      arr[2].should be_false
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [false, true, false].to_java :boolean
      ret = ArrayReceiver::call_with_boolean(arr)
      ret.to_a.should == [false, true, false]
    end
    
    it "inspects to show type and contents" do
      arr = [false, true, false].to_java :boolean
      arr.inspect.should =~ /^boolean\[false, true, false\]@[0-9a-f]+$/
    end
  end

  describe "byte" do 
    it "should be possible to create empty array" do 
      arr = Java::byte[0].new
      arr.java_class.to_s.should == "[B"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::byte[10].new
      arr.java_class.to_s.should == "[B"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::byte[10,10].new
      arr.java_class.to_s.should == "[[B"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1,2].to_java :byte
      arr.java_class.to_s.should == "[B"

      arr.length.should == 2

      arr[0].should == 1
      arr[1].should == 2


      # Check with type
      arr = [1,2].to_java Java::byte
      arr.java_class.to_s.should == "[B"

      arr.length.should == 2

      arr[0].should == 1
      arr[1].should == 2
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::byte[5].new
      arr[0] = 12
      arr[1] = 20
      arr[2] = 42
      
      arr[0].should == 12
      arr[1].should == 20
      arr[2].should == 42
      arr[3].should == 0
      arr[4].should == 0
    end

    it "should be possible to get values from primitive array" do 
      arr = [13, 42, 120].to_java :byte
      arr[0].should == 13
      arr[1].should == 42
      arr[2].should == 120
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13, 42, 120].to_java :byte
      ret = ArrayReceiver::call_with_byte(arr)
      ret.to_a.should == [13, 42, 120]
    end
    
    it "allows setting and getting unsigned bytes with ubyte_set and ubyte_get" do
      arr = Java::byte[1].new
      lambda do
        arr[0] = 0xFF
      end.should raise_error(RangeError)
      arr.ubyte_set(0, 0xFF)
      arr.ubyte_get(0).should == 0xFF
      arr[0].should == -1
    end
    
    it "inspects to show type and contents" do
      arr = [1, 2, 3].to_java :byte
      arr.inspect.should =~ /^byte\[1, 2, 3\]@[0-9a-f]+$/
    end

    it "makes an ascii 8 bit string on to_s" do
      [86, 87].to_java(:byte).to_s.should == "VW"
    end
  end

  describe "char" do 
    it "should be possible to create empty array" do 
      arr = Java::char[0].new
      arr.java_class.to_s.should == "[C"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::char[10].new
      arr.java_class.to_s.should == "[C"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::char[10,10].new
      arr.java_class.to_s.should == "[[C"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1,2].to_java :char
      arr.java_class.to_s.should == "[C"

      arr.length.should == 2

      arr[0].should == 1
      arr[1].should == 2


      # Check with type
      arr = [1,2].to_java Java::char
      arr.java_class.to_s.should == "[C"

      arr.length.should == 2

      arr[0].should == 1
      arr[1].should == 2
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::char[5].new
      arr[0] = 12
      arr[1] = 20
      arr[2] = 42
      
      arr[0].should == 12
      arr[1].should == 20
      arr[2].should == 42
      arr[3].should == 0
      arr[4].should == 0
    end

    it "should be possible to get values from primitive array" do 
      arr = [13, 42, 120].to_java :char
      arr[0].should == 13
      arr[1].should == 42
      arr[2].should == 120
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13, 42, 120].to_java :char
      ret = ArrayReceiver::call_with_char(arr)
      ret.to_a.should == [13, 42, 120]
    end
    
    it "inspects to show type and contents" do
      arr = [100, 101, 102].to_java :char
      arr.inspect.should =~ /^char\[d, e, f\]@[0-9a-f]+$/
    end
  end

  describe "double" do 
    it "should be possible to create empty array" do 
      arr = Java::double[0].new
      arr.java_class.to_s.should == "[D"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::double[10].new
      arr.java_class.to_s.should == "[D"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::double[10,10].new
      arr.java_class.to_s.should == "[[D"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1.2,2.3].to_java :double
      arr.java_class.to_s.should == "[D"

      arr.length.should == 2

      arr[0].should == 1.2
      arr[1].should == 2.3


      # Check with type
      arr = [1.2,2.3].to_java Java::double
      arr.java_class.to_s.should == "[D"

      arr.length.should == 2

      arr[0].should == 1.2
      arr[1].should == 2.3
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::double[5].new
      arr[0] = 12.2
      arr[1] = 20.3
      arr[2] = 42.4
      
      arr[0].should == 12.2
      arr[1].should == 20.3
      arr[2].should == 42.4
      arr[3].should == 0.0
      arr[4].should == 0.0
    end

    it "should be possible to get values from primitive array" do 
      arr = [13.2, 42.3, 120.4].to_java :double
      arr[0].should == 13.2
      arr[1].should == 42.3
      arr[2].should == 120.4
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13.2, 42.3, 120.4].to_java :double
      ret = ArrayReceiver::call_with_double(arr)
      ret.to_a.should == [13.2, 42.3, 120.4]
    end
    
    it "inspects to show type and contents" do
      arr = [1.0, 1.1, 1.2].to_java :double
      arr.inspect.should =~ /^double\[1\.0, 1\.1, 1\.2\]@[0-9a-f]+$/
    end
  end

  describe "float" do 
    it "should be possible to create empty array" do 
      arr = Java::float[0].new
      arr.java_class.to_s.should == "[F"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::float[10].new
      arr.java_class.to_s.should == "[F"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::float[10,10].new
      arr.java_class.to_s.should == "[[F"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1.2,2.3].to_java :float
      arr.java_class.to_s.should == "[F"

      arr.length.should == 2

      arr[0].should be_within(0.00001).of(1.2)
      arr[1].should be_within(0.00001).of(2.3)


      # Check with type
      arr = [1.2,2.3].to_java Java::float
      arr.java_class.to_s.should == "[F"

      arr.length.should == 2

      arr[0].should be_within(0.00001).of(1.2)
      arr[1].should be_within(0.00001).of(2.3)
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::float[5].new
      arr[0] = 12.2
      arr[1] = 20.3
      arr[2] = 42.4
      
      arr[0].should be_within(0.00001).of(12.2)
      arr[1].should be_within(0.00001).of(20.3)
      arr[2].should be_within(0.00001).of(42.4)
      arr[3].should == 0.0
      arr[4].should == 0.0
    end

    it "should be possible to get values from primitive array" do 
      arr = [13.2, 42.3, 120.4].to_java :float

      arr[0].should be_within(0.00001).of(13.2)
      arr[1].should be_within(0.00001).of(42.3)
      arr[2].should be_within(0.00001).of(120.4)
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13.2, 42.3, 120.4].to_java :float
      ret = ArrayReceiver::call_with_float(arr)
      ret.length.should == 3
      ret[0].should be_within(0.00001).of(13.2)
      ret[1].should be_within(0.00001).of(42.3)
      ret[2].should be_within(0.00001).of(120.4)
    end
    
    it "inspects to show type and contents" do
      arr = [1.0, 1.1, 1.2].to_java :float
      arr.inspect.should =~ /^float\[1\.0, 1\.1, 1\.2\]@[0-9a-f]+$/
    end
  end

  describe "int" do 
    it "should be possible to create empty array" do 
      arr = Java::int[0].new
      arr.java_class.to_s.should == "[I"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::int[10].new
      arr.java_class.to_s.should == "[I"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::int[10,10].new
      arr.java_class.to_s.should == "[[I"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1,2].to_java :int
      arr.java_class.to_s.should == "[I"

      arr.length.should == 2

      arr[0].should == 1
      arr[1].should == 2


      # Check with type
      arr = [1,2].to_java Java::int
      arr.java_class.to_s.should == "[I"

      arr.length.should == 2

      arr[0].should == 1
      arr[1].should == 2
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::int[5].new
      arr[0] = 12
      arr[1] = 20
      arr[2] = 42
      
      arr[0].should == 12
      arr[1].should == 20
      arr[2].should == 42
      arr[3].should == 0
      arr[4].should == 0
    end

    it "should be possible to get values from primitive array" do 
      arr = [13, 42, 120].to_java :int
      arr[0].should == 13
      arr[1].should == 42
      arr[2].should == 120
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13, 42, 120].to_java :int
      ret = ArrayReceiver::call_with_int(arr)
      ret.to_a.should == [13, 42, 120]
    end
    
    it "inspects to show type and contents" do
      arr = [13, 42, 120].to_java :int
      arr.inspect.should =~ /^int\[13, 42, 120\]@[0-9a-f]+$/
    end
  end

  describe "long" do 
    it "should be possible to create empty array" do 
      arr = Java::long[0].new
      arr.java_class.to_s.should == "[J"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::long[10].new
      arr.java_class.to_s.should == "[J"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::long[10,10].new
      arr.java_class.to_s.should == "[[J"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1,2].to_java :long
      arr.java_class.to_s.should == "[J"

      arr.length.should == 2

      arr[0].should == 1
      arr[1].should == 2


      # Check with type
      arr = [1,2].to_java Java::long
      arr.java_class.to_s.should == "[J"

      arr.length.should == 2

      arr[0].should == 1
      arr[1].should == 2
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::long[5].new
      arr[0] = 12
      arr[1] = 20
      arr[2] = 42
      
      arr[0].should == 12
      arr[1].should == 20
      arr[2].should == 42
      arr[3].should == 0
      arr[4].should == 0
    end

    it "should be possible to get values from primitive array" do 
      arr = [13, 42, 120].to_java :long
      arr[0].should == 13
      arr[1].should == 42
      arr[2].should == 120
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13, 42, 120].to_java :long
      ret = ArrayReceiver::call_with_long(arr)
      ret.to_a.should == [13, 42, 120]
    end
    
    it "inspects to show type and contents" do
      arr = [13, 42, 120].to_java :long
      arr.inspect.should =~ /^long\[13, 42, 120\]@[0-9a-f]+$/
    end
  end

  describe "short" do 
    it "should be possible to create empty array" do 
      arr = Java::short[0].new
      arr.java_class.to_s.should == "[S"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = Java::short[10].new
      arr.java_class.to_s.should == "[S"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = Java::short[10,10].new
      arr.java_class.to_s.should == "[[S"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = [1,2].to_java :short
      arr.java_class.to_s.should == "[S"

      arr.length.should == 2

      arr[0].should == 1
      arr[1].should == 2


      # Check with type
      arr = [1,2].to_java Java::short
      arr.java_class.to_s.should == "[S"

      arr.length.should == 2

      arr[0].should == 1
      arr[1].should == 2
    end
    
    it "should be possible to set values in primitive array" do 
      arr = Java::short[5].new
      arr[0] = 12
      arr[1] = 20
      arr[2] = 42
      
      arr[0].should == 12
      arr[1].should == 20
      arr[2].should == 42
      arr[3].should == 0
      arr[4].should == 0
    end

    it "should be possible to get values from primitive array" do 
      arr = [13, 42, 120].to_java :short
      arr[0].should == 13
      arr[1].should == 42
      arr[2].should == 120
    end

    it "should be possible to call methods that take primitive array" do 
      arr = [13, 42, 120].to_java :short
      ret = ArrayReceiver::call_with_short(arr)
      ret.to_a.should == [13, 42, 120]
    end
    
    it "inspects to show type and contents" do
      arr = [13, 42, 120].to_java :short
      arr.inspect.should =~ /^short\[13, 42, 120\]@[0-9a-f]+$/
    end
  end

  describe "string" do 
    it "should be possible to create empty array" do 
      arr = java.lang.String[0].new
      arr.java_class.to_s.should == "[Ljava.lang.String;"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = java.lang.String[10].new
      arr.java_class.to_s.should == "[Ljava.lang.String;"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = java.lang.String[10,10].new
      arr.java_class.to_s.should == "[[Ljava.lang.String;"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do 
      # Check with symbol name
      arr = ["foo", :bar].to_java :string
      arr.java_class.to_s.should == "[Ljava.lang.String;"

      arr.length.should == 2

      arr[0].should == "foo"
      arr[1].should == "bar"


      # Check with type
      arr = ["foo", :bar].to_java java.lang.String
      arr.java_class.to_s.should == "[Ljava.lang.String;"

      arr.length.should == 2

      arr[0].should == "foo"
      arr[1].should == "bar"
    end
    
    it "should be possible to set values in primitive array" do 
      arr = java.lang.String[5].new
      arr[0] = "12"
      arr[1] = :blah
      arr[2] = "42"
      
      arr[0].should == "12"
      arr[1].should == "blah"
      arr[2].should == "42"
      arr[3].should be_nil
      arr[4].should be_nil
    end

    it "should be possible to get values from primitive array" do 
      arr = ["flurg", :glax, "morg"].to_java :string
      arr[0].should == "flurg"
      arr[1].should == "glax"
      arr[2].should == "morg"
    end

    it "should be possible to call methods that take primitive array" do 
      arr = ["flurg", :glax, "morg"].to_java :string
      ret = ArrayReceiver::call_with_string(arr)
      ret.to_a.should == ["flurg", "glax", "morg"]
    end
    
    it "inspects to show type and contents" do
      arr = ['foo', 'bar', 'baz'].to_java :string
      arr.inspect.should =~ /^java.lang.String\[foo, bar, baz\]@[0-9a-f]+$/
    end
  end

  describe "Object" do
    it "should be possible to create empty array" do 
      arr = java.util.HashMap[0].new
      arr.java_class.to_s.should == "[Ljava.util.HashMap;"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = java.util.HashMap[10].new
      arr.java_class.to_s.should == "[Ljava.util.HashMap;"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = java.util.HashMap[10,10].new
      arr.java_class.to_s.should == "[[Ljava.util.HashMap;"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      h1 = java.util.HashMap.new
      h1["foo"] = "max"

      h2 = java.util.HashMap.new
      h2["max"] = "foo"

      arr = [h1, h2].to_java java.util.HashMap
      arr.java_class.to_s.should == "[Ljava.util.HashMap;"

      arr.length.should == 2

      arr[0].should be_equal(h1)
      arr[1].should be_equal(h2)
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
      
      arr[0].should == h1
      arr[1].should == h2
      arr[2].should == h3
      arr[3].should be_nil
      arr[4].should be_nil
    end

    it "should be possible to get values from primitive array" do
      h1 = java.util.HashMap.new
      h1["foo"] = "max"

      h2 = java.util.HashMap.new
      h2["max"] = "foo"

      h3 = java.util.HashMap.new
      h3["flix"] = "mux"

      arr = [h1, h2, h3].to_java java.util.HashMap
      arr[0].should be_equal(h1)
      arr[1].should be_equal(h2)
      arr[2].should be_equal(h3)
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
      ret.to_a.should == [h1, h2, h3]
    end

    it "should coerce strings, booleans, and numerics via []" do
      ary = [1, 1.0, "blah", true, false, nil].to_java

      ary[0].class.should == Fixnum
      ary[1].class.should == Float
      ary[2].class.should == String
      ary[3].class.should == TrueClass
      ary[4].class.should == FalseClass
      ary[5].class.should == NilClass
    end

    it "should raise TypeError when types can't be coerced" do
      lambda { [Time.new].to_java :string }.should raise_error(TypeError)
    end
    
    it "inspects to show type and contents" do
      jobject = java.lang.Object
      arr = [jobject.new, jobject.new, jobject.new].to_java :object
      arr.inspect.should =~ /^java.lang.Object\[java\.lang\.Object@[0-9a-f]+, java\.lang\.Object@[0-9a-f]+, java\.lang\.Object@[0-9a-f]+\]@[0-9a-f]+$/
    end
  end

  describe "Class ref" do 
    it "should be possible to create empty array" do 
      arr = java.lang.Class[0].new
      arr.java_class.to_s.should == "[Ljava.lang.Class;"
      arr.should be_empty
    end
    
    it "should be possible to create uninitialized single dimensional array" do 
      arr = java.lang.Class[10].new
      arr.java_class.to_s.should == "[Ljava.lang.Class;"
      arr.should_not be_empty
    end
    
    it "should be possible to create uninitialized multi dimensional array" do 
      arr = java.lang.Class[10,10].new
      arr.java_class.to_s.should == "[[Ljava.lang.Class;"
      arr.should_not be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
        h1 = java.lang.String.java_class
        h2 = java.util.HashMap.java_class

        arr = [h1, h2].to_java java.lang.Class
        arr.java_class.to_s.should == "[Ljava.lang.Class;"

        arr.length.should == 2

        arr[0].should == h1
        arr[1].should == h2
    end
    
    it "should be possible to set values in primitive array" do 
        h1 = java.util.Set.java_class
        h2 = java.util.HashMap.java_class
        h3 = java.lang.ref.SoftReference.java_class

        arr = java.lang.Class[5].new
        arr[0] = h1
        arr[1] = h2
        arr[2] = h3
        
        arr[0].should == h1
        arr[1].should == h2
        arr[2].should == h3
        arr[3].should be_nil
        arr[4].should be_nil
    end

    it "should be possible to get values from primitive array" do
        h1 = java.util.Set.java_class
        h2 = java.util.HashMap.java_class
        h3 = java.lang.ref.SoftReference.java_class

        arr = [h1, h2, h3].to_java java.lang.Class
        arr[0].should == h1
        arr[1].should == h2
        arr[2].should == h3
    end

    it "should be possible to call methods that take primitive array" do
        h1 = java.util.Set.java_class
        h2 = java.util.HashMap.java_class
        h3 = java.lang.ref.SoftReference.java_class

        arr = [h1, h2, h3].to_java java.lang.Class
        ret = ArrayReceiver::call_with_object(arr)
        ret.to_a.should == [h1, h2, h3]
    end
    
    it "inspects to show type and contents" do
      h1 = java.util.Set.java_class
      h2 = java.util.HashMap.java_class
      h3 = java.lang.ref.SoftReference.java_class

      arr = [h1, h2, h3].to_java java.lang.Class
      arr.inspect.should =~ /^java\.lang\.Class\[interface java\.util\.Set, class java\.util\.HashMap, class java\.lang\.ref\.SoftReference\]@[0-9a-f]+$/
    end
  end
end

describe "A Ruby array with a nil element" do
  it "can be coerced to an array of objects" do
    ary = [nil]
    result = ary.to_java java.lang.Runnable
    result[0].should be_nil
  end

  it "can be coerced to an array of classes" do
    ary = [nil]
    result = ary.to_java java.lang.Class
    result[0].should be_nil
  end
end

describe "A multi-dimensional Ruby array" do
  it "can be coerced to a multi-dimensional Java array" do
    ary = [[1,2],[3,4],[5,6],[7,8],[9,0]]
    java_ary = ary.to_java(Java::long[])
    java_ary.class.should == Java::long[][]
    java_ary[0].class.should == Java::long[]

    java_ary = ary.to_java(Java::double[])
    java_ary.class.should == Java::double[][]
    java_ary[0].class.should == Java::double[]

    ary = [[[1]]]
    java_ary = ary.to_java(Java::long[][])
    java_ary.class.should == Java::long[][][]
    java_ary[0].class.should == Java::long[][]
    java_ary[0][0].class.should == Java::long[]
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
    ArrayReturningInterfaceConsumer.new.eat(Bar.new).should_not == nil
    ArrayReturningInterfaceConsumer.new.eat(Bar.new).java_object.class.name.should == 'Java::JavaArray'
    ArrayReturningInterfaceConsumer.new.eat(Bar.new).java_object.class.should == Java::JavaArray
  end
end

# JRUBY-3175: Cloning java byte array returns incorrect object
describe "A Java byte array" do
  it "should clone to another usable Java byte array" do
    s = "switch me to bytes".to_java_bytes
    c = s.clone
    String.from_java_bytes(s).should == "switch me to bytes"
    String.from_java_bytes(c).should == "switch me to bytes"
    String.from_java_bytes(s).should == String.from_java_bytes(c)
  end
end

# JRUBY-928
describe "ArrayJavaProxy" do
  it "descends from java.lang.Object" do
    ArrayJavaProxy.superclass.should == java.lang.Object
  end
end
