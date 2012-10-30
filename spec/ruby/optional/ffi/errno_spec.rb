require File.expand_path('../spec_helper', __FILE__)

describe "FFI.errno" do
  it "FFI.errno contains errno from last function" do
    FFISpecs::LibTest.setLastError(0)
    FFISpecs::LibTest.setLastError(0x12345678)
    FFI.errno.should == 0x12345678
  end
end
