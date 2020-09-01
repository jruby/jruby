#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))
require 'bigdecimal'

# long double not yet supported on TruffleRuby
describe ":long_double arguments and return values" do
  module LibTest
    extend FFI::Library
    ffi_lib TestLibrary::PATH

    callback :cbVrLD, [ ], :long_double
    attach_function :testCallbackVrLD, :testClosureVrLD, [ :cbVrLD ], :long_double

    callback :cbLDrV, [ :long_double ], :void
    attach_function :testCallbackLDrV, :testClosureLDrV, [ :cbLDrV, :long_double ], :void

    attach_function :add_f128, [ :long_double, :long_double ], :long_double
    attach_function :ret_f128, [ :long_double ], :long_double
  end

  it "returns first parameter" do
    expect(LibTest.ret_f128(0.1)).to be_within(0.01).of(0.1)
  end

  it "returns first parameter with high precision", if: FFI::Platform::LONG_DOUBLE_SIZE > 64 do
    ld =        BigDecimal("1.234567890123456789")
    tolerance = BigDecimal("0.0000000000000000001")
    expect(LibTest.ret_f128(ld)).to be_within(tolerance).of(ld)
  end

  it "add two long double numbers" do
    expect(LibTest.add_f128(0.1, 0.2)).to be_within(0.01).of(0.3)
  end

  describe "Callback" do
    it "returning :long_double" do
      expect(LibTest.testCallbackVrLD { -0.1 }).to be_within(0.01).of(-0.1)
    end

    it ":long_double argument" do
      v = nil
      LibTest.testCallbackLDrV(0.1) { |i| v = i }
      expect(v).to be_within(0.01).of(0.1)
    end
  end
end unless ['truffleruby', 'jruby'].include?(RUBY_ENGINE)
