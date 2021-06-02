#
# This file is part of ruby-ffi.
# For licensing, see LICENSE.SPECS
#

require File.expand_path(File.join(File.dirname(__FILE__), "spec_helper"))

describe "async callback" do
  module LibTest
    extend FFI::Library
    ffi_lib TestLibrary::PATH
    AsyncIntCallback = callback [ :int ], :void

    @blocking = true
    attach_function :testAsyncCallback, [ AsyncIntCallback, :int ], :void
  end

  it ":int (0x7fffffff) argument" do
    skip "not yet supported on TruffleRuby" if RUBY_ENGINE == "truffleruby"
    v = 0xdeadbeef
    called = false
    cb = Proc.new {|i| v = i; called = Thread.current }
    LibTest.testAsyncCallback(cb, 0x7fffffff)
    expect(called).to be_kind_of(Thread)
    expect(called).to_not eq(Thread.current)
    expect(v).to eq(0x7fffffff)
  end

  it "called a second time" do
    skip "not yet supported on TruffleRuby" if RUBY_ENGINE == "truffleruby"
    v = 1
    th1 = th2 = false
    LibTest.testAsyncCallback(2) { |i| v += i; th1 = Thread.current }
    LibTest.testAsyncCallback(3) { |i| v += i; th2 = Thread.current }
    expect(th1).to be_kind_of(Thread)
    expect(th2).to be_kind_of(Thread)
    expect(th1).to_not eq(Thread.current)
    expect(th2).to_not eq(Thread.current)
    expect(th1).to_not eq(th2)
    expect(v).to eq(6)
  end

  it "sets the name of the thread that runs the callback" do
    skip "not yet supported on TruffleRuby" if RUBY_ENGINE == "truffleruby"
    skip "not yet supported on JRuby" if RUBY_ENGINE == "jruby"

    callback_runner_thread = nil

    LibTest.testAsyncCallback(proc { callback_runner_thread = Thread.current }, 0)

    expect(callback_runner_thread.name).to eq("FFI Callback Runner")
  end
end
