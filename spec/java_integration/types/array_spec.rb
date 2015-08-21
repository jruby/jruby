require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ArrayReceiver"
java_import "java_integration.fixtures.ArrayReturningInterface"
java_import "java_integration.fixtures.ArrayReturningInterfaceConsumer"

describe "A Java primitive Array of type" do
  describe "boolean" do
    it "should be possible to create empty array" do
      arr = Java::boolean[0].new
      expect(arr.java_class.to_s).to eq("[Z")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::boolean[10].new
      expect(arr.java_class.to_s).to eq("[Z")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::boolean[10,10].new
      expect(arr.java_class.to_s).to eq("[[Z")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = [true, false].to_java :boolean
      expect(arr.java_class.to_s).to eq("[Z")

      expect(arr.length).to eq(2)

      expect(arr[0]).to be_truthy
      expect(arr[1]).to be_falsey


      # Check with type
      arr = [true, false].to_java Java::boolean
      expect(arr.java_class.to_s).to eq("[Z")

      expect(arr.length).to eq(2)

      expect(arr[0]).to be_truthy
      expect(arr[1]).to be_falsey
    end

    it "should be possible to set values in primitive array" do
      arr = Java::boolean[5].new
      arr[3] = true

      expect(arr[0]).to be_falsey
      expect(arr[1]).to be_falsey
      expect(arr[2]).to be_falsey
      expect(arr[3]).to be_truthy
      expect(arr[4]).to be_falsey
    end

    it "should be possible to get values from primitive array" do
      arr = [false, true, false].to_java :boolean
      expect(arr[0]).to be_falsey
      expect(arr[1]).to be_truthy
      expect(arr[2]).to be_falsey
    end

    it "should be possible to call methods that take primitive array" do
      arr = [false, true, false].to_java :boolean
      ret = ArrayReceiver::call_with_boolean(arr)
      expect(ret.to_a).to eq([false, true, false])
    end

    it "inspects to show type and contents" do
      arr = [false, true, false].to_java :boolean
      expect(arr.inspect).to match(/^boolean\[false, true, false\]@[0-9a-f]+$/)
    end
  end

  describe "byte" do
    it "should be possible to create empty array" do
      arr = Java::byte[0].new
      expect(arr.java_class.to_s).to eq("[B")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::byte[10].new
      expect(arr.java_class.to_s).to eq("[B")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::byte[10,10].new
      expect(arr.java_class.to_s).to eq("[[B")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = [1,2].to_java :byte
      expect(arr.java_class.to_s).to eq("[B")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::byte
      expect(arr.java_class.to_s).to eq("[B")

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

    it "allows setting and getting unsigned bytes with ubyte_set and ubyte_get" do
      arr = Java::byte[1].new
      expect do
        arr[0] = 0xFF
      end.to raise_error(RangeError)
      arr.ubyte_set(0, 0xFF)
      expect(arr.ubyte_get(0)).to eq(0xFF)
      expect(arr[0]).to eq(-1)
    end

    it "inspects to show type and contents" do
      arr = [1, 2, 3].to_java :byte
      expect(arr.inspect).to match(/^byte\[1, 2, 3\]@[0-9a-f]+$/)
    end

    it "makes an ascii 8 bit string on to_s" do
      expect([86, 87].to_java(:byte).to_s).to eq("VW")
    end
  end

  describe "char" do
    it "should be possible to create empty array" do
      arr = Java::char[0].new
      expect(arr.java_class.to_s).to eq("[C")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::char[10].new
      expect(arr.java_class.to_s).to eq("[C")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::char[10,10].new
      expect(arr.java_class.to_s).to eq("[[C")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = [1,2].to_java :char
      expect(arr.java_class.to_s).to eq("[C")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::char
      expect(arr.java_class.to_s).to eq("[C")

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

    it "inspects to show type and contents" do
      arr = [100, 101, 102].to_java :char
      expect(arr.inspect).to match(/^char\[d, e, f\]@[0-9a-f]+$/)
    end

    it "uses toString on to_s" do
      arr = [100, 101, 102].to_java :char
      expect(arr.to_s).to match(/\[C@[0-9a-f]+$/)
    end
  end

  describe "double" do
    it "should be possible to create empty array" do
      arr = Java::double[0].new
      expect(arr.java_class.to_s).to eq("[D")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::double[10].new
      expect(arr.java_class.to_s).to eq("[D")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::double[10,10].new
      expect(arr.java_class.to_s).to eq("[[D")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = [1.2,2.3].to_java :double
      expect(arr.java_class.to_s).to eq("[D")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1.2)
      expect(arr[1]).to eq(2.3)


      # Check with type
      arr = [1.2,2.3].to_java Java::double
      expect(arr.java_class.to_s).to eq("[D")

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

    it "inspects to show type and contents" do
      arr = [1.0, 1.1, 1.2].to_java :double
      expect(arr.inspect).to match(/^double\[1\.0, 1\.1, 1\.2\]@[0-9a-f]+$/)
    end
  end

  describe "float" do
    it "should be possible to create empty array" do
      arr = Java::float[0].new
      expect(arr.java_class.to_s).to eq("[F")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::float[10].new
      expect(arr.java_class.to_s).to eq("[F")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::float[10,10].new
      expect(arr.java_class.to_s).to eq("[[F")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = [1.2,2.3].to_java :float
      expect(arr.java_class.to_s).to eq("[F")

      expect(arr.length).to eq(2)

      expect(arr[0]).to be_within(0.00001).of(1.2)
      expect(arr[1]).to be_within(0.00001).of(2.3)


      # Check with type
      arr = [1.2,2.3].to_java Java::float
      expect(arr.java_class.to_s).to eq("[F")

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

    it "inspects to show type and contents" do
      arr = [1.0, 1.1, 1.2].to_java :float
      expect(arr.inspect).to match(/^float\[1\.0, 1\.1, 1\.2\]@[0-9a-f]+$/)
    end
  end

  describe "int" do
    it "should be possible to create empty array" do
      arr = Java::int[0].new
      expect(arr.java_class.to_s).to eq("[I")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::int[10].new
      expect(arr.java_class.to_s).to eq("[I")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::int[10,10].new
      expect(arr.java_class.to_s).to eq("[[I")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = [1,2].to_java :int
      expect(arr.java_class.to_s).to eq("[I")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::int
      expect(arr.java_class.to_s).to eq("[I")

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

    it "inspects to show type and contents" do
      arr = [13, 42, 120].to_java :int
      expect(arr.inspect).to match(/^int\[13, 42, 120\]@[0-9a-f]+$/)
    end
  end

  describe "long" do
    it "should be possible to create empty array" do
      arr = Java::long[0].new
      expect(arr.java_class.to_s).to eq("[J")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::long[10].new
      expect(arr.java_class.to_s).to eq("[J")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::long[10,10].new
      expect(arr.java_class.to_s).to eq("[[J")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = [1,2].to_java :long
      expect(arr.java_class.to_s).to eq("[J")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::long
      expect(arr.java_class.to_s).to eq("[J")

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

    it "inspects to show type and contents" do
      arr = [13, 42, 120].to_java :long
      expect(arr.inspect).to match(/^long\[13, 42, 120\]@[0-9a-f]+$/)
    end
  end

  describe "short" do
    it "should be possible to create empty array" do
      arr = Java::short[0].new
      expect(arr.java_class.to_s).to eq("[S")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::short[10].new
      expect(arr.java_class.to_s).to eq("[S")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::short[10,10].new
      expect(arr.java_class.to_s).to eq("[[S")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = [1,2].to_java :short
      expect(arr.java_class.to_s).to eq("[S")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::short
      expect(arr.java_class.to_s).to eq("[S")

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

    it "inspects to show type and contents" do
      arr = [13, 42, 120].to_java :short
      expect(arr.inspect).to match(/^short\[13, 42, 120\]@[0-9a-f]+$/)
    end
  end

  describe "string" do
    it "should be possible to create empty array" do
      arr = java.lang.String[0].new
      expect(arr.java_class.to_s).to eq("[Ljava.lang.String;")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = java.lang.String[10].new
      expect(arr.java_class.to_s).to eq("[Ljava.lang.String;")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = java.lang.String[10,10].new
      expect(arr.java_class.to_s).to eq("[[Ljava.lang.String;")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = ["foo", :bar].to_java :string
      expect(arr.java_class.to_s).to eq("[Ljava.lang.String;")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq("foo")
      expect(arr[1]).to eq("bar")


      # Check with type
      arr = ["foo", :bar].to_java java.lang.String
      expect(arr.java_class.to_s).to eq("[Ljava.lang.String;")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq("foo")
      expect(arr[1]).to eq("bar")
    end

    it "should be possible to set values in primitive array" do
      arr = java.lang.String[5].new
      arr[0] = "12"
      arr[1] = :blah
      arr[2] = "42"

      expect(arr[0]).to eq("12")
      expect(arr[1]).to eq("blah")
      expect(arr[2]).to eq("42")
      expect(arr[3]).to be_nil
      expect(arr[4]).to be_nil
    end

    it "should be possible to get values from primitive array" do
      arr = ["flurg", :glax, "morg"].to_java :string
      expect(arr[0]).to eq("flurg")
      expect(arr[1]).to eq("glax")
      expect(arr[2]).to eq("morg")
    end

    it "should be possible to call methods that take primitive array" do
      arr = ["flurg", :glax, "morg"].to_java :string
      ret = ArrayReceiver::call_with_string(arr)
      expect(ret.to_a).to eq(["flurg", "glax", "morg"])
    end

    it "inspects to show type and contents" do
      arr = ['foo', 'bar', 'baz'].to_java :string
      expect(arr.inspect).to match(/^java.lang.String\[foo, bar, baz\]@[0-9a-f]+$/)
    end
  end

  describe "Object" do
    it "should be possible to create empty array" do
      arr = java.util.HashMap[0].new
      expect(arr.java_class.to_s).to eq("[Ljava.util.HashMap;")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = java.util.HashMap[10].new
      expect(arr.java_class.to_s).to eq("[Ljava.util.HashMap;")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = java.util.HashMap[10,10].new
      expect(arr.java_class.to_s).to eq("[[Ljava.util.HashMap;")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      h1 = java.util.HashMap.new
      h1["foo"] = "max"

      h2 = java.util.HashMap.new
      h2["max"] = "foo"

      arr = [h1, h2].to_java java.util.HashMap
      expect(arr.java_class.to_s).to eq("[Ljava.util.HashMap;")

      expect(arr.length).to eq(2)

      expect(arr[0]).to be_equal(h1)
      expect(arr[1]).to be_equal(h2)
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
      expect(arr[0]).to be_equal(h1)
      expect(arr[1]).to be_equal(h2)
      expect(arr[2]).to be_equal(h3)
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

    it "should coerce strings, booleans, and numerics via []" do
      ary = [1, 1.0, "blah", true, false, nil].to_java

      expect(ary[0].class).to eq(Fixnum)
      expect(ary[1].class).to eq(Float)
      expect(ary[2].class).to eq(String)
      expect(ary[3].class).to eq(TrueClass)
      expect(ary[4].class).to eq(FalseClass)
      expect(ary[5].class).to eq(NilClass)
    end

    it "should raise TypeError when types can't be coerced" do
      expect { [Time.new].to_java :string }.to raise_error(TypeError)
    end

    it "inspects to show type and contents" do
      jobject = java.lang.Object
      arr = [jobject.new, jobject.new, jobject.new].to_java :object
      expect(arr.inspect).to match(/^java.lang.Object\[java\.lang\.Object@[0-9a-f]+, java\.lang\.Object@[0-9a-f]+, java\.lang\.Object@[0-9a-f]+\]@[0-9a-f]+$/)
    end
  end

  describe "Class ref" do
    it "should be possible to create empty array" do
      arr = java.lang.Class[0].new
      expect(arr.java_class.to_s).to eq("[Ljava.lang.Class;")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = java.lang.Class[10].new
      expect(arr.java_class.to_s).to eq("[Ljava.lang.Class;")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = java.lang.Class[10,10].new
      expect(arr.java_class.to_s).to eq("[[Ljava.lang.Class;")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
        h1 = java.lang.String.java_class
        h2 = java.util.HashMap.java_class

        arr = [h1, h2].to_java java.lang.Class
        expect(arr.java_class.to_s).to eq("[Ljava.lang.Class;")

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

    it "inspects to show type and contents" do
      h1 = java.util.Set.java_class
      h2 = java.util.HashMap.java_class
      h3 = java.lang.ref.SoftReference.java_class

      arr = [h1, h2, h3].to_java java.lang.Class
      expect(arr.inspect).to match(/^java\.lang\.Class\[interface java\.util\.Set, class java\.util\.HashMap, class java\.lang\.ref\.SoftReference\]@[0-9a-f]+$/)
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

