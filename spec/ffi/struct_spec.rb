# -*- encoding: utf-8 -*-
#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))

module StructSpecs
describe "Struct aligns fields correctly" do
  it "char, followed by an int" do
    class CIStruct < FFI::Struct
      layout :c => :char, :i => :int
    end
    expect(CIStruct.size).to eq(8)
  end

  it "short, followed by an int" do
    class SIStruct < FFI::Struct
      layout :s => :short, :i => :int
    end
    expect(SIStruct.size).to eq(8)
  end

  it "int, followed by an int" do
    class IIStruct < FFI::Struct
      layout :i1 => :int, :i => :int
    end
    expect(IIStruct.size).to eq(8)
  end

  it "long long, followed by an int" do
    class LLIStruct < FFI::Struct
      layout :l => :long_long, :i => :int
    end
    expect(LLIStruct.size).to eq(FFI::TYPE_UINT64.alignment == 4 ? 12 : 16)
  end
end

module StructSpecsStructTests
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
    module LibTest
      extend FFI::Library
      ffi_lib TestLibrary::PATH
      attach_function :ptr_ret_pointer, [ :pointer, :int], :string
      begin
        attach_function :ptr_ret_int32_t, [ :pointer, :int ], :int
      rescue FFI::NotFoundError
        # NetBSD uses #define instead of typedef for these
        attach_function :ptr_ret_int32_t, :ptr_ret___int32_t, [ :pointer, :int ], :int
      end
      attach_function :ptr_from_address, [ :ulong ], :pointer
      [ 's8', 's16', 's32', 's64', 'f32', 'f64', 'long' ].each do |t|
        attach_function "struct_align_#{t}", [ :pointer ], StructTypes[t]
      end
    end
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
      expect(s[:pointer]).to eq(mp)
    end

    it "Struct#[:pointer].nil? for NULL value" do
      magic = 0x12345678
      mp = FFI::MemoryPointer.new :long
      mp.put_long(0, magic)
      smp = FFI::MemoryPointer.new :pointer
      smp.put_pointer(0, nil)
      s = PointerMember.new smp
      expect(s[:pointer].null?).to be true
    end

    it "Struct#[:pointer]=" do
      magic = 0x12345678
      mp = FFI::MemoryPointer.new :long
      mp.put_long(0, magic)
      smp = FFI::MemoryPointer.new :pointer
      s = PointerMember.new smp
      s[:pointer] = mp
      expect(smp.get_pointer(0)).to eq(mp)
    end

    it "Struct#[:pointer]=struct" do
      smp = FFI::MemoryPointer.new :pointer
      s = PointerMember.new smp
      expect { s[:pointer] = s }.not_to raise_error Exception
      expect { s[:pointer].nil? }.not_to raise_error Exception
    end

    it "Struct#[:pointer]=nil" do
      smp = FFI::MemoryPointer.new :pointer
      s = PointerMember.new smp
      s[:pointer] = nil
      expect(smp.get_pointer(0)).to be_null
    end

    it "Struct#[:string]" do
      magic = "test"
      mp = FFI::MemoryPointer.new 1024
      mp.put_string(0, magic)
      smp = FFI::MemoryPointer.new :pointer
      smp.put_pointer(0, mp)
      s = StringMember.new smp
      expect(s[:string]).to eq(magic)
    end

    it "Struct#[:string].nil? for NULL value" do
      smp = FFI::MemoryPointer.new :pointer
      smp.put_pointer(0, nil)
      s = StringMember.new smp
      expect(s[:string]).to be_nil
    end

    it "Struct#clear sets the memory to zero" do
      klass = Class.new(FFI::Struct) do
        layout :a, :int, :b, :long
      end

      s = klass.new
      s[:a] = 1
      s[:b] = 2
      s.clear
      expect(s[:a]).to eq(0)
      expect(s[:b]).to eq(0)
    end

    it "Struct#layout works with :name, :type pairs" do
      s = Class.new(FFI::Struct) do
        layout :a, :int, :b, :long_long
      end
      ll_off = (FFI::TYPE_UINT64.alignment == 4 ? 4 : 8)
      expect(s.size).to eq((ll_off + 8))
      mp = FFI::MemoryPointer.new(s.size)
      s = s.new mp
      s[:a] = 0x12345678
      expect(mp.get_int(0)).to eq(0x12345678)
      s[:b] = 0xfee1deadbeef
      expect(mp.get_int64(ll_off)).to eq(0xfee1deadbeef)
    end

    it "Struct#layout works with :name, :type, offset tuples" do
      s = Class.new(FFI::Struct) do
        layout :a, :int, 0, :b, :long_long, 4
      end
      expect(s.size).to eq((FFI::TYPE_UINT64.alignment == 4 ? 12 : 16))
      mp = FFI::MemoryPointer.new(s.size)
      s = s.new mp
      s[:a] = 0x12345678
      expect(mp.get_int(0)).to eq(0x12345678)
      s[:b] = 0xfee1deadbeef
      expect(mp.get_int64(4)).to eq(0xfee1deadbeef)
    end

    it "Struct#layout works with mixed :name,:type and :name,:type,offset" do
      class MixedLayout < FFI::Struct
        layout :a, :int, :b, :long_long, 4
      end
      expect(MixedLayout.size).to eq((FFI::TYPE_UINT64.alignment == 4 ? 12 : 16))
      mp = FFI::MemoryPointer.new(MixedLayout.size)
      s = MixedLayout.new mp
      s[:a] = 0x12345678
      expect(mp.get_int(0)).to eq(0x12345678)
      s[:b] = 0xfee1deadbeef
      expect(mp.get_int64(4)).to eq(0xfee1deadbeef)
    end

    it "Struct#layout withs with a hash of :name => type" do
      class HashLayout < FFI::Struct
        layout :a => :int, :b => :long_long
      end
      ll_off = (FFI::TYPE_UINT64.alignment == 4 ? 4 : 8)
      expect(HashLayout.size).to eq(ll_off + 8)
      mp = FFI::MemoryPointer.new(HashLayout.size)
      s = HashLayout.new mp
      s[:a] = 0x12345678
      expect(mp.get_int(0)).to eq(0x12345678)
      s[:b] = 0xfee1deadbeef
      expect(mp.get_int64(ll_off)).to eq(0xfee1deadbeef)
    end

    it "subclass overrides initialize without calling super" do
      class InitializeWithoutSuper < FFI::Struct
        layout :a, :int, :b, :long_long, :d, [:double, 2]

        def initialize(a, b)
          self[:a] = a
          self[:b] = b
          self[:d][0] = 1.2
          self[:d][1] = 3.4
        end

      end
      s = InitializeWithoutSuper.new(0x1eefbeef, 0xdeadcafebabe)
      expect(s[:a]).to eq(0x1eefbeef)
      expect(s[:b]).to eq(0xdeadcafebabe)
    end

    it "Can use Struct subclass as parameter type" do
      expect(module StructParam
        extend FFI::Library
        ffi_lib TestLibrary::PATH
        class TestStruct < FFI::Struct
          layout :c, :char
        end
        attach_function :struct_field_s8, [ TestStruct.in ], :char
      end).to be_an_instance_of FFI::Function
    end

    it "Can use Struct subclass as IN parameter type" do
      expect(module StructParam2
        extend FFI::Library
        ffi_lib TestLibrary::PATH
        class TestStruct < FFI::Struct
          layout :c, :char
        end
        attach_function :struct_field_s8, [ TestStruct.in ], :char
      end).to be_an_instance_of FFI::Function
    end

    it "Can use Struct subclass as OUT parameter type" do
      expect(module StructParam3
        extend FFI::Library
        ffi_lib TestLibrary::PATH
        class TestStruct < FFI::Struct
          layout :c, :char
        end
        attach_function :struct_field_s8, [ TestStruct.out ], :char
      end).to be_an_instance_of FFI::Function
    end

    it "can be passed directly as a :pointer parameter" do
      class TestStruct < FFI::Struct
        layout :i, :int
      end
      s = TestStruct.new
      s[:i] = 0x12
      expect(LibTest.ptr_ret_int32_t(s, 0)).to eq(0x12)
    end

    it ":char member aligned correctly" do
      class AlignChar < FFI::Struct
        layout :c, :char, :v, :char
      end
      s = AlignChar.new
      s[:v] = 0x12
      expect(LibTest.struct_align_s8(s.pointer)).to eq(0x12)
    end

    it ":short member aligned correctly" do
      class AlignShort < FFI::Struct
        layout :c, :char, :v, :short
      end
      s = AlignShort.alloc_in
      s[:v] = 0x1234
      expect(LibTest.struct_align_s16(s.pointer)).to eq(0x1234)
    end

    it ":int member aligned correctly" do
      class AlignInt < FFI::Struct
        layout :c, :char, :v, :int
      end
      s = AlignInt.alloc_in
      s[:v] = 0x12345678
      expect(LibTest.struct_align_s32(s.pointer)).to eq(0x12345678)
    end

    it ":long_long member aligned correctly" do
      class AlignLongLong < FFI::Struct
        layout :c, :char, :v, :long_long
      end
      s = AlignLongLong.alloc_in
      s[:v] = 0x123456789abcdef0
      expect(LibTest.struct_align_s64(s.pointer)).to eq(0x123456789abcdef0)
    end

    it ":long member aligned correctly" do
      class AlignLong < FFI::Struct
        layout :c, :char, :v, :long
      end
      s = AlignLong.alloc_in
      s[:v] = 0x12345678
      expect(LibTest.struct_align_long(s.pointer)).to eq(0x12345678)
    end

    it ":float member aligned correctly" do
      class AlignFloat < FFI::Struct
        layout :c, :char, :v, :float
      end
      s = AlignFloat.alloc_in
      s[:v] = 1.23456
      expect((LibTest.struct_align_f32(s.pointer) - 1.23456).abs).to be < 0.00001
    end

    it ":double member aligned correctly" do
      class AlignDouble < FFI::Struct
        layout :c, :char, :v, :double
      end
      s = AlignDouble.alloc_in
      s[:v] = 1.23456789
      expect((LibTest.struct_align_f64(s.pointer) - 1.23456789).abs).to be < 0.00000001
    end

    it ":ulong, :pointer struct" do
      class ULPStruct < FFI::Struct
        layout :ul, :ulong, :p, :pointer
      end
      s = ULPStruct.alloc_in
      s[:ul] = 0xdeadbeef
      s[:p] = LibTest.ptr_from_address(0x12345678)
      expect(s.pointer.get_ulong(0)).to eq(0xdeadbeef)
    end
    def test_num_field(type, v)
      klass = Class.new(FFI::Struct)
      klass.layout :v, type, :dummy, :long

      s = klass.new
      s[:v] = v
      expect(s.pointer.send("get_#{type.to_s}", 0)).to eq(v)
      s.pointer.send("put_#{type.to_s}", 0, 0)
      expect(s[:v]).to eq(0)

      # Test coercion
      obj = double("coerce")
      expect(obj).to receive(:to_int).and_return(v)
      s[:v] = obj
      expect(s.pointer.send("get_#{type.to_s}", 0)).to eq(v)

      zero = double("zero")
      expect(zero).to receive(:to_int).and_return(0)
      s.pointer.send("put_#{type.to_s}", 0, zero)
      expect(s[:v]).to eq(0)
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
      expect((s.pointer.get_float(0) - value).abs).to be < 0.0001

      # Test coercion
      obj = double("coerce")
      expect(obj).to receive(:to_f).and_return(42.0)
      s[:v] = obj
      expect((s.pointer.get_float(0) - 42.0).abs).to be < 0.0001

      s.pointer.put_float(0, 1.0)
      expect(s.pointer.get_float(0)).to eq(1.0)
    end

    it ":double field r/w" do
      klass = Class.new(FFI::Struct)
      klass.layout :v, :double, :dummy, :long

      s = klass.new
      value = 1.23456
      s[:v] = value
      expect((s.pointer.get_double(0) - value).abs).to be < 0.0001

      # Test coercion
      obj = double("coerce")
      expect(obj).to receive(:to_f).and_return(42.0)
      s[:v] = obj
      expect((s.pointer.get_double(0) - 42.0).abs).to be < 0.0001

      s.pointer.put_double(0, 1.0)
      expect(s.pointer.get_double(0)).to eq(1.0)
    end
    module EnumFields
      extend FFI::Library
      TestEnum = enum :test_enum, [:c1, 10, :c2, 20, :c3, 30, :c4, 40]
      class TestStruct < FFI::Struct
        layout :a, :int, :c, :test_enum,
          :d, [ TestEnum, TestEnum.symbols.length ]
      end
    end

    it ":enum field r/w" do
      s = EnumFields::TestStruct.new
      s[:c] = :c3

      expect(s.pointer.get_uint(FFI::Type::INT32.size)).to eq(30)
      expect(s[:c]).to eq(:c3)
    end

    it "array of :enum field" do
      s = EnumFields::TestStruct.new
      EnumFields::TestEnum.symbols.each_with_index do |val, i|
        s[:d][i] = val
      end

      EnumFields::TestEnum.symbols.each_with_index do |val, i|
        expect(s.pointer.get_uint(FFI::Type::INT32.size * (2 + i))).to eq(EnumFields::TestEnum[val])
      end

      s[:d].each_with_index do |val, i|
        expect(val).to eq(EnumFields::TestEnum.symbols[i])
      end
    end

    module CallbackMember
      extend FFI::Library
      ffi_lib TestLibrary::PATH
      callback :add, [ :int, :int ], :int
      callback :sub, [ :int, :int ], :int
      class TestStruct < FFI::Struct
        layout :add, :add,
          :sub, :sub
      end
      attach_function :struct_call_add_cb, [TestStruct.in, :int, :int], :int
      attach_function :struct_call_sub_cb, [TestStruct.in, :int, :int], :int
    end

    it "Can have CallbackInfo struct field" do
        s = CallbackMember::TestStruct.new
        add_proc = lambda { |a, b| a+b }
        sub_proc = lambda { |a, b| a-b }
        s[:add] = add_proc
        s[:sub] = sub_proc
        expect(CallbackMember.struct_call_add_cb(s, 40, 2)).to eq(42)
        expect(CallbackMember.struct_call_sub_cb(s, 44, 2)).to eq(42)
    end

    it "Can return its members as a list" do
      s = Class.new(FFI::Struct) do
        layout :a, :int, :b, :int, :c, :int
      end
      expect(s.members).to include(:a, :b, :c)
    end

    it "Can return its instance members and values as lists" do
      s = Class.new(FFI::Struct) do
        layout :a, :int, :b, :int, :c, :int
      end
      s = s.new
      expect(s.members).to include(:a, :b, :c)
      s[:a] = 1
      s[:b] = 2
      s[:c] = 3
      expect(s.values).to include(1, 2, 3)
    end

    it 'should return an ordered field/offset pairs array' do
      s = Class.new(FFI::Struct) do
        layout :a, :int, :b, :int, :c, :int
      end
      s = s.new
      expect(s.offsets).to eq([[:a, 0], [:b, 4], [:c, 8]])
      expect(s.offsets).to eq([[:a, 0], [:b, 4], [:c, 8]])
    end

    it "Struct#offset_of returns offset of field within struct" do
      s = Class.new(FFI::Struct) do
        layout :a, :int, :b, :int, :c, :int
      end
      expect(s.offset_of(:a)).to eq(0)
      expect(s.offset_of(:b)).to eq(4)
      expect(s.offset_of(:c)).to eq(8)
    end

    if FFI::VERSION < "2"
      it "warns about redefinition of struct layouts" do
        expect do
          Class.new(FFI::Struct) do
            layout :a, :int
            layout :a, :int
          end
        end.to output(/Redefinition .* will be disallowed in ffi-2.0/).to_stderr
      end
    else
      it "denies redefinition of struct layouts" do
        expect do
          Class.new(FFI::Struct) do
            layout :a, :int
            layout :a, :int
          end
        end.to raise_error(/struct layout already defined/)
      end
    end

    it "allows redefinition of struct layouts in derived classes" do
      a = Class.new(FFI::Struct) do
        layout :a, :char
      end
      b = Class.new(a) do
        layout :a, :char, :b, :char
      end
      expect(a.members).to eq([:a])
      expect(b.members).to eq([:a, :b])
    end
  end
