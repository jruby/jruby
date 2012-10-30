require File.expand_path('../spec_helper', __FILE__)

describe "String tests" do
  it "FFI::MemoryPointer#get_string returns a tainted string" do
    mp = FFI::MemoryPointer.new 1024
    mp.put_string(0, "test\0")
    str = mp.get_string(0)
    str.tainted?.should == true
  end

  it "String returned by a method is tainted" do
    mp = FFI::MemoryPointer.new :pointer
    sp = FFI::MemoryPointer.new 1024
    sp.put_string(0, "test")
    mp.put_pointer(0, sp)

    # TODO: Have to define this manually because setting it in the fixture
    # classes will override it with different function definitions.
    FFISpecs::LibTest.attach_function :ptr_ret_pointer, [ :pointer, :int], :string

    str = FFISpecs::LibTest.ptr_ret_pointer(mp, 0)
    str.should == "test"
    str.tainted?.should == true
  end

  it "Poison null byte raises error" do
    s = "123\0abc"
    lambda { FFISpecs::LibTest.string_equals(s, s) }.should raise_error
  end

  not_compliant_on :rubinius do
    it "Tainted String parameter should throw a SecurityError" do
      $SAFE = 1
      str = "test"
      str.taint
      begin
        FFISpecs::LibTest.string_equals(str, str).should == false
      rescue SecurityError => e
      end
    end
  end

  it "casts nil as NULL pointer" do
    lambda { FFISpecs::LibTest.string_dummy(nil) }.should_not raise_error
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
    ptrary.get_array_of_string(0).should == ["foo", "bar", "baz"]
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
    ptrary.get_array_of_string(0, 4).should == ["foo", "bar", nil, "baz"]
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
    ptrary.get_array_of_string(2 * FFI.type_size(:pointer), 3).should == ["baz", "testing", "ffi"]
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
    lambda { ptrary.get_array_of_string(0, 6) }.should raise_error
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
    lambda { ptrary.get_array_of_string(-1) }.should raise_error
  end
end
