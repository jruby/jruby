require 'ffi'

describe "Callback" do
  module LibC
    extend FFI::Library
    callback :qsort_cmp, [ :pointer, :pointer ], :int
    attach_function :qsort, [ :pointer, :int, :int, :qsort_cmp ], :int
  end
  it "arguments get passed correctly" do
    p = MemoryPointer.new(:int, 2)
    p.put_array_of_int32(0, [ 1 , 2 ])
    args = []
    cmp = proc do |p1, p2| args.push(p1.get_int(0)); args.push(p2.get_int(0)); 0; end
    # this is a bit dodgey, as it relies on qsort passing the args in order
    LibC.qsort(p, 2, 4, cmp)
    args.should == [ 1, 2 ]
  end

  it "Block can be substituted for Callback as last argument" do
    p = MemoryPointer.new(:int, 2)
    p.put_array_of_int32(0, [ 1 , 2 ])
    args = []
    # this is a bit dodgey, as it relies on qsort passing the args in order
    LibC.qsort(p, 2, 4) do |p1, p2| 
      args.push(p1.get_int(0))
      args.push(p2.get_int(0))
      0
    end
    args.should == [ 1, 2 ]
  end  
end