end

describe FFI::Struct, ".layout" do
  module FFISpecs
    module LibTest
      extend FFI::Library
      ffi_lib TestLibrary::PATH
      begin
        attach_function :ptr_ret_int32_t, [ :pointer, :int ], :int
      rescue FFI::NotFoundError
        # NetBSD uses #define instead of typedef for these
        attach_function :ptr_ret_int32_t, :ptr_ret___int32_t, [ :pointer, :int ], :int
      end
    end
  end

  describe "when derived class is not assigned to any constant" do
    it "resolves a built-in type" do
      klass = Class.new FFI::Struct
      klass.layout :number, :int

      instance = klass.new
      instance[:number] = 0xA1
      expect(FFISpecs::LibTest.ptr_ret_int32_t(instance, 0)).to eq(0xA1)
    end
  end

  describe "when derived class is assigned to a constant" do
    it "resolves a built-in type" do
      class FFISpecs::TestStruct < FFI::Struct
        layout :number, :int
      end

      instance = FFISpecs::TestStruct.new
      instance[:number] = 0xA1
      expect(FFISpecs::LibTest.ptr_ret_int32_t(instance, 0)).to eq(0xA1)
    end

    it "resolves a type from the enclosing module" do
      module FFISpecs::LibTest
        typedef :uint, :custom_int

        class TestStruct < FFI::Struct
          layout :number, :custom_int
        end
      end

      instance = FFISpecs::LibTest::TestStruct.new
      instance[:number] = 0xA1
      expect(FFISpecs::LibTest.ptr_ret_int32_t(instance, 0)).to eq(0xA1)
    end

    it "ignores a module which does not extend FFI::Library or subclass FFI::Struct" do
      module FFISpecs::UnrelatedFindTypeTest
        # MakeMakefile from 'mkmf' defines such a method
        def self.find_type(*args)
          raise "should not be called"
        end

        class TestStruct < FFI::Struct
          layout :number, :int
        end
      end

      instance = FFISpecs::UnrelatedFindTypeTest::TestStruct.new
      instance[:number] = 123
      expect(FFISpecs::LibTest.ptr_ret_int32_t(instance, 0)).to eq(123)
    end
  end
