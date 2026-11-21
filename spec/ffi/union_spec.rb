#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))

module UnionSpec

Types = {
  [:union_test_t, 's8'] => [:char, :c, 1],
  [:union_test_t, 's16'] => [:short, :s, 0xff0],
  [:union_test_t, 's32'] => [:int, :i, 0xff00],
  [:union_test_t, 's64'] => [:long_long, :j, 0xffff00],
  [:union_test_t, 'long'] => [:long, :l, 0xffff],
  [:union_test_t, 'f32'] => [:float, :f, 1.0001],
  [:union_test_t, 'f64'] => [:double, :d, 1.000000001],
  [:union_small_test_t, 's8'] => [:char, :c, 74],
  [:union_small_test_t, 's16'] => [:short, :s, 0x2439],
  [:union_mixed_test_t, 's8'] => [:char, :c, 74],
  [:union_mixed_test_t, 'f32'] => [:float, :f, 4.555],
  [:union_float_test_t, 'f32'] => [:float, :f, 3.444],
  [:union_float_test_t, 'f64'] => [:double, :d, 2.0000000000001],
}

module LibTest
  extend FFI::Library
  ffi_lib TestLibrary::PATH

  class Test_union_test_t < FFI::Union
    layout( :a, [:char, 10],
            :i, :int,
            :f, :float,
            :d, :double,
            :s, :short,
            :l, :long,
            :j, :long_long,
            :c, :char )
  end
  class Test_union_small_test_t < FFI::Union
    layout( :c, :char,
            :s, :short)
  end
  class Test_union_mixed_test_t < FFI::Union
    layout( :f, :float,
            :c, :char)
  end
  class Test_union_float_test_t < FFI::Union
    layout( :f, :float,
            :d, :double)
  end

  Types.keys.each do |(uni, k)|
    attach_function "#{uni}_ptr_align_#{k}", [ :pointer ], Types[[uni, k]][0]
    attach_function "#{uni}_ptr_make_union_with_#{k}", [ Types[[uni, k]][0] ], :pointer
  end
  attach_function :union_size, [], :uint
  attach_function :union_small_size, [], :uint
  attach_function :union_mixed_size, [], :uint
  attach_function :union_float_size, [], :uint
end

describe 'Union' do
  it 'should place all the fields at offset 0' do
    expect(LibTest::Test_union_test_t.members.all? { |m| LibTest::Test_union_test_t.offset_of(m) == 0 }).to be true
  end

  Types.each do |(uni, k), (type, name, val)|
    it "should correctly align/write a #{type} value to #{uni} by pointer" do
      @u = LibTest.const_get("Test_#{uni}").new
      @u[name] = val
      if k == 'f32' or k == 'f64'
        expect((@u[name] - LibTest.send("#{uni}_ptr_align_#{k}", @u.to_ptr)).abs).to be < 0.00001
      else
        expect(@u[name]).to eq(LibTest.send("#{uni}_ptr_align_#{k}", @u.to_ptr))
      end
    end
  end

  Types.each do |(uni, k), (type, name, val)|
    it "should read a #{type} value from memory of #{uni} by pointer" do
      kl = LibTest.const_get("Test_#{uni}")
      @u = kl.new(LibTest.send("#{uni}_ptr_make_union_with_#{k}", val))
      if k == 'f32' or k == 'f64'
        expect((@u[name] - val).abs).to be < 0.00001
      else
        expect(@u[name]).to eq(val)
      end
    end
  end

  it 'should return a size equals to the size of the biggest field' do
    expect(LibTest::Test_union_test_t.size).to eq(LibTest.union_size)
  end

  it 'should return a size equals to the size of the biggest field of a small union' do
    expect(LibTest::Test_union_small_test_t.size).to eq(LibTest.union_small_size)
  end

  it 'should return a size equals to the size of the biggest field of a mixed union' do
    expect(LibTest::Test_union_mixed_test_t.size).to eq(LibTest.union_mixed_size)
  end

  it 'should return a size equals to the size of the biggest field of a float union' do
    expect(LibTest::Test_union_float_test_t.size).to eq(LibTest.union_float_size)
  end
