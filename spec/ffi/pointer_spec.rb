# -*- encoding: utf-8 -*-
#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))
require 'delegate'

module PointerTestLib
  extend FFI::Library
  ffi_lib TestLibrary::PATH
  begin
    attach_function :ptr_ret_int32_t, [ :pointer, :int ], :int
  rescue FFI::NotFoundError
    # NetBSD uses #define instead of typedef for these
    attach_function :ptr_ret_int32_t, :ptr_ret___int32_t, [ :pointer, :int ], :int
  end
  attach_function :ptr_ret_int64_t, [ :pointer, :int ], :int64_t
  attach_function :ptr_from_address, [ FFI::Platform::ADDRESS_SIZE == 32 ? :uint : :ulong_long ], :pointer
  attach_function :ptr_set_pointer, [ :pointer, :int, :pointer ], :void
  attach_function :ptr_ret_pointer, [ :pointer, :int ], :pointer
end
describe "Pointer" do
  include FFI
  class ToPtrTest
    def initialize(ptr)
      @ptr = ptr
    end
    def to_ptr
      @ptr
    end
  end

  it "Any object implementing #to_ptr can be passed as a :pointer parameter" do
    memory = FFI::MemoryPointer.new :long_long
    magic = 0x12345678
    memory.put_int32(0, magic)
    tp = ToPtrTest.new(memory)
    expect(PointerTestLib.ptr_ret_int32_t(tp, 0)).to eq(magic)
  end
  class PointerDelegate < DelegateClass(FFI::Pointer)
    def initialize(ptr)
      @ptr = ptr
    end
    def to_ptr
      @ptr
    end
  end

  it "A DelegateClass(Pointer) can be passed as a :pointer parameter" do
    memory = FFI::MemoryPointer.new :long_long
    magic = 0x12345678
    memory.put_int32(0, magic)
    ptr = PointerDelegate.new(memory)
    expect(PointerTestLib.ptr_ret_int32_t(ptr, 0)).to eq(magic)
  end

  it "Fixnum cannot be used as a Pointer argument" do
    expect { PointerTestLib.ptr_ret_int32_t(0, 0) }.to raise_error(ArgumentError)
  end

  it "Bignum cannot be used as a Pointer argument" do
    expect { PointerTestLib.ptr_ret_int32_t(0xfee1deadbeefcafebabe, 0) }.to raise_error(ArgumentError)
  end

  it "String can be used as a Pointer argument" do
    s = "123\0abö"
    expect( PointerTestLib.ptr_ret_int64_t(s, 0) ).to eq(s.unpack("q")[0])
  end

  it "#to_ptr" do
    memory = FFI::MemoryPointer.new :pointer
    expect(memory.to_ptr).to eq(memory)

    expect(FFI::Pointer::NULL.to_ptr).to eq(FFI::Pointer::NULL)
  end

  describe "pointer type methods" do

    it "#read_pointer" do
      memory = FFI::MemoryPointer.new :pointer
      PointerTestLib.ptr_set_pointer(memory, 0, PointerTestLib.ptr_from_address(0xdeadbeef))
      expect(memory.read_pointer.address).to eq(0xdeadbeef)
    end

    it "#write_pointer" do
      memory = FFI::MemoryPointer.new :pointer
      memory.write_pointer(PointerTestLib.ptr_from_address(0xdeadbeef))
      expect(PointerTestLib.ptr_ret_pointer(memory, 0).address).to eq(0xdeadbeef)
    end

    it "#read_array_of_pointer" do
      values = [0x12345678, 0xfeedf00d, 0xdeadbeef]
      memory = FFI::MemoryPointer.new :pointer, values.size
      values.each_with_index do |address, j|
        PointerTestLib.ptr_set_pointer(memory, j * FFI.type_size(:pointer), PointerTestLib.ptr_from_address(address))
      end
      array = memory.read_array_of_pointer(values.size)
      values.each_with_index do |address, j|
        expect(array[j].address).to eq(address)
      end
    end

    it "#write_array_of_type for uint8" do
      values = [10, 227, 32]
      memory = FFI::MemoryPointer.new FFI::TYPE_UINT8, values.size
      memory.write_array_of_type(FFI::TYPE_UINT8, :put_uint8, values)
      array = memory.read_array_of_type(FFI::TYPE_UINT8, :read_uint8, values.size)
      values.each_with_index do |val, j|
        expect(array[j]).to eq(val)
      end
    end

    it "#write_array_of_type for uint32" do
      values = [10, 227, 32]
      memory = FFI::MemoryPointer.new FFI::TYPE_UINT32, values.size
      memory.write_array_of_type(FFI::TYPE_UINT32, :put_uint32, values)
      array = memory.read_array_of_type(FFI::TYPE_UINT32, :read_uint32, values.size)
      values.each_with_index do |val, j|
        expect(array[j]).to eq(val)
      end
    end
    it "#write_array_of_type should raise an error with non-array argument" do
      memory = FFI::MemoryPointer.new FFI::TYPE_INT8, 1
      expect { memory.write_array_of_int8(0) }.to raise_error(TypeError)
    end
  end

  describe 'NULL' do
    it 'should be obtained using Pointer::NULL constant' do
      null_ptr = FFI::Pointer::NULL
      expect(null_ptr).to be_null
    end
    it 'should be obtained passing address 0 to constructor' do
      expect(FFI::Pointer.new(0)).to be_null
    end
    it 'should raise an error when attempting read/write operations on it' do
      null_ptr = FFI::Pointer::NULL
      expect { null_ptr.read_int }.to raise_error(FFI::NullPointerError)
      expect { null_ptr.write_int(0xff1) }.to raise_error(FFI::NullPointerError)
    end
    it 'returns true when compared with nil' do
      expect((FFI::Pointer::NULL == nil)).to be true
    end
    it 'should not raise an error when attempting read/write zero length array' do
      null_ptr = FFI::Pointer::NULL
      expect( null_ptr.read_array_of_uint(0) ).to eq([])
      null_ptr.write_array_of_uint([])
    end
  end

  it "Pointer.size returns sizeof pointer on platform" do
    expect(FFI::Pointer.size).to eq((FFI::Platform::ADDRESS_SIZE / 8))
  end

  describe "#slice" do
    before(:each) do
      @mptr = FFI::MemoryPointer.new(:char, 12)
      @mptr.put_uint(0, 0x12345678)
      @mptr.put_uint(4, 0xdeadbeef)
    end

    it "contents of sliced pointer matches original pointer at offset" do
      expect(@mptr.slice(4, 4).get_uint(0)).to eq(0xdeadbeef)
    end

    it "modifying sliced pointer is reflected in original pointer" do
      @mptr.slice(4, 4).put_uint(0, 0xfee1dead)
      expect(@mptr.get_uint(4)).to eq(0xfee1dead)
    end

    it "access beyond bounds should raise IndexError" do
      expect { @mptr.slice(4, 4).get_int(4) }.to raise_error(IndexError)
    end
  end

  describe "#type_size" do
    it "should be same as FFI.type_size(type)" do
      expect(FFI::MemoryPointer.new(:int, 1).type_size).to eq(FFI.type_size(:int))
    end
  end

  describe "#order" do
    it "should return the system order by default" do
      expect(FFI::Pointer.new(0x0).order).to eq(OrderHelper::ORDER)
    end

    it "should return self if there is no change" do
      pointer = FFI::Pointer.new(0x0)
      expect(pointer.order(OrderHelper::ORDER)).to be pointer
    end

    it "should return a new pointer if there is a change" do
      pointer = FFI::Pointer.new(0x0)
      expect(pointer.order(OrderHelper::OTHER_ORDER)).to_not be pointer
    end

    it "can be set to :little" do
      expect(FFI::Pointer.new(0x0).order(:little).order).to eq(:little)
    end

    it "can be set to :big" do
      expect(FFI::Pointer.new(0x0).order(:big).order).to eq(:big)
    end

    it "can be set to :network, which sets it to :big" do
      expect(FFI::Pointer.new(0x0).order(:network).order).to eq(:big)
    end

    it "cannot be set to other symbols" do
      expect { FFI::Pointer.new(0x0).order(:unknown) }.to raise_error(ArgumentError)
    end

    it "can be used to read in little order" do
      pointer = FFI::MemoryPointer.from_string("\x1\x2\x3\x4").order(:little)
      expect(pointer.read_int32).to eq(67305985)
    end

    it "can be used to read in big order" do
      pointer = FFI::MemoryPointer.from_string("\x1\x2\x3\x4").order(:big)
      expect(pointer.read_int32).to eq(16909060)
    end

    it "can be used to read in network order" do
      pointer = FFI::MemoryPointer.from_string("\x1\x2\x3\x4").order(:network)
      expect(pointer.read_int32).to eq(16909060)
    end
  end if RUBY_ENGINE != "truffleruby"

  describe "#size_limit?" do
    it "should not have size limit" do
      expect(FFI::Pointer.new(0).size_limit?).to be false
    end

    it "should have size limit" do
      expect(FFI::Pointer.new(0).slice(0, 10).size_limit?).to be true
    end
  end