end

describe FFI::Struct, ' with a nested struct field'  do
  module LibTest
    extend FFI::Library
    ffi_lib TestLibrary::PATH
    class NestedStruct < FFI::Struct
      layout :i, :int
    end
    class ContainerStruct < FFI::Struct
      layout :first, :char, :ns, NestedStruct
    end
    attach_function :struct_align_nested_struct, [ :pointer ], :int
    attach_function :struct_make_container_struct, [ :int ], :pointer
  end
  before do
    @cs = LibTest::ContainerStruct.new
  end

  it 'should align correctly nested struct field' do
    @cs[:ns][:i] = 123
    expect(LibTest.struct_align_nested_struct(@cs.to_ptr)).to eq(123)
  end

  it 'should correctly calculate Container size (in bytes)' do
    expect(LibTest::ContainerStruct.size).to eq(8)
  end

  it 'should return a Struct object when the field is accessed' do
    expect(@cs[:ns].is_a?(FFI::Struct)).to be true
  end

  it 'should read a value from memory' do
    @cs = LibTest::ContainerStruct.new(LibTest.struct_make_container_struct(123))
    expect(@cs[:ns][:i]).to eq(123)
  end

  it 'should write a value to memory' do
    @cs = LibTest::ContainerStruct.new(LibTest.struct_make_container_struct(123))
    @cs[:ns][:i] = 456
    expect(LibTest.struct_align_nested_struct(@cs.to_ptr)).to eq(456)
  end

  it 'should be able to assign struct instance to nested field' do
    cs = LibTest::ContainerStruct.new(LibTest.struct_make_container_struct(123))
    ns = LibTest::NestedStruct.new
    ns[:i] = 567
    cs[:ns] = ns
    expect(cs[:ns][:i]).to eq(567)
    expect(LibTest.struct_align_nested_struct(cs.to_ptr)).to eq(567)
  end
