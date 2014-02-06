#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require 'ffi'
require_relative 'spec_helper'

class Timeval < FFI::Struct
  layout :tv_sec, :ulong, 0, :tv_usec, :ulong, 4  
end

module LibC
  extend FFI::Library
  ffi_lib FFI::Library::LIBC

  attach_function :gettimeofday, [:pointer, :pointer], :int
end

describe FFI::Library, "#attach_function" do
  it "correctly returns a value for gettimeofday" do
    t = Timeval.new
    time = LibC.gettimeofday(t.pointer, nil)
    time.should be_kind_of(Integer)
  end
  
  it "correctly populates a struct for gettimeofday" do
    t = Timeval.new
    time = LibC.gettimeofday(t.pointer, nil)
    t[:tv_sec].should be_kind_of(Numeric)
    t[:tv_usec].should be_kind_of(Numeric)
  end
end