end

describe "AutoPointer" do
  loop_count = 30
  wiggle_room = 5 # GC rarely cleans up all objects. we can get most of them, and that's enough to determine if the basic functionality is working.
  magic = 0x12345678

  class AutoPointerTestHelper
    @@count = 0
    def self.release
      @@count += 1 if @@count > 0
    end
    def self.reset
      @@count = 0
    end
    def self.gc_everything(count)
      loop = 5
      while @@count < count && loop > 0
        loop -= 1
        TestLibrary.force_gc
        sleep 0.05 unless @@count == count
      end
      @@count = 0
    end
    def self.finalizer
      self.method(:release).to_proc
    end
  end
  class AutoPointerSubclass < FFI::AutoPointer
    def self.release(ptr); end
  end

  # see #427
  it "cleanup via default release method", :broken => true do
    expect(AutoPointerSubclass).to receive(:release).at_least(loop_count-wiggle_room).times
    AutoPointerTestHelper.reset
    loop_count.times do
      # note that if we called
      # AutoPointerTestHelper.method(:release).to_proc inline, we'd
      # have a reference to the pointer and it would never get GC'd.
      AutoPointerSubclass.new(PointerTestLib.ptr_from_address(magic))
    end
    AutoPointerTestHelper.gc_everything loop_count
  end

  # see #427
  it "cleanup when passed a proc", :broken => true do
    #  NOTE: passing a proc is touchy, because it's so easy to create a memory leak.
    #
    #  specifically, if we made an inline call to
    #
    #      AutoPointerTestHelper.method(:release).to_proc
    #
    #  we'd have a reference to the pointer and it would
    #  never get GC'd.
    expect(AutoPointerTestHelper).to receive(:release).at_least(loop_count-wiggle_room).times
    AutoPointerTestHelper.reset
    loop_count.times do
      FFI::AutoPointer.new(PointerTestLib.ptr_from_address(magic),
                           AutoPointerTestHelper.finalizer)
    end
    AutoPointerTestHelper.gc_everything loop_count
  end

  # see #427
  it "cleanup when passed a method", :broken => true do
    expect(AutoPointerTestHelper).to receive(:release).at_least(loop_count-wiggle_room).times
    AutoPointerTestHelper.reset
    loop_count.times do
      FFI::AutoPointer.new(PointerTestLib.ptr_from_address(magic),
                           AutoPointerTestHelper.method(:release))
    end
    AutoPointerTestHelper.gc_everything loop_count
  end

  it "can be used as the return type of a function" do
    expect do
      Module.new do
        extend FFI::Library
        ffi_lib TestLibrary::PATH
        class CustomAutoPointer < FFI::AutoPointer
          def self.release(ptr); end
        end
        attach_function :ptr_from_address, [ FFI::Platform::ADDRESS_SIZE == 32 ? :uint : :ulong_long ], CustomAutoPointer
      end
    end.not_to raise_error
  end

  describe "#new" do
    it "MemoryPointer argument raises TypeError" do
      expect { FFI::AutoPointer.new(FFI::MemoryPointer.new(:int))}.to raise_error(::TypeError)
    end
    it "AutoPointer argument raises TypeError" do
      expect { AutoPointerSubclass.new(AutoPointerSubclass.new(PointerTestLib.ptr_from_address(0))) }.to raise_error(::TypeError)
    end
    it "Buffer argument raises TypeError" do
      expect { FFI::AutoPointer.new(FFI::Buffer.new(:int))}.to raise_error(::TypeError)
    end

  end

  describe "#autorelease?" do
    ptr_class = Class.new(FFI::AutoPointer) do
      def self.release(ptr); end
    end

    it "should be true by default" do
      expect(ptr_class.new(FFI::Pointer.new(0xdeadbeef)).autorelease?).to be true
    end

    it "should return false when autorelease=(false)" do
      ptr = ptr_class.new(FFI::Pointer.new(0xdeadbeef))
      ptr.autorelease = false
      expect(ptr.autorelease?).to be false
    end
  end

  describe "#type_size" do
    ptr_class = Class.new(FFI::AutoPointer) do
      def self.release(ptr); end
    end

    it "type_size of AutoPointer should match wrapped Pointer" do
      aptr = ptr_class.new(FFI::Pointer.new(:int, 0xdeadbeef))
      expect(aptr.type_size).to eq(FFI.type_size(:int))
    end

    it "[] offset should match wrapped Pointer" do
      mptr = FFI::MemoryPointer.new(:int, 1024)
      aptr = ptr_class.new(FFI::Pointer.new(:int, mptr))
      aptr[0].write_uint(0xfee1dead)
      aptr[1].write_uint(0xcafebabe)
      expect(mptr[0].read_uint).to eq(0xfee1dead)
      expect(mptr[1].read_uint).to eq(0xcafebabe)
    end
  end
end