end

describe FFI::Struct, ' with a nested array of structs'  do
  module InlineArrayOfStructs
    extend FFI::Library
    ffi_lib TestLibrary::PATH
    class NestedStruct < FFI::Struct
      layout :i, :int
    end
    class ContainerStruct < FFI::Struct
      layout :first, :char, :ns, [ NestedStruct, 1 ]
    end
    attach_function :struct_align_nested_struct, [ :pointer ], :int
    attach_function :struct_make_container_struct, [ :int ], :pointer
  end

  before do
    @cs = InlineArrayOfStructs::ContainerStruct.new
  end

  it 'should align correctly nested struct field' do
    @cs[:ns][0][:i] = 123
    expect(InlineArrayOfStructs.struct_align_nested_struct(@cs.to_ptr)).to eq(123)
  end

  it 'should correctly calculate Container size (in bytes)' do
    expect(InlineArrayOfStructs::ContainerStruct.size).to eq(8)
  end

  it 'should return a Struct object when the field is accessed' do
    expect(@cs[:ns][0].is_a?(FFI::Struct)).to be true
  end

  it 'should read a value from memory' do
    @cs = InlineArrayOfStructs::ContainerStruct.new(InlineArrayOfStructs.struct_make_container_struct(123))
    expect(@cs[:ns][0][:i]).to eq(123)
  end

  it 'should write a value to memory' do
    @cs = InlineArrayOfStructs::ContainerStruct.new(InlineArrayOfStructs.struct_make_container_struct(123))
    @cs[:ns][0][:i] = 456
    expect(InlineArrayOfStructs.struct_align_nested_struct(@cs.to_ptr)).to eq(456)
  end

  it 'should support Enumerable#each' do
    @cs = InlineArrayOfStructs::ContainerStruct.new(InlineArrayOfStructs.struct_make_container_struct(123))
    ints = []
    @cs[:ns].each { |s| ints << s[:i] }
    expect(ints[0]).to eq(123)
  end
