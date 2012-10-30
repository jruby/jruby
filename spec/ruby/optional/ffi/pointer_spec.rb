require File.expand_path('../spec_helper', __FILE__)

describe "Pointer" do
  it "Any object implementing #to_ptr can be passed as a :pointer parameter" do
    memory = FFI::MemoryPointer.new :long_long
    magic = 0x12345678
    memory.put_int32(0, magic)
    tp = FFISpecs::ToPtrTest.new(memory)
    FFISpecs::LibTest.ptr_ret_int32_t(tp, 0).should == magic
  end

  it "A DelegateClass(Pointer) can be passed as a :pointer parameter" do
    memory = FFI::MemoryPointer.new :long_long
    magic = 0x12345678
    memory.put_int32(0, magic)
    ptr = FFISpecs::PointerDelegate.new(memory)
    FFISpecs::LibTest.ptr_ret_int32_t(ptr, 0).should == magic
  end

  it "Fixnum cannot be used as a Pointer argument" do
    lambda { FFISpecs::LibTest.ptr_ret_int32(0, 0) }.should raise_error
  end

  it "Bignum cannot be used as a Pointer argument" do
    lambda { FFISpecs::LibTest.ptr_ret_int32(0xfee1deadbeefcafebabe, 0) }.should raise_error
  end

  # TODO: Shouldn't these use #it ?
  describe "pointer type methods" do
    describe "#read_pointer" do
      memory = FFI::MemoryPointer.new :pointer
      FFISpecs::LibTest.ptr_set_pointer(memory, 0, FFISpecs::LibTest.ptr_from_address(0xdeadbeef))
      memory.read_pointer.address.should == 0xdeadbeef
    end

    describe "#write_pointer" do
      memory = FFI::MemoryPointer.new :pointer
      memory.write_pointer(FFISpecs::LibTest.ptr_from_address(0xdeadbeef))

      # TODO: Have to define this manually because setting it in the fixture
      # classes will override it with different function definitions.

      FFISpecs::LibTest.attach_function :ptr_ret_pointer, [ :pointer, :int ], :pointer
      FFISpecs::LibTest.ptr_ret_pointer(memory, 0).address.should == 0xdeadbeef
    end

    describe "#read_array_of_pointer" do
      values = [0x12345678, 0xfeedf00d, 0xdeadbeef]
      memory = FFI::MemoryPointer.new :pointer, values.size
      values.each_with_index do |address, j|
        FFISpecs::LibTest.ptr_set_pointer(memory, j * FFI.type_size(:pointer), FFISpecs::LibTest.ptr_from_address(address))
      end
      array = memory.read_array_of_pointer(values.size)
      values.each_with_index do |address, j|
        array[j].address.should == address
      end
    end

    describe "#write_array_of_pointer" do
      values = [0x12345678, 0xfeedf00d, 0xdeadbeef]
      memory = FFI::MemoryPointer.new :pointer, values.size
      memory.write_array_of_pointer(values.map { |address| FFISpecs::LibTest.ptr_from_address(address) })
      array = []
      values.each_with_index do |address, j|
        array << FFISpecs::LibTest.ptr_ret_pointer(memory, j * FFI.type_size(:pointer))
      end
      values.each_with_index do |address, j|
        array[j].address.should == address
      end
    end
  end

  describe "NULL" do
    it "is obtained using Pointer::NULL constant" do
      null_ptr = FFI::Pointer::NULL
      null_ptr.null?.should be_true
    end

    it "is obtained passing address 0 to constructor" do
      FFI::Pointer.new(0).null?.should be_true
    end

    it "raises an error when attempting read/write operations on it" do
      null_ptr = FFI::Pointer::NULL
      lambda { null_ptr.read_int }.should raise_error(FFI::NullPointerError)
      lambda { null_ptr.write_int(0xff1) }.should raise_error(FFI::NullPointerError)
    end
  end
end

describe "AutoPointer" do
  before :all do
    @loop_count = 30
    @wiggle_room = 2 # GC rarely cleans up all objects. we can get most of them, and that's enough to determine if the basic functionality is working.
    @magic = 0x12345678
  end

  after :each do
    FFISpecs::AutoPointerTestHelper.gc_everything @loop_count
  end

  it "cleanup via default release method" do
    FFI::AutoPointer.should_receive(:release).at_least(@loop_count - @wiggle_room).times
    FFISpecs::AutoPointerTestHelper.reset
    @loop_count.times do
      # note that if we called
      # FFISpecs::AutoPointerTestHelper.method(:release).to_proc inline, we'd
      # have a reference to the pointer and it would never get GC'd.
      ap = FFI::AutoPointer.new(FFISpecs::LibTest.ptr_from_address(@magic))
    end
  end

  it "cleanup when passed a proc" do
    #  NOTE: passing a proc is touchy, because it's so easy to create a memory leak.
    #
    #  specifically, if we made an inline call to
    #
    #      FFISpecs::AutoPointerTestHelper.method(:release).to_proc
    #
    #  we'd have a reference to the pointer and it would
    #  never get GC'd.
    FFISpecs::AutoPointerTestHelper.should_receive(:release).at_least(@loop_count - @wiggle_room).times
    FFISpecs::AutoPointerTestHelper.reset
    @loop_count.times do
      ap = FFI::AutoPointer.new(FFISpecs::LibTest.ptr_from_address(@magic),
                                FFISpecs::AutoPointerTestHelper.finalizer)
    end
  end

  it "cleanup when passed a method" do
    FFISpecs::AutoPointerTestHelper.should_receive(:release).at_least(@loop_count - @wiggle_room).times
    FFISpecs::AutoPointerTestHelper.reset
    @loop_count.times do
      ap = FFI::AutoPointer.new(FFISpecs::LibTest.ptr_from_address(@magic),
                                FFISpecs::AutoPointerTestHelper.method(:release))
    end
  end
end

describe "AutoPointer#new" do
  it "MemoryPointer argument raises TypeError" do
    lambda {
      FFI::AutoPointer.new(FFI::MemoryPointer.new(:int))
    }.should raise_error(TypeError)
  end

  it "AutoPointer argument raises TypeError" do
    lambda {
      FFI::AutoPointer.new(FFI::AutoPointer.new(FFISpecs::LibTest.ptr_from_address(0)))
    }.should raise_error(TypeError)
  end

  it "Buffer argument raises TypeError" do
    lambda {
      FFI::AutoPointer.new(FFI::Buffer.new(:int))
    }.should raise_error(TypeError)
  end
end
