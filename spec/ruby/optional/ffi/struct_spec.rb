require File.expand_path('../spec_helper', __FILE__)

describe "Struct tests" do
  it "Struct#[:pointer]" do
    magic = 0x12345678
    mp = FFI::MemoryPointer.new :long
    mp.put_long(0, magic)
    smp = FFI::MemoryPointer.new :pointer
    smp.put_pointer(0, mp)
    s = FFISpecs::PointerMember.new smp
    s[:pointer].should == mp
  end

  it "Struct#[:pointer].nil? for NULL value" do
    magic = 0x12345678
    mp = FFI::MemoryPointer.new :long
    mp.put_long(0, magic)
    smp = FFI::MemoryPointer.new :pointer
    smp.put_pointer(0, nil)
    s = FFISpecs::PointerMember.new smp
    s[:pointer].null?.should == true
  end

  it "Struct#[:pointer]=" do
    magic = 0x12345678
    mp = FFI::MemoryPointer.new :long
    mp.put_long(0, magic)
    smp = FFI::MemoryPointer.new :pointer
    s = FFISpecs::PointerMember.new smp
    s[:pointer] = mp
    smp.get_pointer(0).should == mp
  end

  it "Struct#[:pointer]=struct" do
    magic = 0x12345678
    smp = FFI::MemoryPointer.new :pointer
    s = FFISpecs::PointerMember.new smp
    lambda { s[:pointer] = s }.should_not raise_error
  end

  it "Struct#[:pointer]=nil" do
    smp = FFI::MemoryPointer.new :pointer
    s = FFISpecs::PointerMember.new smp
    s[:pointer] = nil
    smp.get_pointer(0).null?.should == true
  end

  it "Struct#[:string]" do
    magic = "test"
    mp = FFI::MemoryPointer.new 1024
    mp.put_string(0, magic)
    smp = FFI::MemoryPointer.new :pointer
    smp.put_pointer(0, mp)
    s = FFISpecs::StringMember.new smp
    s[:string].should == magic
  end

  it "Struct#[:string].nil? for NULL value" do
    smp = FFI::MemoryPointer.new :pointer
    smp.put_pointer(0, nil)
    s = FFISpecs::StringMember.new smp
    s[:string].nil?.should == true
  end

  it "Struct#layout works with :name, :type pairs" do
    pair_layout = Class.new(FFI::Struct) do
      layout :a, :int, :b, :long_long
    end

    ll_off = (FFI::TYPE_UINT64.alignment == 4 ? 4 : 8)
    pair_layout.size.should == (ll_off + 8)

    mp = FFI::MemoryPointer.new(pair_layout.size)
    s = pair_layout.new(mp)

    s[:a] = 0x12345678
    mp.get_int(0).should == 0x12345678

    s[:b] = 0xfee1deadbeef
    mp.get_int64(ll_off).should == 0xfee1deadbeef
  end

  it "Struct#layout works with :name, :type, offset tuples" do
    pair_layout = Class.new(FFI::Struct) do
      layout :a, :int, 0, :b, :long_long, 4
    end

    pair_layout.size.should == (FFI::TYPE_UINT64.alignment == 4 ? 12 : 16)

    mp = FFI::MemoryPointer.new(pair_layout.size)
    s = pair_layout.new(mp)

    s[:a] = 0x12345678
    mp.get_int(0).should == 0x12345678

    s[:b] = 0xfee1deadbeef
    mp.get_int64(4).should == 0xfee1deadbeef
  end

  it "Struct#layout works with mixed :name,:type and :name,:type,offset" do
    mixed_layout = Class.new(FFI::Struct) do
      layout :a, :int, :b, :long_long, 4
    end

    mixed_layout.size.should == (FFI::TYPE_UINT64.alignment == 4 ? 12 : 16)

    mp = FFI::MemoryPointer.new(mixed_layout.size)
    s = mixed_layout.new(mp)

    s[:a] = 0x12345678
    mp.get_int(0).should == 0x12345678

    s[:b] = 0xfee1deadbeef
    mp.get_int64(4).should == 0xfee1deadbeef
  end

  rb_maj, rb_min = RUBY_VERSION.split('.')
  if rb_maj.to_i >= 1 && rb_min.to_i >= 9 || RUBY_PLATFORM =~ /java/
    it "Struct#layout withs with a hash of :name => type" do
      hash_layout = Class.new(FFI::Struct) do
        layout :a => :int, :b => :long_long
      end

      ll_off = (FFI::TYPE_UINT64.alignment == 4? 4 : 8)
      hash_layout.size.should == (ll_off + 8)

      mp = FFI::MemoryPointer.new(hash_layout.size)
      s = hash_layout.new(mp)

      s[:a] = 0x12345678
      mp.get_int(0).should == 0x12345678

      s[:b] = 0xfee1deadbeef
      mp.get_int64(ll_off).should == 0xfee1deadbeef
      end
  end

  it "Can use Struct subclass as parameter type" do
    lambda {
      Module.new do
        extend FFI::Library
        ffi_lib FFISpecs::LIBRARY

        struct = Class.new(FFI::Struct) { layout :c, :char }
        attach_function :struct_field_s8, [ struct ], :char
      end
    }.should_not raise_error
  end

  it "Can use Struct subclass as IN parameter type" do
    lambda {
      Module.new do
        extend FFI::Library
        ffi_lib FFISpecs::LIBRARY

        struct = Class.new(FFI::Struct) { layout :c, :char }
        attach_function :struct_field_s8, [ struct.in ], :char
      end
    }.should_not raise_error
  end

  it "Can use Struct subclass as OUT parameter type" do
    lambda {
      Module.new do
        extend FFI::Library
        ffi_lib FFISpecs::LIBRARY

        struct = Class.new(FFI::Struct) { layout :c, :char }
        attach_function :struct_field_s8, [ struct.out ], :char
      end
    }.should_not raise_error
  end

  it "can be passed directly as a :pointer parameter" do
    struct = Class.new(FFI::Struct) do
      layout :i, :int
    end

    s = struct.new
    s[:i] = 0x12
    FFISpecs::LibTest.ptr_ret_int32_t(s, 0).should == 0x12
  end

  it ":char member aligned correctly" do
    align_char = Class.new(FFI::Struct) do
      layout :c, :char, :v, :char
    end

    s = align_char.new
    s[:v] = 0x12
    FFISpecs::LibTest.struct_align_s8(s.pointer).should == 0x12
  end

  it ":short member aligned correctly" do
    align_short = Class.new(FFI::Struct) do
      layout :c, :char, :v, :short
    end

    s = align_short.alloc_in
    s[:v] = 0x1234
    FFISpecs::LibTest.struct_align_s16(s.pointer).should == 0x1234
  end

  it ":int member aligned correctly" do
    align_int = Class.new(FFI::Struct) do
      layout :c, :char, :v, :int
    end

    s = align_int.alloc_in
    s[:v] = 0x12345678
    FFISpecs::LibTest.struct_align_s32(s.pointer).should == 0x12345678
  end

  it ":long_long member aligned correctly" do
    align_long_long = Class.new(FFI::Struct) do
      layout :c, :char, :v, :long_long
    end

    s = align_long_long.alloc_in
    s[:v] = 0x123456789abcdef0
    FFISpecs::LibTest.struct_align_s64(s.pointer).should == 0x123456789abcdef0
  end

  it ":long member aligned correctly" do
    align_long = Class.new(FFI::Struct) do
      layout :c, :char, :v, :long
    end

    s = align_long.alloc_in
    s[:v] = 0x12345678
    FFISpecs::LibTest.struct_align_long(s.pointer).should == 0x12345678
  end

  it ":float member aligned correctly" do
    align_float = Class.new(FFI::Struct) do
      layout :c, :char, :v, :float
    end

    s = align_float.alloc_in
    s[:v] = 1.23456
    (FFISpecs::LibTest.struct_align_f32(s.pointer) - 1.23456).abs.should < 0.00001
  end

  it ":double member aligned correctly" do
    align_double = Class.new(FFI::Struct) do
      layout :c, :char, :v, :double
    end

    s = align_double.alloc_in
    s[:v] = 1.23456789
    (FFISpecs::LibTest.struct_align_f64(s.pointer) - 1.23456789).abs.should < 0.00000001
  end

  it ":ulong, :pointer struct" do
    ulp_struct = Class.new(FFI::Struct) do
      layout :ul, :ulong, :p, :pointer
    end

    s = ulp_struct.alloc_in
    s[:ul] = 0xdeadbeef
    s[:p] = FFISpecs::LibTest.ptr_from_address(0x12345678)
    s.pointer.get_ulong(0).should == 0xdeadbeef
  end

  def self.test_num_field(type, v)
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

  it "Can have CallbackInfo struct field" do
    s = FFISpecs::CallbackMember::TestStruct.new
    add_proc = lambda { |a, b| a+b }
    sub_proc = lambda { |a, b| a-b }
    s[:add] = add_proc
    s[:sub] = sub_proc
    FFISpecs::CallbackMember.struct_call_add_cb(s.pointer, 40, 2).should == 42
    FFISpecs::CallbackMember.struct_call_sub_cb(s.pointer, 44, 2).should == 42
  end

  it "Can return its members as a list" do
    klass = Class.new(FFI::Struct)
    klass.layout :a, :int, :b, :int, :c, :int
    klass.members.should include(:a, :b, :c)
  end

  it "Can return its instance members and values as lists" do
    klass = Class.new(FFI::Struct)
    klass.layout :a, :int, :b, :int, :c, :int

    s = klass.new
    s.members.should include(:a, :b, :c)
    s[:a] = 1
    s[:b] = 2
    s[:c] = 3
    s.values.should include(1, 2, 3)
  end

  it "returns an ordered field/offset pairs array" do
    klass = Class.new(FFI::Struct)
    klass.layout :a, :int, :b, :int, :c, :int

    s = klass.new
    s.offsets.should == [[:a, 0], [:b, 4], [:c, 8]]
    klass.offsets.should == [[:a, 0], [:b, 4], [:c, 8]]
  end

  it "Struct#offset_of returns offset of field within struct" do
    klass = Class.new(FFI::Struct)
    klass.layout :a, :int, :b, :int, :c, :int

    klass.offset_of(:a).should == 0
    klass.offset_of(:b).should == 4
    klass.offset_of(:c).should == 8
  end