end

# struct by value not yet supported on TruffleRuby
describe FFI::Struct, ' by value' do
  module LibTest
    extend FFI::Library
    ffi_lib TestLibrary::PATH

    class S8S32 < FFI::Struct
      layout :s8, :char, :s32, :int
    end

    class StructString < FFI::Struct
      layout :bytes, :string, :len, :int
    end

    attach_function :struct_return_s8s32, [ ], S8S32.by_value
    attach_function :struct_s8s32_set, [ :char, :int ], S8S32.by_value
    attach_function :struct_s8s32_get_s8, [ S8S32.by_value ], :char
    attach_function :struct_s8s32_get_s32, [ S8S32.by_value ], :int
    attach_function :struct_s8s32_s32_ret_s32, [ S8S32.by_value, :int ], :int
    attach_function :struct_s8s32_s64_ret_s64, [ S8S32.by_value, :long_long ], :long_long
    attach_function :struct_s8s32_ret_s8s32, [ S8S32.by_value ], S8S32.by_value
    attach_function :struct_s32_ptr_s32_s8s32_ret_s32, [ :int, :pointer, :int, S8S32.by_value ], :int
    attach_function :struct_varargs_ret_struct_string, [ :int, :varargs ], StructString.by_value
  end

  it 'return using pre-set values' do
    s = LibTest.struct_return_s8s32
    expect(s[:s8]).to eq(0x7f)
    expect(s[:s32]).to eq(0x12345678)
  end

  it 'return using passed in values' do
    s = LibTest.struct_s8s32_set(123, 456789)
    expect(s[:s8]).to eq(123)
    expect(s[:s32]).to eq(456789)
  end

  it 'parameter' do
    s = LibTest::S8S32.new
    s[:s8] = 0x12
    s[:s32] = 0x34567890
    expect(LibTest.struct_s8s32_get_s8(s)).to eq(0x12)
    expect(LibTest.struct_s8s32_get_s32(s)).to eq(0x34567890)
  end

  it 'parameter with following s32' do
    s = LibTest::S8S32.new
    s[:s8] = 0x12
    s[:s32] = 0x34567890

    expect(LibTest.struct_s8s32_s32_ret_s32(s, 0x1eefdead)).to eq(0x1eefdead)
  end

  # it 'parameter with following s64' do
  #   s = LibTest::S8S64.new
  #   s[:s8] = 0x12
  #   s[:s64] = 0x34567890
  #
  #
  #   LibTest.struct_s8s64_s64_ret_s64(s, 0x1eefdead1eefdead).should == 0x1eefdead1eefdead
  # end

  it 'parameter with preceding s32,ptr,s32' do
    s = LibTest::S8S32.new
    s[:s8] = 0x12
    s[:s32] = 0x34567890
    out = LibTest::S8S32.new
    expect(LibTest.struct_s32_ptr_s32_s8s32_ret_s32(0x1000000, out, 0x1eafbeef, s)).to eq(0x34567890)
    expect(out[:s8]).to eq(s[:s8])
    expect(out[:s32]).to eq(s[:s32])
  end

  it 'parameter with preceding s32,string,s32' do
    s = LibTest::S8S32.new
    s[:s8] = 0x12
    s[:s32] = 0x34567890
    out = 0.chr * 32
    expect(LibTest.struct_s32_ptr_s32_s8s32_ret_s32(0x1000000, out, 0x1eafbeef, s)).to eq(0x34567890)
  end

  it 'parameter, returning struct by value' do
    s = LibTest::S8S32.new
    s[:s8] = 0x12
    s[:s32] = 0x34567890

    ret = LibTest.struct_s8s32_ret_s8s32(s)
    expect(ret[:s8]).to eq(s[:s8])
    expect(ret[:s32]).to eq(s[:s32])
  end

  it 'varargs returning a struct' do
    string = "test"
    s = LibTest.struct_varargs_ret_struct_string(4, :string, string)
    expect(s[:len]).to eq(string.length)
    expect(s[:bytes]).to eq(string)
  end
