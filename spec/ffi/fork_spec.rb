#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))

if Process.respond_to?(:fork)
  describe "Callback in conjunction with fork()" do

    module LibTest
      extend FFI::Library
      ffi_lib TestLibrary::PATH

      callback :cbVrL, [ ], :long
      attach_function :testCallbackVrL, :testClosureVrL, [ :cbVrL ], :long
    end

    it "works with forked process and GC" do
      expect(LibTest.testCallbackVrL { 12345 }).to eq(12345)
      fork do
        expect(LibTest.testCallbackVrL { 12345 }).to eq(12345)
      end
      expect(LibTest.testCallbackVrL { 12345 }).to eq(12345)
      GC.start
    end

    it "works with forked process and free()" do
      cbf = FFI::Function.new(FFI::Type::LONG, []) { 234 }

      fork do
        cbf.free
      end

      expect(LibTest.testCallbackVrL(cbf)).to eq(234)
      cbf.free
    end
  end
end
