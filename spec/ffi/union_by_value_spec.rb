#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))

describe 'Union by value calling convention' do
  module UnionByValueLibTest
    extend FFI::Library
    ffi_lib TestLibrary::PATH

    class I64F64 < FFI::Union
      layout :i, :int64, :d, :double
    end
    attach_function :union_i64_f64_make, [:double], I64F64.by_value
    attach_function :union_i64_f64_get, [I64F64.by_value], :double
    attach_function :union_i64_f64_make_i, [:int64], I64F64.by_value
    attach_function :union_i64_f64_get_i, [I64F64.by_value], :int64

    class F32x2 < FFI::Union
      layout :f, :float, :v, [:float, 2]
    end
    attach_function :union_f32x2_make, [:float, :float], F32x2.by_value
    attach_function :union_f32x2_get_0, [F32x2.by_value], :float
    attach_function :union_f32x2_get_1, [F32x2.by_value], :float

    class TagF64Struct < FFI::Struct
      layout :tag, :int32, :value, :double
    end
    class TagF64 < FFI::Union
      layout :s, TagF64Struct, :f, :float
    end
    attach_function :union_tag_f64_make, [:int32, :double], TagF64.by_value
    attach_function :union_tag_f64_get_tag, [TagF64.by_value], :int32
    attach_function :union_tag_f64_get_value, [TagF64.by_value], :double

    class F64F32x2 < FFI::Union
      layout :d, :double, :v, [:float, 2]
    end
    class StructWithUnion < FFI::Struct
      layout :x, :double, :u, F64F32x2
    end
    attach_function :struct_with_union_make, [:double, :double], StructWithUnion.by_value
    attach_function :struct_with_union_get_x, [StructWithUnion.by_value], :double
    attach_function :struct_with_union_get_d, [StructWithUnion.by_value], :double

    class F64x3I64 < FFI::Union
      layout :v, [:double, 3], :i, :int64
    end
    attach_function :union_f64x3_i64_make, [:double, :double, :double], F64x3I64.by_value
    attach_function :union_f64x3_i64_sum, [F64x3I64.by_value], :double

    class F64x5 < FFI::Union
      layout :v, [:double, 5], :w, [:double, 2]
    end
    attach_function :union_f64x5_make, [:double, :double, :double, :double, :double], F64x5.by_value
    attach_function :union_f64x5_sum, [F64x5.by_value], :double

    class Coord < FFI::Struct
      layout :x, :double, :y, :double, :z, :double, :t, :double
    end
    class F64x4 < FFI::Union
      layout :v, [:double, 4], :coord, Coord
    end
    class F32F64 < FFI::Union
      layout :f, :float, :d, :double
    end
    callback :f64x4_cb, [F64x4.by_value], :double
    callback :f32_f64_cb, [F32F64.by_value], :double
    callback :f64x4_make_cb, [:double, :double, :double, :double], F64x4.by_value
    attach_function :union_f64x4_callback, [:double, :double, :double, :double, :f64x4_cb], :double
    attach_function :union_f32_f64_callback, [:double, :f32_f64_cb], :double
    attach_function :union_f64x4_callback_ret_t, [:f64x4_make_cb], :double
  end

  it 'passes and returns a union of int64 and double as an integer' do
    u = UnionByValueLibTest.union_i64_f64_make(2.0000000000001)
    expect(u[:d]).to eq(2.0000000000001)
    expect(UnionByValueLibTest.union_i64_f64_get(u)).to eq(2.0000000000001)
    u = UnionByValueLibTest.union_i64_f64_make_i(0x1122334455667788)
    expect(u[:i]).to eq(0x1122334455667788)
    expect(UnionByValueLibTest.union_i64_f64_get_i(u)).to eq(0x1122334455667788)
  end

  it 'passes and returns a union of two floats as a homogeneous float aggregate' do
    u = UnionByValueLibTest.union_f32x2_make(1.5, 2.5)
    expect(u[:v].to_a).to eq([1.5, 2.5])
    expect(u[:f]).to eq(1.5)
    expect(UnionByValueLibTest.union_f32x2_get_0(u)).to eq(1.5)
    expect(UnionByValueLibTest.union_f32x2_get_1(u)).to eq(2.5)
  end

  it 'passes and returns a union whose eightbytes have different classes' do
    u = UnionByValueLibTest.union_tag_f64_make(42, 2.0000000000001)
    expect(u[:s][:tag]).to eq(42)
    expect(u[:s][:value]).to eq(2.0000000000001)
    expect(UnionByValueLibTest.union_tag_f64_get_tag(u)).to eq(42)
    expect(UnionByValueLibTest.union_tag_f64_get_value(u)).to eq(2.0000000000001)
  end

  it 'passes and returns a struct containing a union of mixed float types' do
    s = UnionByValueLibTest.struct_with_union_make(1.5, 2.0000000000001)
    expect(s[:x]).to eq(1.5)
    expect(s[:u][:d]).to eq(2.0000000000001)
    expect(UnionByValueLibTest.struct_with_union_get_x(s)).to eq(1.5)
    expect(UnionByValueLibTest.struct_with_union_get_d(s)).to eq(2.0000000000001)
  end

  it 'passes and returns a union larger than sixteen bytes through memory' do
    u = UnionByValueLibTest.union_f64x3_i64_make(1.5, 2.5, 3.5)
    expect(u[:v].to_a).to eq([1.5, 2.5, 3.5])
    expect(UnionByValueLibTest.union_f64x3_i64_sum(u)).to eq(7.5)
  end

  it 'passes and returns a union of five doubles through memory' do
    u = UnionByValueLibTest.union_f64x5_make(1.5, 2.5, 3.5, 4.5, 5.5)
    expect(u[:v].to_a).to eq([1.5, 2.5, 3.5, 4.5, 5.5])
    expect(UnionByValueLibTest.union_f64x5_sum(u)).to eq(17.5)
  end

  it 'passes a union of doubles by value to a callback' do
    t = UnionByValueLibTest.union_f64x4_callback(1.5, 2.5, 3.5, 4.5) { |u| u[:coord][:t] - u[:coord][:x] }
    expect(t).to eq(3.0)
  end

  it 'passes a union of float and double by value to a callback' do
    d = UnionByValueLibTest.union_f32_f64_callback(2.0000000000001) { |u| u[:d] }
    expect(d).to eq(2.0000000000001)
  end

  it 'returns a union of doubles by value from a callback' do
    t = UnionByValueLibTest.union_f64x4_callback_ret_t do |x, y, z, t|
      u = UnionByValueLibTest::F64x4.new
      u[:v][0] = x; u[:v][1] = y; u[:v][2] = z; u[:v][3] = t
      u
    end
    expect(t).to eq(4.5)
  end
end
