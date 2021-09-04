#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))

describe "Function with variadic arguments" do
  module LibTest
    extend FFI::Library
    ffi_lib TestLibrary::PATH
    enum :enum_type1, [:c1, :c2]
    enum :enum_type2, [:c3, 42, :c4]
    attach_function :pack_varargs, [ :buffer_out, :string, :varargs ], :void
    attach_function :pack_varargs2, [ :buffer_out, :enum_type1, :string, :varargs ], :enum_type1
    callback :cbVrD, [ ], :double
    callback :cbVrI, [:int], :int
    callback :cbVrL, [:long], :long

    attach_function :testBlockingOpen, [ ], :pointer
    attach_function :testBlockingRWva, [ :pointer, :char, :varargs ], :char, :blocking => true
    attach_function :testBlockingWRva, [ :pointer, :char, :varargs ], :char, :blocking => true
    attach_function :testBlockingClose, [ :pointer ], :void
    attach_function :testCallbackVrDva, :testClosureVrDva, [ :double, :varargs ], :double
    attach_function :testCallbackVrILva, :testClosureVrILva, [ :int, :long, :varargs ], :long
  end

  it "takes enum arguments" do
    buf = FFI::Buffer.new :long_long, 2
    LibTest.pack_varargs(buf, "ii", :int, :c3, :int, :c4)
    expect(buf.get_int64(0)).to eq(42)
    expect(buf.get_int64(8)).to eq(43)
  end

  it "returns symbols for enums" do
    buf = FFI::Buffer.new :long_long, 2
    expect(LibTest.pack_varargs2(buf, :c1, "ii", :int, :c3, :int, :c4)).to eq(:c2)
  end

  it 'can wrap a blocking function with varargs' do
    handle = LibTest.testBlockingOpen
    expect(handle).not_to be_null
    begin
      thWR = Thread.new { LibTest.testBlockingWRva(handle, 63, :int, 40, :int, 23, :int, 0) }
      thRW = Thread.new { LibTest.testBlockingRWva(handle, 64, :int, 40, :int, 24, :int, 0) }
      expect(thWR.value).to eq(64)
      expect(thRW.value).to eq(63)
    ensure
      LibTest.testBlockingClose(handle)
    end
  end

  [ 0, 127, -128, -1 ].each do |i|
    it "call variadic with (:char (#{i})) argument" do
      buf = FFI::Buffer.new :long_long
      LibTest.pack_varargs(buf, "c", :char, i)
      expect(buf.get_int64(0)).to eq(i)
    end
  end

  [ 0, 0x7f, 0x80, 0xff ].each do |i|
    it "call variadic with (:uchar (#{i})) argument" do
      buf = FFI::Buffer.new :long_long
      LibTest.pack_varargs(buf, "C", :uchar, i)
      expect(buf.get_int64(0)).to eq(i)
    end
  end

  [ 0, 1.234567, 9.87654321 ].each do |v|
    it "call variadic with (:float (#{v})) argument" do
      buf = FFI::Buffer.new :long_long
      LibTest.pack_varargs(buf, "f", :float, v.to_f)
      expect(buf.get_float64(0)).to eq(v)
    end
  end

  [ 0, 1.234567, 9.87654321 ].each do |v|
    it "call variadic with (:double (#{v})) argument" do
      buf = FFI::Buffer.new :long_long
      LibTest.pack_varargs(buf, "f", :double, v.to_f)
      expect(buf.get_float64(0)).to eq(v)
    end
  end

  it "call variadic with callback argument" do
    pr = proc { 42.0 }
    expect(LibTest.testCallbackVrDva(3.0, :cbVrD, pr)).to be_within(0.0000001).of(45.0)
  end

  it "call variadic with several callback arguments" do
    pr1 = proc { |i| i + 1 }
    pr2 = proc { |l| l + 2 }
    expect(LibTest.testCallbackVrILva(5, 6, :cbVrI, pr1, :cbVrL, pr2)).to eq(14)
  end

  module Varargs
    PACK_VALUES = {
      'c' => [ 0x12  ],
      'C' => [ 0x34  ],
      's' => [ 0x5678 ],
      'S' => [ 0x9abc ],
      'i' => [ 0x7654321f ],
      'I' => [ 0xfee1babe ],
      'l' => [ 0x1f2e3d4c ],
      'L' => [ 0xf7e8d9ca ],
      'j' => [ 0x1eafdeadbeefa1b2 ],
      'f' => [ 1.23456789 ],
      'd' => [ 9.87654321 ]
    }

    TYPE_MAP = {
      'c' => :char, 'C' => :uchar, 's' => :short, 'S' => :ushort,
      'i' => :int, 'I' => :uint, 'j' => :long_long, 'J' => :ulong_long,
      'l' => :long, 'L' => :ulong, 'f' => :float, 'd' => :double
    }
  end

  def verify(p, off, v)
    if v.kind_of?(Float)
      expect(p.get_float64(off)).to eq(v)
    else
      expect(p.get_int64(off)).to eq(v)
    end
  end

  Varargs::PACK_VALUES.keys.each do |t1|
    Varargs::PACK_VALUES.keys.each do |t2|
      Varargs::PACK_VALUES.keys.each do |t3|
        Varargs::PACK_VALUES[t1].each do |v1|
          Varargs::PACK_VALUES[t2].each do |v2|
            Varargs::PACK_VALUES[t3].each do |v3|
              fmt = "#{t1}#{t2}#{t3}"
              params = [ Varargs::TYPE_MAP[t1], v1, Varargs::TYPE_MAP[t2], v2, Varargs::TYPE_MAP[t3], v3 ]
              it "call(#{fmt}, #{params.join(',')})" do
                buf = FFI::Buffer.new :long_long, 3
                LibTest.pack_varargs(buf, fmt, *params)
                verify(buf, 0, v1)
                verify(buf, 8, v2)
                verify(buf, 16, v3)
              end
            end
          end
        end
      end
    end
  end
end
