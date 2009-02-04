require 'ffi'

describe "Struct aligns fields correctly" do
  it "char, followed by an int" do
    class CIStruct < FFI::Struct
      layout :c => :char, :i => :int
    end
    CIStruct.size.should == 8
  end
  it "short, followed by an int" do
    class SIStruct < FFI::Struct
      layout :s => :short, :i => :int
    end
    SIStruct.size.should == 8
  end
  it "int, followed by an int" do
    class IIStruct < FFI::Struct
      layout :i1 => :int, :i => :int
    end
    IIStruct.size.should == 8
  end
  it "long long, followed by an int" do
    class LLIStruct < FFI::Struct
      layout :l => :long_long, :i => :int
    end
    LLIStruct.size.should == (FFI::Platform::LONG_SIZE == 32 ? 12 : 16)
  end
end
