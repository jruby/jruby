require 'ffi'

describe "Struct aligns fields correctly" do
  it "char, followed by an int" do
    class CIStruct < FFI::Struct
      layout :c => :char, :i => :int
    end
    CIStruct.size.should == 8
  end
  it "short, followed by an int" do
    class SIStruct < FFI::Struct
      layout :s => :short, :i => :int
    end
    SIStruct.size.should == 8
  end
  it "int, followed by an int" do
    class IIStruct < FFI::Struct
      layout :i1 => :int, :i => :int
    end
    IIStruct.size.should == 8
  end
  it "long long, followed by an int" do
    class LLIStruct < FFI::Struct
      layout :l => :long_long, :i => :int
    end
    LLIStruct.size.should == (FFI::TYPE_UINT64.alignment == 4 ? 12 : 16)
  end
end

describe "Struct tests" do
  StructTypes = {
    's8' => :char,
    's16' => :short,
    's32' => :int,
    's64' => :long_long,
    'long' => :long,
    'f32' => :float,
    'f64' => :double
  }
  class PointerMember < FFI::Struct
    layout :pointer, :pointer
  end
  class StringMember < FFI::Struct
    layout :string, :string
  end
  it "Struct#[:pointer]" do
    magic = 0x12345678
    mp = FFI::MemoryPointer.new :long
    mp.put_long(0, magic)
    smp = FFI::MemoryPointer.new :pointer
    smp.put_pointer(0, mp)
    s = PointerMember.new smp
    s[:pointer].should == mp
  end
  it "Struct#[:pointer].nil? for NULL value" do
    magic = 0x12345678
    mp = FFI::MemoryPointer.new :long
    mp.put_long(0, magic)
    smp = FFI::MemoryPointer.new :pointer
    smp.put_pointer(0, nil)
    s = PointerMember.new smp
    s[:pointer].null?.should == true
  end
  it "Struct#[:pointer]=" do
    magic = 0x12345678
    mp = FFI::MemoryPointer.new :long
    mp.put_long(0, magic)
    smp = FFI::MemoryPointer.new :pointer
    s = PointerMember.new smp
    s[:pointer] = mp
    smp.get_pointer(0).should == mp
  end
  it "Struct#[:pointer]=struct" do
    magic = 0x12345678
    smp = FFI::MemoryPointer.new :pointer
    s = PointerMember.new smp
    lambda { s[:pointer] = s }.should_not raise_error
    foo = s[:pointer] # should not crash with a java.lang.ClassCastException: org.jruby.ext.ffi.Struct cannot be cast to org.jruby.ext.ffi.AbstractMemory
  end
  it "Struct#[:pointer]=nil" do
    smp = FFI::MemoryPointer.new :pointer
    s = PointerMember.new smp
    s[:pointer] = nil
    smp.get_pointer(0).null?.should == true
  end
  it "Struct#[:string]" do
    magic = "test"
    mp = FFI::MemoryPointer.new 1024
    mp.put_string(0, magic)
    smp = FFI::MemoryPointer.new :pointer
    smp.put_pointer(0, mp)
    s = StringMember.new smp
    s[:string].should == magic
  end
  it "Struct#[:string].nil? for NULL value" do
    smp = FFI::MemoryPointer.new :pointer
    smp.put_pointer(0, nil)
    s = StringMember.new smp
    s[:string].nil?.should == true
  end
  it "Struct#layout works with :name, :type pairs" do
    class PairLayout < FFI::Struct
      layout :a, :int, :b, :long_long
    end
    ll_off = (FFI::TYPE_UINT64.alignment == 4 ? 4 : 8)
    PairLayout.size.should == (ll_off + 8)
    mp = FFI::MemoryPointer.new(PairLayout.size)
    s = PairLayout.new mp
    s[:a] = 0x12345678
    mp.get_int(0).should == 0x12345678
    s[:b] = 0xfee1deadbeef
    mp.get_int64(ll_off).should == 0xfee1deadbeef
  end
  it "Struct#layout works with :name, :type, offset tuples" do
    class PairLayout < FFI::Struct
      layout :a, :int, 0, :b, :long_long, 4
    end
    PairLayout.size.should == (FFI::TYPE_UINT64.alignment == 4 ? 12 : 16)
    mp = FFI::MemoryPointer.new(PairLayout.size)
    s = PairLayout.new mp
    s[:a] = 0x12345678
    mp.get_int(0).should == 0x12345678
    s[:b] = 0xfee1deadbeef
    mp.get_int64(4).should == 0xfee1deadbeef
  end
  it "Struct#layout works with mixed :name,:type and :name,:type,offset" do
    class MixedLayout < FFI::Struct
      layout :a, :int, :b, :long_long, 4
    end
    MixedLayout.size.should == (FFI::TYPE_UINT64.alignment == 4 ? 12 : 16)
    mp = FFI::MemoryPointer.new(MixedLayout.size)
    s = MixedLayout.new mp
    s[:a] = 0x12345678
    mp.get_int(0).should == 0x12345678
    s[:b] = 0xfee1deadbeef
    mp.get_int64(4).should == 0xfee1deadbeef
  end
  rb_maj, rb_min = RUBY_VERSION.split('.')
  if rb_maj.to_i >= 1 && rb_min.to_i >= 9 || RUBY_PLATFORM =~ /java/
    it "Struct#layout withs with a hash of :name => type" do
      class HashLayout < FFI::Struct
        layout :a => :int, :b => :long_long
      end
      ll_off = (FFI::TYPE_UINT64.alignment == 4 ? 4 : 8)
      HashLayout.size.should == (ll_off + 8)
      mp = FFI::MemoryPointer.new(HashLayout.size)
      s = HashLayout.new mp
      s[:a] = 0x12345678
      mp.get_int(0).should == 0x12345678
      s[:b] = 0xfee1deadbeef
      mp.get_int64(ll_off).should == 0xfee1deadbeef
      end
  end
  def test_num_field(type, v)
    klass = Class.new(FFI::Struct)
    klass.layout :v, type, :dummy, :long
    
    s = klass.new
    s[:v] = v
    s.pointer.send("get_#{type.to_s}", 0).should == v
    s.pointer.send("put_#{type.to_s}", 0, 0)
    s[:v].should == 0
  end
  def self.int_field_test(type, values)
    values.each do |v|
      it "#{type} field r/w (#{v.to_s(16)})" do
        test_num_field(type, v)
      end
    end
  end
  int_field_test(:char, [ 0, 127, -128, -1 ])
  int_field_test(:uchar, [ 0, 0x7f, 0x80, 0xff ])
  int_field_test(:short, [ 0, 0x7fff, -0x8000, -1 ])
  int_field_test(:ushort, [ 0, 0x7fff, 0x8000, 0xffff ])
  int_field_test(:int, [ 0, 0x7fffffff, -0x80000000, -1 ])
  int_field_test(:uint, [ 0, 0x7fffffff, 0x80000000, 0xffffffff ])
  int_field_test(:long_long, [ 0, 0x7fffffffffffffff, -0x8000000000000000, -1 ])
  int_field_test(:ulong_long, [ 0, 0x7fffffffffffffff, 0x8000000000000000, 0xffffffffffffffff ])
  if FFI::Platform::LONG_SIZE == 32
    int_field_test(:long, [ 0, 0x7fffffff, -0x80000000, -1 ])
    int_field_test(:ulong, [ 0, 0x7fffffff, 0x80000000, 0xffffffff ])
  else
    int_field_test(:long, [ 0, 0x7fffffffffffffff, -0x8000000000000000, -1 ])
    int_field_test(:ulong, [ 0, 0x7fffffffffffffff, 0x8000000000000000, 0xffffffffffffffff ])
  end
  it ":float field r/w" do
    klass = Class.new(FFI::Struct)
    klass.layout :v, :float, :dummy, :long

    s = klass.new
    value = 1.23456
    s[:v] = value
    (s.pointer.get_float(0) - value).abs.should < 0.0001
  end
  it ":double field r/w" do
    klass = Class.new(FFI::Struct)
    klass.layout :v, :double, :dummy, :long

    s = klass.new
    value = 1.23456
    s[:v] = value
    (s.pointer.get_double(0) - value).abs.should < 0.0001
  end
  it "Can return its members as a list" do
    class TestStruct < FFI::Struct
      layout :a, :int, :b, :int, :c, :int
    end
    TestStruct.members.should include(:a, :b, :c)
  end
  it "Can return its instance members and values as lists" do
    class TestStruct < FFI::Struct
      layout :a, :int, :b, :int, :c, :int
    end
    s = TestStruct.new
    s.members.should include(:a, :b, :c)
    s[:a] = 1
    s[:b] = 2
    s[:c] = 3
    s.values.should include(1, 2, 3)
  end
  it 'should return an ordered field/offset pairs array' do
    class TestStruct < FFI::Struct
      layout :a, :int, :b, :int, :c, :int
    end
    s = TestStruct.new
    s.offsets.should == [[:a, 0], [:b, 4], [:c, 8]]
    TestStruct.offsets.should == [[:a, 0], [:b, 4], [:c, 8]]
  end
  it "Struct#offset_of returns offset of field within struct" do
    class TestStruct < FFI::Struct
      layout :a, :int, :b, :int, :c, :int
    end
    TestStruct.offset_of(:a).should == 0
    TestStruct.offset_of(:b).should == 4
    TestStruct.offset_of(:c).should == 8
  end
end