end

describe 'Union by_value' do
  module ByValueLibTest
    extend FFI::Library
    ffi_lib TestLibrary::PATH

    class PjXyzt < FFI::Struct
      layout :x, :double, :y, :double, :z, :double, :t, :double
    end

    class UnionDouble < FFI::Union
      layout :v, [:double, 4],
             :coord, PjXyzt
    end

    attach_function :union_double_coord, [:double, :double, :double, :double], UnionDouble.by_value
    attach_function :union_double_get_x, [UnionDouble.by_value], :double
    attach_function :union_double_get_y, [UnionDouble.by_value], :double
    attach_function :union_double_get_z, [UnionDouble.by_value], :double
    attach_function :union_double_get_t, [UnionDouble.by_value], :double
    attach_function :union_double_add, [UnionDouble.by_value, UnionDouble.by_value], UnionDouble.by_value

    Types.keys.each do |(uni, k)|
      attach_function "#{uni}_val_align_#{k}", [ LibTest.const_get("Test_#{uni}").by_value ], Types[[uni, k]][0]
      attach_function "#{uni}_val_make_union_with_#{k}", [ Types[[uni, k]][0] ], LibTest.const_get("Test_#{uni}").by_value
    end
  end

  Types.each do |(uni, k), (type, name, val)|
    it "should correctly align/write a #{type} value to #{uni} by value" do
      @u = LibTest.const_get("Test_#{uni}").new
      @u[name] = val
      if k == 'f32' or k == 'f64'
        expect((@u[name] - ByValueLibTest.send("#{uni}_val_align_#{k}", @u)).abs).to be < 0.00001
      else
        expect(@u[name]).to eq(ByValueLibTest.send("#{uni}_val_align_#{k}", @u))
      end
    end
  end

  Types.each do |(uni, k), (type, name, val)|
    it "should read a #{type} value from memory of #{uni} by value" do
      @u = ByValueLibTest.send("#{uni}_val_make_union_with_#{k}", val)
      if k == 'f32' or k == 'f64'
        expect((@u[name] - val).abs).to be < 0.00001
      else
        expect(@u[name]).to eq(val)
      end
    end
  end


  it 'should return a union of doubles by value' do
    u = ByValueLibTest.union_double_coord(1.5, 2.5, 3.5, 4.5)
    expect(u[:coord][:x]).to eq(1.5)
    expect(u[:coord][:y]).to eq(2.5)
    expect(u[:coord][:z]).to eq(3.5)
    expect(u[:coord][:t]).to eq(4.5)
  end

  it 'should pass a union of doubles by value' do
    u = ByValueLibTest.union_double_coord(10.0, 20.0, 30.0, 40.0)
    expect(ByValueLibTest.union_double_get_x(u)).to eq(10.0)
    expect(ByValueLibTest.union_double_get_y(u)).to eq(20.0)
    expect(ByValueLibTest.union_double_get_z(u)).to eq(30.0)
    expect(ByValueLibTest.union_double_get_t(u)).to eq(40.0)
  end

  it 'should pass and return a union of doubles by value' do
    a = ByValueLibTest.union_double_coord(1.0, 2.0, 3.0, 4.0)
    b = ByValueLibTest.union_double_coord(0.5, 0.5, 0.5, 0.5)
    r = ByValueLibTest.union_double_add(a, b)
    expect(r[:coord][:x]).to eq(1.5)
    expect(r[:coord][:y]).to eq(2.5)
    expect(r[:coord][:z]).to eq(3.5)
    expect(r[:coord][:t]).to eq(4.5)
  end

  it 'should access union doubles via array field' do
    u = ByValueLibTest.union_double_coord(1.5, 2.5, 3.5, 4.5)
    expect(u[:v].to_a).to eq([1.5, 2.5, 3.5, 4.5])
  end
end if RUBY_ENGINE != "truffleruby"
end
