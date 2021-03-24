require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.ArrayReceiver"
java_import "java_integration.fixtures.ArrayReturningInterface"
java_import "java_integration.fixtures.ArrayReturningInterfaceConsumer"

describe "A Java primitive Array of type" do
  describe "boolean" do
    it "should be possible to create empty array" do
      arr = Java::boolean[0].new
      expect(arr.java_class.to_s).to eq("boolean[]")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::boolean[10].new
      expect(arr.java_class.name).to eq("[Z")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::boolean[10,10].new
      expect(arr.java_class.to_s).to eq("boolean[][]")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = [true, false].to_java :boolean
      expect(arr.java_class.name).to eq("[Z")

      expect(arr.length).to eq(2)

      expect(arr[0]).to be_truthy
      expect(arr[1]).to be_falsey


      # Check with type
      arr = [true, false].to_java Java::boolean
      expect(arr.java_class.name).to eq("[Z")

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
      expect(arr[3]).to be true
      expect(arr[4]).to be false
    end

    it "should be possible to get values from primitive array" do
      arr = [false, true, false].to_java :boolean
      expect(arr[0]).to be false
      expect(arr[1]).to be true
      expect(arr[2]).to be_falsey
    end

    it "should be possible to call methods that take primitive array" do
      arr = [false, true, false].to_java :boolean
      ret = ArrayReceiver::call_with_boolean(arr)
      expect(ret.to_a).to eq([false, true, false])
    end

    it "inspects to show type and contents" do
      arr = [false, true, false].to_java :boolean
      expect(arr.inspect).to eql '#<Java::boolean[3]: [false, true, false]>'
    end
  end

  describe "byte" do
    it "should be possible to create empty array" do
      arr = Java::byte[0].new
      expect(arr.java_class.name).to eq("[B")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::byte[10].new
      expect(arr.java_class.name).to eq("[B")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::byte[10,10].new
      expect(arr.java_class.name).to eq("[[B")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = [1,2].to_java :byte
      expect(arr.java_class.to_s).to eq("byte[]")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq(1)
      expect(arr[1]).to eq(2)


      # Check with type
      arr = [1,2].to_java Java::byte
      expect(arr.java_class.to_s).to eq("byte[]")

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
      arr = [1, 2, -128].to_java :byte
      expect(arr.inspect).to eql '#<Java::byte[3]: [1, 2, -128]>'
    end

    it 'handles equality to another array' do
      arr1 = [ 1, -123, 127 ].to_java :byte
      arr2 = Java::byte[3].new
      arr2[0] = 1; arr2[2] = 127; arr2[1] = -123
      expect( arr1 == arr2 ).to be true
      expect( arr1.eql? arr2 ).to be true
      expect( [ 1, -123, 127 ].to_java(:int).eql? arr2 ).to be false
      expect( [ 1, -123, -127 ].to_java(:byte) == arr2 ).to be false

      expect( arr1 === arr1 ).to be true
      expect( arr2 === arr1 ).to be true
      expect( arr1.class === arr2 ).to be true

      expect( arr1 == [ 1, -123 ] ).to be false
      expect( arr1 == [ 1, -123, 127 ] ).to be true
      expect( [ 1, -123, 127 ] == arr2 ).to be true
      expect( arr1.eql? [ 1, -123, 127 ] ).to be false
      expect( arr2 === [ 1, -123, 127 ] ).to be true
    end

    it "makes an ascii 8 bit string on to_s" do
      expect([86, 87].to_java(:byte).to_s).to eq("VW")
    end

    it "detects element using include?" do
      arr = Java::byte[3].new
      arr[0] = 1
      arr[1] = 127
      arr[2] = -128

      expect(arr.include?(1)).to be true
      expect(arr.include?(0)).to be false
      expect(arr.include?(10000)).to be false
      expect(arr.include?(127)).to be true
      expect(arr.include?(-128)).to be true
      expect(arr.include?(-127)).to be false
      expect(arr.include?(-1)).to be false
      expect(arr.include?(-200)).to be false
      expect(arr.include?(nil)).to be false
      expect(arr.include?('x')).to be false
    end

    it "clones" do
      arr = Java::byte[10].new
      arr[1] = 1

      dup = arr.clone
      expect( dup.object_id ).to_not eql arr.object_id
      dup[1] = 11
      expect(arr[1]).to eq(1)
    end
  end

  describe "char" do
    it "should be possible to create empty array" do
      arr = Java::char[0].new
      expect(arr.java_class.to_s).to eq("char[]")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::char[10].new
      expect(arr.java_class.name).to eq("[C")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::char[10,10].new
      expect(arr.java_class.name).to eq("[[C")
      expect(arr).not_to be_empty
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

    it "inspects to show type and contents" do
      arr = [100, 101, 225].to_java :char
      expect(arr.inspect).to eql "#<Java::char[3]: ['d', 'e', 'á']>"

      arr = Java::char[2].new; arr[1] = 'ō' # null char + multi-byte char
      expect(arr.inspect).to eql "#<Java::char[2]: ['\u0000', 'ō']>"
    end

    it 'handles equality to another array' do
      arr1 = [ 0, 111 ].to_java :char
      arr2 = Java::char[2].new
      arr2[0] = 0; arr2[1] = 111
      expect( arr1 == arr2 ).to be true
      expect( arr1.eql? arr2 ).to be true
      expect( [ 0, 111 ].to_java(:int).eql? arr2 ).to be false
      expect( [ 111 ].to_java(:char) == arr2 ).to be false
      expect( [ 1, 111 ].to_java(:char) == arr2 ).to be false

      expect( arr1 == [ 0 ] ).to be false
      expect( arr1 == [ 0, 111 ] ).to be true
      expect( [ 0, 111 ] == arr2 ).to be true
      expect( arr1.eql? [ 0, 111 ] ).to be false
      expect( arr2 === [ 0, 111 ] ).to be true
    end

    it "uses toString on to_s" do
      arr = [100, 101, 102].to_java :char
      expect(arr.to_s).to match(/\[C@[0-9a-f]+$/)
    end

    it "detects element using include?" do
      arr = Java::char[3].new
      arr[0] = 1
      arr[1] = 0
      arr[2] = 64

      expect(arr.include?(1)).to be true
      expect(arr.include?(0)).to be true
      expect(arr.include?(10)).to be false
      expect(arr.include?(64)).to be true
      expect(arr.include?(-128)).to be false
      expect(arr.include?(-1)).to be false
      expect(arr.include?('z')).to be false
    end
  end

  describe "double" do
    it "should be possible to create empty array" do
      arr = Java::double[0].new
      expect(arr.java_class.name).to eq("[D")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::double[10].new
      expect(arr.java_class.name).to eq("[D")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::double[10,10].new
      expect(arr.java_class.name).to eq("[[D")
      expect(arr).not_to be_empty
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
      expect(arr.java_class.to_s).to eq("double[]")

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

    it 'handles equality to another array' do
      arr1 = [-111, 101010.99].to_java :double
      arr2 = Java::double[2].new
      arr2[0] = -111; arr2[1] = 101010.99
      expect( arr1 == arr2 ).to be true
      expect( arr1.eql? arr2 ).to be true
      expect( [-111, 101010.99].to_java(:float).eql? arr2 ).to be false
      expect( [ -111 ].to_java(:double) == arr2 ).to be false

      expect( arr1 == [ -111 ] ).to be false
      expect( arr1 == [ -111, 101010.99 ] ).to be true
      expect( [ -111, 101010.99 ] == arr2 ).to be true
      expect( arr1.eql? [ -111, 101010.99 ] ).to be false
      expect( arr2 === [ -111, 101010.99 ] ).to be true
    end

    it "inspects to show type and contents" do
      arr = [1.0, 1.1, 1.2].to_java :double
      expect(arr.inspect).to eql '#<Java::double[3]: [1.0, 1.1, 1.2]>'
    end

    it "detects element using include?" do
      arr = Java::double[3].new
      arr[0] = 111
      arr[1] = 0.001
      arr[2] = -1234560000.789

      expect(arr.include?(111)).to be true
      expect(arr.include?(111.1)).to be false
      expect(arr.include?(0.0011)).to be false
      expect(arr.include?(0.001)).to be true
      expect(arr.include?(-1234560000.789)).to be true
      expect(arr.include?(-1234560000.79)).to be false
      expect(arr.include?(nil)).to be false
      expect(arr.include?('x')).to be false
    end
  end

  describe "float" do
    it "should be possible to create empty array" do
      arr = Java::float[0].new
      expect(arr.java_class.to_s).to eq("float[]")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::float[10].new
      expect(arr.java_class.name).to eq("[F")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::float[10,10].new
      expect(arr.java_class.name).to eq("[[F")
      expect(arr).not_to be_empty
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

    it 'handles equality to another array' do
      arr1 = [-111, 101010.99].to_java :float
      arr2 = Java::float[2].new
      arr2[0] = -111; arr2[1] = 101010.99
      expect( arr1 == arr2 ).to be true
      expect( arr1.eql? arr2 ).to be true
      expect( [-111, 101010.99].to_java(:double).eql? arr2 ).to be false
      expect( arr1 == [ -111.1, 101010.99 ] ).to be false
      expect( [ -111 ].to_java(:float) == arr2 ).to be false

      expect( arr1 === arr1 ).to be true
      expect( arr2 === arr1 ).to be true
      expect( arr1.class === arr2 ).to be true

      expect( arr1 == [ -111 ] ).to be false
      expect( arr1 == [ -111, 101010.99 ] ).to be true
      expect( [ -111, 101010.99 ] == arr2 ).to be true
      expect( arr1.eql? [ -111, 101010.99 ] ).to be false
      expect( arr1 === [ -111, 101010.99 ] ).to be true
    end

    it "inspects to show type and contents" do
      arr = [1.0, 1.1, 1.2].to_java :float
      expect(arr.inspect).to eql '#<Java::float[3]: [1.0, 1.1, 1.2]>'
    end

    it "detects element using include?" do
      arr = Java::float[3].new
      arr[0] = 111
      arr[1] = 0.001
      arr[2] = -123456.789

      expect(arr.include?(111)).to be true
      expect(arr.include?(111.1)).to be false
      expect(arr.include?(0.0011)).to be false
      expect(arr.include?(0.001)).to be true
      expect(arr.include?(-123456.789)).to be true
      # expect(arr.include?(-123456.79)).to be false
      expect(arr.include?(-123456.8)).to be false
      expect(arr.include?(nil)).to be false
      expect(arr.include?('x')).to be false
    end
  end

  describe "int" do
    it "should be possible to create empty array" do
      arr = Java::int[0].new
      expect(arr.java_class.name).to eq("[I")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::int[10].new
      expect(arr.java_class.name).to eq("[I")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::int[10,10].new
      expect(arr.java_class.name).to eq("[[I")
      expect(arr).not_to be_empty
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

    it 'handles equality to another array' do
      arr1 = [-111, 12345678].to_java :int
      arr2 = Java::int[2].new
      arr2[0] = 111; arr2[1] = 12345678
      arr2[0] = arr2[0] * -1
      expect( arr1 == arr2 ).to be true
      expect( arr1.eql? arr2 ).to be true
      expect( [-111, 12345678].to_java(:long).eql? arr2 ).to be false
      expect( [ -111 ].to_java(:int) == arr2 ).to be false

      expect( arr1 == [ -111 ] ).to be false
      expect( arr1 == [ -111, 12345678 ] ).to be true
      expect( [ -111, 12345678 ] == arr2 ).to be true
      expect( arr1.eql? [ -111, 12345678 ] ).to be false

      expect( arr1 === arr1 ).to be true
      expect( arr2 === arr1 ).to be true
      expect( arr1.class === arr2 ).to be true
    end

    it "inspects to show type and contents" do
      arr = [13, 42, 120].to_java :int
      expect(arr.inspect).to eql '#<Java::int[3]: [13, 42, 120]>'
    end

    it "detects element using include?" do
      arr = Java::int[8].new
      arr[0] = -1
      arr[1] = 22
      arr[3] = 2147483647
      arr[6] = -111111111

      expect(arr.include?(0)).to be true
      expect(arr.include?(1)).to be false
      expect(arr.include?(22)).to be true
      expect(arr.include?(-111111111)).to be true
      expect(arr.include?(-1111111111111)).to be false
      expect(arr.include?(2147483648)).to be false
      expect(arr.include?(2147483646)).to be false
      expect(arr.include?(2147483647)).to be true
      expect(arr.include?(nil)).to be false
      expect(arr.include?('x')).to be false
    end
  end

  describe "long" do
    it "should be possible to create empty array" do
      arr = Java::long[0].new
      expect(arr.java_class.to_s).to eq("long[]")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::long[10].new
      expect(arr.java_class.name).to eq("[J")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::long[10,10].new
      expect(arr.java_class.name).to eq("[[J")
      expect(arr).not_to be_empty
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

    it 'handles equality to another array' do
      arr1 = [111, 2222222222].to_java :long
      arr2 = Java::long[2].new
      arr2[0] = 111; arr2[1] = 2222222222
      expect( arr1 == arr2 ).to be true
      expect( arr1.eql? arr2 ).to be true
      expect( [ 111 ].to_java(:long) == arr2 ).to be false

      expect( arr1 == [ 111 ] ).to be false
      expect( arr1 == [ 111, 2222222222 ] ).to be true
      expect( [ 111, 2222222222 ] == arr2 ).to be true
      expect( arr1.eql? [ 111, 2222222222 ] ).to be false
    end

    it "inspects to show type and contents" do
      arr = [13, 42, 120000000000].to_java :long
      expect(arr.inspect).to eql '#<Java::long[3]: [13, 42, 120000000000]>'
    end

    it "clones" do
      arr = Java::long[5].new
      arr[1] = 1

      dup = arr.clone
      expect( dup.object_id ).to_not eql arr.object_id
      dup[1] = 11
      expect(arr[1]).to eq(1)
    end

    it "detects element using include?" do
      arr = Java::long[8].new
      arr[0] = -1
      arr[1] = 22
      arr[3] = 2147483647000
      arr[6] = -111111111000

      expect(arr.include?(0)).to be true
      expect(arr.include?(1)).to be false
      expect(arr.include?(22)).to be true
      expect(arr.include?(-111111111)).to be false
      expect(arr.include?(-111111111000)).to be true
      expect(arr.include?(2147483647001)).to be false
      expect(arr.include?(2147483647000)).to be true
      expect(arr.include?(nil)).to be false
      expect(arr.include?('x')).to be false
    end
  end

  describe "short" do
    it "should be possible to create empty array" do
      arr = Java::short[0].new
      expect(arr.java_class.name).to eq("[S")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = Java::short[10].new
      expect(arr.java_class.name).to eq("[S")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = Java::short[10,10].new
      expect(arr.java_class.to_s).to eq("short[][]")
      expect(arr).not_to be_empty
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

    it "inspects to show type and contents" do
      arr = [13, 42, 120].to_java :short
      expect(arr.inspect).to eql '#<Java::short[3]: [13, 42, 120]>'
    end

    it "dups" do
      arr = Java::short[5].new
      arr[1] = 1

      dup = arr.dup
      expect( dup.object_id ).to_not eql arr.object_id
      dup[1] = 11
      expect(arr[1]).to eq(1)
    end
  end

  describe "string" do
    it "should be possible to create empty array" do
      arr = java.lang.String[0].new
      expect(arr.java_class.to_s).to eq("java.lang.String[]")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = java.lang.String[10].new
      expect(arr.java_class.name).to eq("[Ljava.lang.String;")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = java.lang.String[10,10].new
      expect(arr.java_class.to_s).to eq("java.lang.String[][]")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      # Check with symbol name
      arr = ["foo", :bar].to_java :string
      expect(arr.java_class.name).to eq("[Ljava.lang.String;")

      expect(arr.length).to eq(2)

      expect(arr[0]).to eq("foo")
      expect(arr[1]).to eq("bar")


      # Check with type
      arr = ["foo", :bar].to_java java.lang.String
      expect(arr.java_class.name).to eq("[Ljava.lang.String;")

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
      arr = ['foo', 'bar', 'bar'].to_java :string
      expect(arr.inspect).to eql '#<Java::JavaLang::String[3]: ["foo", "bar", "bar"]>'
    end

    it "dups" do
      arr = java.lang.String[3].new
      arr[1] = '000'

      dup = arr.dup
      expect( dup.object_id ).to_not eql arr.object_id
      dup[1] = 'DUP'
      expect(arr[1]).to eq('000')
    end
  end

  describe "Object" do
    it "should be possible to create empty array" do
      arr = java.util.HashMap[0].new
      expect(arr.java_class.name).to eq("[Ljava.util.HashMap;")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = java.util.HashMap[10].new
      expect(arr.java_class.name).to eq("[Ljava.util.HashMap;")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = java.util.HashMap[10,10].new
      expect(arr.java_class.name).to eq("[[Ljava.util.HashMap;")
      expect(arr).not_to be_empty
    end

    it "should be possible to create primitive array from Ruby array" do
      h1 = java.util.HashMap.new
      h1["foo"] = "max"

      h2 = java.util.HashMap.new
      h2["max"] = "foo"

      arr = [h1, h2].to_java java.util.HashMap
      expect(arr.java_class.name).to eq("[Ljava.util.HashMap;")

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

      expect(ary[0].class).to eq(Integer)
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
      arr = [java.lang.Object.new, Object.new].to_java :object
      expect(arr.inspect).to start_with '#<Java::JavaLang::Object[2]: '
      expect(arr.inspect).to match(/\[#<Java::JavaLang::Object:0x[0-9a-f]+>, #<Object:0x[0-9a-f]+>]>$/)
    end
  end

  it "inspects a Boolean wrapper like a primitive" do
    arr = java.lang.Boolean[3].new
    arr[0] = java.lang.Boolean::TRUE
    arr[1] = java.lang.Boolean.new(false)
    expect(arr.inspect).to eql '#<Java::JavaLang::Boolean[3]: [true, false, nil]>'
  end

  it "inspects a Character wrapper Ruby style" do
    arr = java.lang.Character[3].new

    c = java.lang.Character.new(48)
    expect( c.to_s ).to eql '0'
    expect( c.inspect ).to eql "'0'"
    arr[0] = c

    c = java.lang.Character.new(0)
    expect( c.to_s ).to eql "\u0000"
    expect( c.inspect ).to eql "'\u0000'"
    arr[1] = c

    expect(arr.inspect).to eql "#<Java::JavaLang::Character[3]: ['0', '\u0000', nil]>"
  end

  describe "Class ref" do
    it "should be possible to create empty array" do
      arr = java.lang.Class[0].new
      expect(arr.java_class.to_s).to eq("java.lang.Class[]")
      expect(arr).to be_empty
    end

    it "should be possible to create uninitialized single dimensional array" do
      arr = java.lang.Class[10].new
      expect(arr.java_class.to_s).to eq("java.lang.Class[]")
      expect(arr).not_to be_empty
    end

    it "should be possible to create uninitialized multi dimensional array" do
      arr = java.lang.Class[10,10].new
      expect(arr.java_class.name).to eq("[[Ljava.lang.Class;")
      expect(arr).not_to be_empty
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

    it "inspects to show type and contents" do
      h1 = java.util.Map::Entry.java_class
      h2 = Java::jnr::posix::Times.java_class

      arr = [h1, h2].to_java java.lang.Class
      expect(arr.inspect).to match(/^\#<Java::JavaLang::Class\[2\]: \[#<Java::JavaLang::Class: java.util.Map.Entry>, #<Java::JavaLang::Class: jnr.posix.Times>]>$/)
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
    expect(arr.first).to eql [1]
    expect(arr.first.first).to be_kind_of(Integer)
  end

  it "returns a new array from to_a" do
    j_arr = [ 1, 2 ].to_java
    r_arr = j_arr.to_a
    j_arr[0] = 3
    expect( r_arr[0] ).to eql 1

    j_arr = [1, 2].to_java(:int)
    r_arr = j_arr.entries
    j_arr[0] = 3
    expect( r_arr[0] ).to eql 1
  end

  it "returns a new array from to_ary" do
    j_arr = [ 1, 2 ].to_java
    r_arr = j_arr.to_ary
    j_arr[0] = 3
    expect( r_arr[0] ).to eql 1
  end

  it 'supports #first (Enumerable)' do
    arr = [ '1', '2', '3' ].to_java('java.lang.String')
    expect( arr.first ).to eql '1'
    expect( arr.first(2) ).to eql [ '1', '2' ]

    arr = [ 1, 2, 3 ].to_java(:int)
    expect( arr.first ).to eql 1
    expect( arr.first(1) ).to eql [ 1 ]
    expect( arr.first(5) ).to eql [ 1, 2, 3 ]

    arr = Java::byte[0].new
    expect( arr.first ).to be nil
    expect( arr.first(1) ).to eql []
  end

  it 'supports #last (like Ruby Array)' do
    arr = [ '1', '2', '3' ].to_java('java.lang.String')
    expect( arr.last ).to eql '3'
    expect( arr.last(2) ).to eql [ '2', '3' ]

    arr = [ 1, 2, 3 ].to_java(:int)
    expect( arr.last ).to eql 3
    expect( arr.last(1) ).to eql [ 3 ]
    expect( arr.last(8) ).to eql [ 1, 2, 3 ]

    arr = Java::byte[0].new
    expect( arr.last ).to be nil
    expect( arr.last(1) ).to eql []
  end

  it 'counts' do
    arr = [ '1', '2', '3' ].to_java('java.lang.String')
    expect( arr.count ).to eql 3
    expect( arr.count('2') ).to eql 1
    expect( arr.count(1) ).to eql 0

    arr = [ 1, 2, 2 ].to_java(:int)
    expect( arr.count { |e| e > 0 } ).to eql 3
    expect( arr.count(2) ).to eql 2

    arr = Java::byte[0].new
    expect( arr.count ).to eql 0
  end

  it 'each return self' do
    arr = Java::int[5].new
    expect( arr.each { |i| i } ).to be arr
    arr = [].to_java
    expect( arr.each { |i| i } ).to be arr
  end

  it 'each without block' do
    arr = Java::float[5].new; arr[1] = 1.0
    expect( enum = arr.each ).to be_a Enumerator
    expect( enum.next ).to eql 0.0
    expect( enum.next ).to eql 1.0
  end

  it 'each with index' do
    arr = Java::byte[5].new
    counter = 0
    ret = arr.each_with_index { |el, i| expect(el).to eql 0; expect(i).to eql(counter); counter += 1 }
    expect( counter ).to eql 5
    expect( ret ).to eql arr

    arr = Java::long[4].new; arr[1] = 1; arr[2] = 1; arr[3] = 3
    expect( enum = arr.each_with_index ).to be_a Enumerator
    expect( enum.next ).to eql [0, 0]
    expect( enum.next ).to eql [1, 1]
    expect( enum.next ).to eql [1, 2]
    expect( enum.next ).to eql [3, 3]
  end

  describe '#at' do

    it "returns the (n+1)'th element for the passed index n" do
      a = [1, 2, 3, 4, 5, 6].to_java
      expect( a.at(0) ).to eql 1
      expect( a.at(1) ).to eql 2
      expect( a.at(5) ).to eql 6
    end

    it "returns nil if the given index is greater than or equal to the array's length" do
      a = [1, 2, 3, 4, 5, 6].to_java(:int)
      expect( a.at(6) ).to eql nil
      expect( a.at(7) ).to eql nil
    end

    it "returns the (-n)'th element from the last, for the given negative index n" do
      a = [1, 2, 3, 4, 5, 6].to_java(:long)
      expect( a.at(-1) ).to eql 6
      expect( a.at(-2) ).to eql 5
      expect( a.at(-6) ).to eql 1
    end

    it "returns nil if the given index is less than -len, where len is length of the array"  do
      a = [1, 2, 3, 4, 5, 6].to_java
      expect( a.at(-7) ).to eql nil
      expect( a.at(-8) ).to eql nil
    end

    it "tries to convert the passed argument to an Integer using #to_int" do
      a = ["a", "b", "c"].to_java
      expect( a.at(0.5) ).to eql "a"

      obj = java.lang.Integer.new(2)
      expect( a.at(obj) ).to eql "c"
    end

    it "raises a TypeError when the passed argument can't be coerced to Integer" do
        expect { [].to_java.at("cat") }.to raise_error(TypeError)
    end

    it "raises an ArgumentError when 2 or more arguments are passed" do
      expect { [:a, :b].to_java.at(0, 1) }.to raise_error(ArgumentError)
    end

  end

  describe '#[]' do

    it "returns [] if the index is valid but length is zero with [index, length]" do
      expect( [ "a", "b", "c", "d", "e" ].to_java[0, 0] ).to eql [].to_java
      expect( [ "a", "b", "c", "d", "e" ].to_java[2, 0] ).to eql [].to_java
    end

    # This is by design. It is in the official documentation.
    # it "returns [] if index == array.size with [index, length]" do
    #   expect( %w|a b c d e|.to_java[5, 2] ).to eql [].to_java
    # end

    it "returns nil if index > array.size with [index, length]" do
      expect( %w|a b c d e|.to_java[6, 2] ).to be nil
    end

    it "returns nil if length is negative with [index, length]" do
      expect( %w|a b c d e|.to_java[3, -1] ).to be nil
    end

    it 'raises IndexError for invalid [index]' do
      expect { [ "a", "b", "c", "d", "e" ].to_java[-10] }.to raise_error(IndexError)
      expect { [ "a", "b", "c", "d", "e" ].to_java[5] }.to raise_error(IndexError)
    end

    it "returns nil if no requested index is in the array with [m..n]" do
      expect( %w|a b c d e|.to_java[6..10] ).to be nil
    end

    it "returns sub-array even if upper range is out of array [m..n]" do
      expect( %w|a b c d e|.to_java[3..10] ).to eql ['d', 'e'].to_java
    end

    it "returns nil if range start is not in the array with [m..n]" do
      expect( [ "a", "b", "c", "d", "e" ].to_java[10..12] ).to be nil
      expect( [ "a", "b", "c", "d", "e" ].to_java[-10..2] ).to be nil
    end

    it "returns a subarray where m, n negatives and m < n with [m..n]" do
      expect( [ "a", "b", "c", "d", "e" ].to_java[-3..-2] ).to eql ["c", "d"].to_java
    end

    it "returns an empty array when m == n with [m...n]" do
      expect( [1, 2, 3, 4, 5].to_java(:int)[1...1] ).to eql [].to_java(:int)
    end

    it "returns an empty array with [0...0]" do
      expect( [1, 2, 3, 4, 5].to_java(:long)[0...0] ).to eql [].to_java(:long)
    end

    it "returns an array containing the first element with [0..0]" do
      expect( [1, 2, 3, 4, 5].to_java(:byte)[0..0] ).to eql [1].to_java(:byte)
    end

    it "returns the entire array with [0..-1]" do
      expect( [1, 2, 3, 4, 5].to_java[0..-1] ).to eql [1, 2, 3, 4, 5].to_java
    end

    it "returns all but the last element with [0...-1]" do
      expect( [1, 2, 3, 4, 5].to_java[0...-1] ).to eql [1, 2, 3, 4].to_java
    end

    it "returns [3] for [2..-1] out of [1, 2, 3]" do
      expect( [1, 2, 3].to_java[2..-1] ).to eql [3].to_java
    end

    it "returns an empty array when m > n and m, n are positive with [m..n]" do
      expect( [1, 2, 3, 4, 5].to_java[3..2] ).to eql [].to_java
    end

    it "returns an empty array when m > n and m, n are negative with [m..n]" do
      expect( [1, 2, 3, 4, 5].to_java(:long)[-2..-3] ).to eql [].to_java(:long)
    end

  end

  describe "#dig" do

    it 'returns #at with one arg' do
      expect( [1].to_java.dig(0) ).to eql 1
      expect( [1].to_java(:byte).dig(0) ).to eql 1
      expect( [1].to_java.dig(1) ).to be nil

      expect( Java::long[2].new.dig(1) ).to be 0
      expect( Java::long[2].new.dig(2) ).to be nil
      expect( java.lang.String[1].new.dig(1) ).to be nil
    end

    it 'recurses array elements' do
      arr = Java::int[2].new ; arr[0] = 2; arr[1] = 3
      a = [ [ 1, arr ] ].to_java
      expect( a.dig(0, 0) ).to eql 1
      expect( a.dig(0, 1, 1) ).to eql 3
      expect( a.dig(0, -1, 0) ).to eql 2
    end

    it 'returns the nested value specified if the sequence includes a key' do
      a = [42, { foo: :bar }].to_java :Object
      expect( a.dig(1, :foo) ).to eql :bar
    end

    it 'raises a TypeError for a non-numeric index' do
      expect {  ['a'].dig(:first) }.to raise_error(TypeError)
    end

    it 'raises a TypeError if any intermediate step does not respond to #dig' do
      a = [1, 2].to_java(:int)
      expect { a.dig(0, 1) }.to raise_error(TypeError)
    end

  end

end