end

describe FFI::Struct, ' with a nested struct field'  do
  before do
    @cs = FFISpecs::LibTest::ContainerStruct.new
  end

  it "aligns correctly nested struct field" do
    @cs[:ns][:i] = 123
    FFISpecs::LibTest.struct_align_nested_struct(@cs.to_ptr).should == 123
  end

  it "calculates Container size (in bytes)" do
    FFISpecs::LibTest::ContainerStruct.size.should == 8
  end

  it "returns a Struct object when the field is accessed" do
    @cs[:ns].is_a?(FFI::Struct).should be_true
  end

  it "reads a value from memory" do
    @cs = FFISpecs::LibTest::ContainerStruct.new(FFISpecs::LibTest.struct_make_container_struct(123))
    @cs[:ns][:i].should == 123
  end

  it "writes a value to memory" do
    @cs = FFISpecs::LibTest::ContainerStruct.new(FFISpecs::LibTest.struct_make_container_struct(123))
    @cs[:ns][:i] = 456
    FFISpecs::LibTest.struct_align_nested_struct(@cs.to_ptr).should == 456
  end
end

describe FFI::Struct, ' with an array field'  do
  before do
    @s = FFISpecs::LibTest::StructWithArray.new
  end

  it "calculates StructWithArray size (in bytes)" do
    FFISpecs::LibTest::StructWithArray.size.should == 24
  end

  it "reads values from memory" do
    @s = FFISpecs::LibTest::StructWithArray.new(FFISpecs::LibTest.struct_make_struct_with_array(0, 1, 2, 3, 4))
    @s[:a].to_a.should == [0, 1, 2, 3, 4]
  end

  it "caches array object for successive calls" do
    @s[:a].object_id.should == @s[:a].object_id
  end

  it "returns the size of the array field in bytes" do
    @s = FFISpecs::LibTest::StructWithArray.new(FFISpecs::LibTest.struct_make_struct_with_array(0, 1, 2, 3, 4))
    @s[:a].size.should == 20
  end

  it "iterates through the array elements" do
    @s = FFISpecs::LibTest::StructWithArray.new(FFISpecs::LibTest.struct_make_struct_with_array(0, 1, 2, 3, 4))
    @s[:a].each_with_index { |elem, i| elem.should == i }
  end

  it "returns the pointer to the array" do
    @s = FFISpecs::LibTest::StructWithArray.new(FFISpecs::LibTest.struct_make_struct_with_array(0, 1, 2, 3, 4))
    @s[:a].to_ptr.should == FFISpecs::LibTest::struct_field_array(@s.to_ptr)
  end
