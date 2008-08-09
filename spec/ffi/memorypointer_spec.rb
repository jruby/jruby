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
describe "MemoryPointer#autorelease" do
  System = Java::java.lang.System
  def wait_gc(ref, timeout = 5000)
    System.gc
    (timeout / 100).times do
      java.lang.Thread.sleep(100)
      return false if ref.get.nil?
    end
    true
  end
  it "does not free memory when autorelease=false" do
    ptr = MemoryPointer.new(1)
    ptr.autorelease = false
    ref = Java::java.lang.ref.WeakReference.new(ptr)
    ptr = nil
    System.gc
    java.lang.Thread.sleep(1000)
    ref.get.should_not be_nil
  end
  it "freed when autorelease=true" do
    ptr = MemoryPointer.new(1)
    ptr.autorelease = true
    ref = Java::java.lang.ref.WeakReference.new(ptr)
    ptr = nil
    wait_gc(ref)
    ref.get.should be_nil
  end
  it "freed via explicit free() call when autorelease=false" do
    ptr = MemoryPointer.new(1)
    ptr.autorelease = false
    ref = Java::java.lang.ref.WeakReference.new(ptr)
    ptr.free
    ptr = nil
    wait_gc(ref)
    ref.get.should be_nil
  end
end
