#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))

if Process.respond_to?(:fork)
  describe "Callback in conjunction with fork()" do

    libtest = Module.new do
      extend FFI::Library
      ffi_lib TestLibrary::PATH

      callback :cbVrL, [ ], :long
      attach_function :testCallbackVrL, :testClosureVrL, [ :cbVrL ], :long
      AsyncIntCallback = callback [ :int ], :void
      attach_function :testAsyncCallback, [ AsyncIntCallback, :int ], :void, blocking: true
    end

    it "works with forked process and GC" do
      expect(libtest.testCallbackVrL { 12345 }).to eq(12345)
      fork do
        expect(libtest.testCallbackVrL { 12345 }).to eq(12345)
        Process.exit 42
      end
      expect(libtest.testCallbackVrL { 12345 }).to eq(12345)
      GC.start

      expect(Process.wait2[1].exitstatus).to eq(42)
    end

    it "works with forked process and free()" do
      cbf = FFI::Function.new(FFI::Type::LONG, []) { 234 }

      fork do
        cbf.free
        Process.exit 43
      end

      expect(libtest.testCallbackVrL(cbf)).to eq(234)
      cbf.free

      expect(Process.wait2[1].exitstatus).to eq(43)
    end

    def run_async_callback(libtest)
      recv = nil
      libtest.testAsyncCallback(proc { |r| recv = r }, 23)
      expect(recv).to eq(23)
    end

    it "async thread dispatch works after forking" do
      run_async_callback(libtest)

      fork do
        run_async_callback(libtest)
        Process.exit 44
      end

      run_async_callback(libtest)

      expect(Process.wait2[1].exitstatus).to eq(44)
    end
  end
end
