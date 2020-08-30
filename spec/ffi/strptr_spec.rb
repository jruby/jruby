# -*- encoding: utf-8 -*-
#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))

describe "functions returning :strptr" do

  it "can attach function with :strptr return type" do
    expect do
      Module.new do
        extend FFI::Library
        ffi_lib FFI::Library::LIBC
        if !FFI::Platform.windows?
          attach_function :strdup, [ :string ], :strptr
        else
          attach_function :_strdup, [ :string ], :strptr
        end
      end
    end.not_to raise_error
  end

  module StrPtr
    extend FFI::Library
    ffi_lib FFI::Library::LIBC
    attach_function :free, [ :pointer ], :void
    if !FFI::Platform.windows?
      attach_function :strdup, [ :string ], :strptr
    else
      attach_function :strdup, :_strdup, [ :string ], :strptr
    end
  end

  it "should return [ String, Pointer ]" do
    result = StrPtr.strdup("test")
    expect(result[0]).to be_kind_of(String)
    expect(result[1]).to be_kind_of(FFI::Pointer)
  end

  it "should return the correct value" do
    result = StrPtr.strdup("testü")
    expect(result[0]).to eq("testü".b)
  end

  it "should return correct pointer" do
    result = StrPtr.strdup("testö")
    expect(result[1]).not_to be_null
    expect(result[1].read_string).to eq("testö".b)
  end
end
