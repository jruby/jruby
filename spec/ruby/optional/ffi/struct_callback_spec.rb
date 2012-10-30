require File.expand_path('../spec_helper', __FILE__)

describe FFI::Struct, ' with inline callback functions' do
  it "defines inline callback field" do
    lambda {
      Module.new do
        extend FFI::Library
        ffi_lib FFISpecs::LIBRARY

        struct = Class.new(FFI::Struct) do
          layout \
            :add, callback([ :int, :int ], :int),
            :sub, callback([ :int, :int ], :int)
        end

        attach_function :struct_call_add_cb, [struct, :int, :int], :int
        attach_function :struct_call_sub_cb, [struct, :int, :int], :int
      end
    }.should_not raise_error
  end
end
