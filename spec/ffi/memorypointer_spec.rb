require 'ffi'
require 'java'

describe "MemoryPointer#total" do
  it "MemoryPointer.new(:char, 1).total == 1" do
    MemoryPointer.new(:char, 1).total.should == 1
  end
  it "MemoryPointer.new(:short, 1).total == 2" do
    MemoryPointer.new(:short, 1).total.should == 2
  end
  it "MemoryPointer.new(:int, 1).total == 4" do
    MemoryPointer.new(:int, 1).total.should == 4
  end
  it "MemoryPointer.new(:long_long, 1).total == 8" do
    MemoryPointer.new(:long_long, 1).total.should == 8
  end
  it "MemoryPointer.new(1024).total == 1024" do
    MemoryPointer.new(1024).total.should == 1024
  end
end
describe "MemoryPointer#read_array_of_long" do
  it "foo" do
    ptr = MemoryPointer.new(:long, 1024)
    ptr[0].write_long 1234
    ptr[1].write_long 5678
    l = ptr.read_array_of_long(2)
    l[0].should == 1234
    l[1].should == 5678
  end
end
describe "MemoryPointer argument" do
  module Ptr
    extend FFI::Library
    ffi_lib FFI::Platform::LIBC
    attach_function :memset, [ :pointer, :int, :ulong ], :pointer
    attach_function :memcpy, [ :pointer, :pointer, :ulong ], :pointer
  end
  it "Pointer passed correctly" do
    p = MemoryPointer.new :int, 1
    ret = Ptr.memset(p, 0, p.total)
    ret.should == p
  end
  it "Data passed to native function" do
    p = MemoryPointer.new :int, 1
    p2 = MemoryPointer.new :int, 1
    p2.put_int(0, 0xdeadbeef)
    Ptr.memcpy(p, p2, p.total)
    p.get_int(0).should == p2.get_int(0)
  end
end
describe "MemoryPointer return value" do
  module Stdio
    extend FFI::Library
    ffi_lib FFI::Platform::LIBC
    attach_function :fopen, [ :string, :string ], :pointer
    attach_function :fclose, [ :pointer ], :int
    attach_function :fwrite, [ :pointer, :ulong, :ulong, :string ], :ulong
  end
  it "fopen returns non-nil" do
    fp = Stdio.fopen("/dev/null", "w")
    fp.should_not be_nil
    Stdio.fclose(fp).should == 0 unless fp.nil? or fp.null? 
  end
end
