require File.expand_path('../spec_helper', __FILE__)

describe "Custom type definitions" do
  before :each do
    @mod = Module.new do
      extend FFI::Library
      ffi_lib FFISpecs::LIBRARY
    end
  end

  it "attach_function with custom typedef" do
    @mod.typedef :uint, :fubar_t
    @mod.attach_function :ret_u32, [ :fubar_t ], :fubar_t

    @mod.ret_u32(0x12345678).should == 0x12345678
  end

  it "variadic invoker with custom typedef" do
    @mod.typedef :uint, :fubar_t
    @mod.attach_function :pack_varargs, [ :buffer_out, :string, :varargs ], :void

    buf = FFI::Buffer.new :uint, 10
    @mod.pack_varargs(buf, "i", :fubar_t, 0x12345678)
    buf.get_int64(0).should == 0x12345678
  end

  it "Callback with custom typedef parameter" do
    @mod.typedef :uint, :fubar3_t
    @mod.callback :cbIrV, [ :fubar3_t ], :void
    @mod.attach_function :testCallbackU32rV, :testClosureIrV, [ :cbIrV, :fubar3_t ], :void

    i = 0
    @mod.testCallbackU32rV(0xdeadbeef) { |v| i = v }
    i.should == 0xdeadbeef
  end

  it "Struct with custom typedef field" do
    s = FFISpecs::StructCustomTypedef::S.new
    s[:a] = 0x12345678
    s.pointer.get_uint(0).should == 0x12345678
  end
end