end if RUBY_ENGINE != "truffleruby"

describe FFI::Struct, ' with an array field'  do
  module LibTest
    extend FFI::Library
    ffi_lib TestLibrary::PATH
    class StructWithArray < FFI::Struct
      layout :first, :char, :a, [:int, 5]
    end
    attach_function :struct_make_struct_with_array, [:int, :int, :int, :int, :int], :pointer
    attach_function :struct_field_array, [:pointer], :pointer
  end
  before do
    @s = LibTest::StructWithArray.new
  end

  it 'should correctly calculate StructWithArray size (in bytes)' do
    expect(LibTest::StructWithArray.size).to eq(24)
  end

  it 'should read values from memory' do
    @s = LibTest::StructWithArray.new(LibTest.struct_make_struct_with_array(0, 1, 2, 3, 4))
    expect(@s[:a].to_a).to eq([0, 1, 2, 3, 4])
  end
#  it 'should cache array object for successive calls' do
#    @s[:a].object_id.should == @s[:a].object_id
#  end

  it 'should return the number of elements in the array field' do
    @s = LibTest::StructWithArray.new(LibTest.struct_make_struct_with_array(0, 1, 2, 3, 4))
    expect(@s[:a].size).to eq(5)
  end

  it 'should allow iteration through the array elements' do
    @s = LibTest::StructWithArray.new(LibTest.struct_make_struct_with_array(0, 1, 2, 3, 4))
    @s[:a].each_with_index { |elem, i| expect(elem).to eq(i) }
  end

  it 'should return the pointer to the array' do
    @s = LibTest::StructWithArray.new(LibTest.struct_make_struct_with_array(0, 1, 2, 3, 4))
    expect(@s[:a].to_ptr).to eq(LibTest::struct_field_array(@s.to_ptr))
  end

  it 'raises when trying to set an array field' do
    @s = LibTest::StructWithArray.new(LibTest.struct_make_struct_with_array(0, 1, 2, 3, 4))
    expect { @s[:a] = [7, 8, 9, 10, 11] }.to raise_error(NotImplementedError, 'cannot set array field')
  end
