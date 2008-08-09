require 'ffi'

describe "Struct aligns fields correctly" do
  it "char, followed by an int" do
    class CIStruct < JRuby::FFI::Struct
      layout :c => :char, :i => :int
    end
    CIStruct.size.should == 8
  end
  it "short, followed by an int" do
    class SIStruct < JRuby::FFI::Struct
      layout :s => :short, :i => :int
    end
    SIStruct.size.should == 8
  end
  it "int, followed by an int" do
    class IIStruct < JRuby::FFI::Struct
      layout :i1 => :int, :i => :int
    end
    IIStruct.size.should == 8
  end
  it "long long, followed by an int" do
    class LLIStruct < JRuby::FFI::Struct
      layout :l => :long_long, :i => :int
    end
    LLIStruct.size.should == 12
  end
end