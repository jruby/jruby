require File.expand_path('../../spec_helper', __FILE__)

describe FFI::Library, "#attach_function" do
  before :all do
    @klass = Class.new(FFI::Struct)
    @klass.layout :tv_sec, :ulong, 0, :tv_usec, :ulong, 4

    @libc = Module.new do
      extend FFI::Library
      attach_function :gettimeofday, [:pointer, :pointer], :int
    end
  end

  before :each do
    @t = @klass.new
    @time = @libc.gettimeofday(@t.pointer, nil)
  end

  it "correctly returns a value for gettimeofday" do
    @time.should be_kind_of(Integer)
  end

  it "correctly populates a struct for gettimeofday" do
    @t[:tv_sec].should be_kind_of(Numeric)
    @t[:tv_usec].should be_kind_of(Numeric)
  end
end