end

describe FFI::Struct, ' with a char array field'  do
  module LibTest
    class StructWithCharArray < FFI::Struct
      layout :before, :int32, :str, [:char, 4], :after, :int32
    end
  end

  before do
    @s = LibTest::StructWithCharArray.new
    @s.pointer.write_bytes([-1, 1, 2, 0, 255, -2].pack("lC4l"))
  end

  it 'should read values from memory' do
    expect(@s[:before]).to eq(-1)
    expect(@s[:str].to_a).to eq([1, 2, 0, -1])
    expect(@s[:str].to_s).to eq("\x01\x02".b)
    expect(@s[:after]).to eq(-2)
  end

  it 'should return the number of elements in the array field' do
    expect(@s[:str].size).to eq(4)
  end

  it 'should allow iteration through the array elements' do
    @s[:str].each_with_index { |elem, i| expect(elem).to eq([1, 2, 0, -1][i]) }
  end

  it 'should return the pointer to the array' do
    expect(@s[:str].to_ptr).to eq(@s.to_ptr + 4)
  end

  it 'allows writing a shorter String to the char array' do
    @s[:str] = "äc"
    expect(@s[:before]).to eq(-1)
    expect(@s[:str].to_s).to eq("äc".b)
    expect(@s[:after]).to eq(-2)
  end

  it 'allows writing a String of the same size to the char array' do
    @s[:str] = "äcd"
    expect(@s[:before]).to eq(-1)
    expect(@s[:str].to_s).to eq("äcd".b)
    expect(@s[:after]).to eq(-2) # The above should not write to the next field
  end

  it 'raises when writing a longer String to the char array' do
    expect { @s[:str] = "äcde" }.to raise_error(IndexError)
  end
end

describe 'BuggedStruct' do
  module LibTest
    extend FFI::Library
    ffi_lib TestLibrary::PATH
    class BuggedStruct < FFI::Struct
      layout :visible, :uchar,
              :x, :uint,
              :y, :uint,
              :rx, :short,
              :ry, :short,
              :order, :uchar,
              :size, :uchar
    end
    attach_function :bugged_struct_size, [], :uint
  end

  it 'should return its correct size' do
    expect(LibTest::BuggedStruct.size).to eq(LibTest.bugged_struct_size)
  end

  it "offsets within struct should be correct" do
    expect(LibTest::BuggedStruct.offset_of(:visible)).to eq(0)
    expect(LibTest::BuggedStruct.offset_of(:x)).to eq(4)
    expect(LibTest::BuggedStruct.offset_of(:y)).to eq(8)
    expect(LibTest::BuggedStruct.offset_of(:rx)).to eq(12)
    expect(LibTest::BuggedStruct.offset_of(:ry)).to eq(14)
    expect(LibTest::BuggedStruct.offset_of(:order)).to eq(16)
    expect(LibTest::BuggedStruct.offset_of(:size)).to eq(17)
  end

  it 'should return correct field/offset pairs' do
    expect(LibTest::BuggedStruct.offsets.sort do |a, b|
      a[1] <=> b[1]
    end).to eq([[:visible, 0], [:x, 4], [:y, 8], [:rx, 12], [:ry, 14], [:order, 16], [:size, 17]])
  end