# JRUBY-3175: Cloning java byte array returns incorrect object
describe "A Java byte array" do
  it "should clone to another usable Java byte array" do
    s = "switch me to bytes".to_java_bytes
    c = s.clone
    expect(String.from_java_bytes(s)).to eq("switch me to bytes")
    expect(String.from_java_bytes(c)).to eq("switch me to bytes")
    expect(String.from_java_bytes(s)).to eq(String.from_java_bytes(c))
  end
end

# JRUBY-928
describe "ArrayJavaProxy" do
  it "descends from java.lang.Object" do
    expect(ArrayJavaProxy.superclass).to eq(java.lang.Object)
  end

  it "to_a coerces nested Java arrays to Ruby arrays" do
    arr = [[1],[2]].to_java(Java::byte[]).to_a
    expect(arr.first).to eq([1])
    expect(arr.first.first).to be_kind_of(Fixnum)
  end

  it "returns a new array from to_a" do
    j_arr = [ 1, 2 ].to_java
    r_arr = j_arr.to_a
    j_arr[0] = 3
    expect( r_arr[0] ).to eql 1
  end

  it "returns a new array from to_ary" do
    j_arr = [ 1, 2 ].to_java
    r_arr = j_arr.to_ary
    j_arr[0] = 3
    expect( r_arr[0] ).to eql 1
  end

end
