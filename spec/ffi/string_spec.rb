#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))
describe "String tests" do
  include FFI
  module StrLibTest
    extend FFI::Library
    ffi_lib TestLibrary::PATH
    attach_function :ptr_ret_pointer, [ :pointer, :int], :string
    attach_function :pointer_buffer_equals, :buffer_equals, [ :pointer, :string, :size_t ], :int
    attach_function :string_dummy, [ :string ], :void
    attach_function :string_null, [ ], :string
  end

  it "A String can be passed to a :pointer argument" do
    str = "string buffer"
    expect(StrLibTest.pointer_buffer_equals(str, str, str.bytesize)).to eq(1)
    expect(StrLibTest.pointer_buffer_equals(str + "a", str + "a", str.bytesize + 1)).to eq(1)
    expect(StrLibTest.pointer_buffer_equals(str + "\0", str, str.bytesize + 1)).to eq(1)
    expect(StrLibTest.pointer_buffer_equals(str + "a", str + "b", str.bytesize + 1)).to eq(0)
  end

  it "Poison null byte raises error" do
    s = "123\0abc"
    expect{ StrLibTest.pointer_buffer_equals("", s, 0) }.to raise_error(ArgumentError)
    expect( StrLibTest.pointer_buffer_equals(s, "", 0) ).to eq(1)
  end

  it "casts nil as NULL pointer" do
    expect(StrLibTest.string_dummy(nil)).to be_nil
  end

  it "return nil for NULL char*" do
    expect(StrLibTest.string_null).to be_nil
  end

  it "reads an array of strings until encountering a NULL pointer" do
    strings = ["foo", "bar", "baz", "testing", "ffi"]
    ptrary = FFI::MemoryPointer.new(:pointer, 6)
    ary = strings.inject([]) do |a, str|
      f = FFI::MemoryPointer.new(1024)
      f.put_string(0, str)
      a << f
    end
    ary.insert(3, nil)
    ptrary.write_array_of_pointer(ary)
    expect(ptrary.get_array_of_string(0)).to eq(["foo", "bar", "baz"])
  end

  it "reads an array of strings of the size specified, substituting nil when a pointer is NULL" do
    strings = ["foo", "bar", "baz", "testing", "ffi"]
    ptrary = FFI::MemoryPointer.new(:pointer, 6)
    ary = strings.inject([]) do |a, str|
      f = FFI::MemoryPointer.new(1024)
      f.put_string(0, str)
      a << f
    end
    ary.insert(2, nil)
    ptrary.write_array_of_pointer(ary)
    expect(ptrary.get_array_of_string(0, 4)).to eq(["foo", "bar", nil, "baz"])
  end

  it "reads an array of strings, taking a memory offset parameter" do
    strings = ["foo", "bar", "baz", "testing", "ffi"]
    ptrary = FFI::MemoryPointer.new(:pointer, 5)
    ary = strings.inject([]) do |a, str|
      f = FFI::MemoryPointer.new(1024)
      f.put_string(0, str)
      a << f
    end
    ptrary.write_array_of_pointer(ary)
    expect(ptrary.get_array_of_string(2 * FFI.type_size(:pointer), 3)).to eq(["baz", "testing", "ffi"])
  end

  it "raises an IndexError when trying to read an array of strings out of bounds" do
    strings = ["foo", "bar", "baz", "testing", "ffi"]
    ptrary = FFI::MemoryPointer.new(:pointer, 5)
    ary = strings.inject([]) do |a, str|
      f = FFI::MemoryPointer.new(1024)
      f.put_string(0, str)
      a << f
    end
    ptrary.write_array_of_pointer(ary)
    expect { ptrary.get_array_of_string(0, 6) }.to raise_error(IndexError)
  end

  it "raises an IndexError when trying to read an array of strings using a negative offset" do
    strings = ["foo", "bar", "baz", "testing", "ffi"]
    ptrary = FFI::MemoryPointer.new(:pointer, 5)
    ary = strings.inject([]) do |a, str|
      f = FFI::MemoryPointer.new(1024)
      f.put_string(0, str)
      a << f
    end
    ptrary.write_array_of_pointer(ary)
    expect { ptrary.get_array_of_string(-1) }.to raise_error(IndexError)
  end

  describe "#write_string" do
    # https://github.com/ffi/ffi/issues/805
    describe "with no length given" do
      it "writes no final \\0" do
        ptr = FFI::MemoryPointer.new(8)
        ptr.write_int64(-1)
        ptr.write_string("äbc")
        expect(ptr.read_bytes(5)).to eq("äbc\xff".b)
      end

      it "doesn't write anything when size is exceeded" do
        ptr = FFI::MemoryPointer.new(8)
        ptr.write_int64(-1)
        expect do
          ptr.write_string("äbcdefgh")
        end.to raise_error(IndexError, /out of bounds/i)
        expect(ptr.read_int64).to eq(-1)
      end

      it "fits into memory" do
        ptr = FFI::MemoryPointer.new(5)
        ptr.write_string("äbcd")
        expect(ptr.read_string).to eq("äbcd".b)
      end
    end

    describe "with a length" do
      it "writes a final \\0" do
        ptr = FFI::MemoryPointer.new(8)
        ptr.write_int64(-1)
        ptr.write_string("äbcd", 3)
        expect(ptr.read_bytes(5)).to eq("äb\xFF\xFF".b)
      end

      it "doesn't write anything when size is exceeded" do
        ptr = FFI::MemoryPointer.new(8)
        ptr.write_int64(-1)
        expect do
          ptr.write_string("äbcdefghi", 9)
        end.to raise_error(IndexError, /out of bounds/i)
        expect(ptr.read_int64).to eq(-1)
      end

      it "fits into memory" do
        ptr = FFI::MemoryPointer.new(5)
        ptr.write_string("äbcde", 5)
        expect(ptr.read_string).to eq("äbcd".b)
      end
    end
  end

  describe "#put_string" do
    it "writes a final \\0" do
      ptr = FFI::MemoryPointer.new(8)
      ptr.write_int64(-1)
      ptr.put_string(0, "äbc")
      expect(ptr.read_bytes(5)).to eq("äbc\x00".b)
      expect(ptr.read_string).to eq("äbc".b)
    end
  end
end