end

describe "Struct allocation" do
  it "MemoryPointer.new(Struct, 2)" do
    s = Class.new(FFI::Struct) do
      layout :i, :uint
    end
    p = FFI::MemoryPointer.new(s, 2)
    expect(p.total).to eq(8)
    expect(p.type_size).to eq(4)
    p.put_uint(4, 0xdeadbeef)
    expect(s.new(p[1])[:i]).to eq(0xdeadbeef)
    expect(p[1].address).to eq((p[0].address + 4))
  end

  it "Buffer.new(Struct, 2)" do
    s = Class.new(FFI::Struct) do
      layout :i, :uint
    end
    p = FFI::Buffer.new(s, 2)
    expect(p.total).to eq(8)
    expect(p.type_size).to eq(4)
    p.put_uint(4, 0xdeadbeef)
    expect(s.new(p[1])[:i]).to eq(0xdeadbeef)
  end

  it "null? should be true when initialized with NULL pointer" do
    s = Class.new(FFI::Struct) do
      layout :i, :uint
    end
    expect(s.new(FFI::Pointer::NULL)).to be_null
  end

  it "null? should be false when initialized with non-NULL pointer" do
    s = Class.new(FFI::Struct) do
      layout :i, :uint
    end
    expect(s.new(FFI::MemoryPointer.new(s))).not_to be_null
  end

  it "supports :bool as a struct member" do
    expect do
      c = Class.new(FFI::Struct) do
        layout :b, :bool
      end
      struct = c.new
      struct[:b] = ! struct[:b]
    end.not_to raise_error Exception
  end

end

describe "variable-length arrays" do
  it "zero length array should be accepted as last field" do
    expect {
      Class.new(FFI::Struct) do
        layout :count, :int, :data, [ :char, 0 ]
      end
    }.not_to raise_error Exception
  end

  it "zero length array before last element should raise error" do
    expect {
      Class.new(FFI::Struct) do
        layout :data, [ :char, 0 ], :count, :int
      end
    }.to raise_error(TypeError)
  end

  it "can access elements of array" do
    struct_class = Class.new(FFI::Struct) do
      layout :count, :int, :data, [ :long, 0 ]
    end
    s = struct_class.new(FFI::MemoryPointer.new(1024))
    s[:data][0] = 0x1eadbeef
    s[:data][1] = 0x12345678
    expect(s[:data][0]).to eq(0x1eadbeef)
    expect(s[:data][1]).to eq(0x12345678)
  end

  it "non-variable length array is bounds checked" do
    struct_class = Class.new(FFI::Struct) do
      layout :count, :int, :data, [ :long, 1 ]
    end
    s = struct_class.new(FFI::MemoryPointer.new(1024))
    s[:data][0] = 0x1eadbeef
    expect { s[:data][1] = 0x12345678 }.to raise_error(IndexError)
    expect(s[:data][0]).to eq(0x1eadbeef)
    expect { expect(s[:data][1]).to == 0x12345678 }.to raise_error(IndexError)
  end
end

describe "Struct order" do
  before :all do
    @struct = Class.new(FFI::Struct) do
      layout :value, :int32
    end
  end

  before :each do
    @pointer = @struct.new
    @pointer.pointer.write_bytes("\x1\x2\x3\x4")
    @pointer
  end

  it "should return the system order by default" do
    expect(@pointer.order).to eq(OrderHelper::ORDER)
  end

  it "should return a new struct if there is no change" do
    expect(@pointer.order(OrderHelper::ORDER)).to_not be @pointer
  end

  it "should return a new struct if there is a change" do
    expect(@pointer.order(OrderHelper::OTHER_ORDER)).to_not be @pointer
  end

  it "can be set to :little" do
    expect(@pointer.order(:little).order).to eq(:little)
  end

  it "can be set to :big" do
    expect(@pointer.order(:big).order).to eq(:big)
  end

  it "can be set to :network, which sets it to :big" do
    expect(@pointer.order(:network).order).to eq(:big)
  end

  it "cannot be set to other symbols" do
    expect { @pointer.order(:unknown) }.to raise_error(ArgumentError)
  end

  it "can be used to read in little order" do
    expect(@pointer.order(:little)[:value]).to eq(67305985)
  end

  it "can be used to read in big order" do
    expect(@pointer.order(:big)[:value]).to eq(16909060)
  end

  it "can be used to read in network order" do
    expect(@pointer.order(:network)[:value]).to eq(16909060)
  end
end if RUBY_ENGINE != "truffleruby"
end