end

describe "BuggedStruct" do
  it "returns its correct size" do
    FFISpecs::LibTest::BuggedStruct.size.should == FFISpecs::LibTest.bugged_struct_size
  end

  it "offsets within struct should be correct" do
    FFISpecs::LibTest::BuggedStruct.offset_of(:visible).should == 0
    FFISpecs::LibTest::BuggedStruct.offset_of(:x).should == 4
    FFISpecs::LibTest::BuggedStruct.offset_of(:y).should == 8
    FFISpecs::LibTest::BuggedStruct.offset_of(:rx).should == 12
    FFISpecs::LibTest::BuggedStruct.offset_of(:ry).should == 14
    FFISpecs::LibTest::BuggedStruct.offset_of(:order).should == 16
    FFISpecs::LibTest::BuggedStruct.offset_of(:size).should == 17
  end

  it "returns correct field/offset pairs" do
    FFISpecs::LibTest::BuggedStruct.offsets.sort do |a, b|
      a[1] <=> b[1]
    end.should == [[:visible, 0], [:x, 4], [:y, 8], [:rx, 12], [:ry, 14], [:order, 16], [:size, 17]]
  end
end

describe "Struct allocation" do
  before :all do
    @klass = Class.new(FFI::Struct)
    @klass.layout :i, :uint
  end

  it "MemoryPointer.new(Struct, 2)" do
    p = FFI::MemoryPointer.new(@klass, 2)
    p.total.should == 8
    p.type_size.should == 4
    p.put_uint(4, 0xdeadbeef)
    @klass.new(p[1])[:i].should == 0xdeadbeef
    p[1].address.should == (p[0].address + 4)
  end

  it "Buffer.new(Struct, 2)" do
    p = FFI::Buffer.new(@klass, 2)
    p.total.should == 8
    p.type_size.should == 4
    p.put_uint(4, 0xdeadbeef)
    @klass.new(p[1])[:i].should == 0xdeadbeef
  end
end
