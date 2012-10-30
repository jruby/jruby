require File.expand_path('../spec_helper', __FILE__)

describe FFI::Struct, ' with an initialize function' do
  it "calls the initialize function" do
    struct_with_initialize = Class.new(FFI::Struct) do
      layout :string, :string
      attr_accessor :magic
      def initialize
        super
        self.magic = 42
      end
    end

    struct_with_initialize.new.magic.should == 42
  end
end

describe FFI::ManagedStruct, ' with an initialize function' do
  it "calls the initialize function" do
    managed_struct_with_initialize = Class.new(FFI::ManagedStruct) do
      layout :string, :string
      attr_accessor :magic
      def initialize
        super FFI::MemoryPointer.new(:pointer).put_int(0, 0x1234).get_pointer(0)
        self.magic = 42
      end
      def self.release;end
    end

    managed_struct_with_initialize.new.magic.should == 42
  end
end
