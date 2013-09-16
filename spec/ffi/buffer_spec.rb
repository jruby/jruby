require 'ffi'
MemoryPointer = FFI::MemoryPointer
Buffer = FFI::Buffer
Platform = FFI::Platform
LongSize = FFI::Platform::LONG_SIZE / 8

describe "Buffer#total" do
  [1,2,3].each do |i|
    { :char => 1, :uchar => 1, :short => 2, :ushort => 2, :int => 4, :uint => 4, \
        :long => LongSize, :ulong => LongSize, :long_long => 8, :ulong_long => 8, \
        :float => 4, :double => 8
    }.each_pair do |t, s|
      it "Buffer.alloc_in(#{t}, #{i}).total == #{i * s}" do
        expect(Buffer.alloc_in(t, i).total).to  eq(i * s)
      end
      it "Buffer.alloc_out(#{t}, #{i}).total == #{i * s}" do
        expect(Buffer.alloc_out(t, i).total).to eq(i * s)
      end
      it "Buffer.alloc_inout(#{t}, #{i}).total == #{i * s}" do
        expect(Buffer.alloc_inout(t, i).total).to eq(i * s)
      end
    end
  end
end

describe "Buffer#put_char" do
  bufsize = 4
  (0..127).each do |i|
    (0..bufsize-1).each do |offset|
      it "put_char(#{offset}, #{i}).get_char(#{offset}) == #{i}" do
        expect(Buffer.alloc_in(bufsize).put_char(offset, i).get_char(offset)).to  eq(i)
      end
    end
  end
end
describe "Buffer#put_uchar" do
  bufsize = 4
  (0..255).each do |i|
    (0..bufsize-1).each do |offset|
      it "Buffer.put_uchar(#{offset}, #{i}).get_uchar(#{offset}) == #{i}" do
        Buffer.alloc_in(bufsize).put_uchar(offset, i).get_uchar(offset).should == i
      end
    end
  end 
end
describe "Buffer#put_short" do
  bufsize = 4
  [0, 1, 128, 32767].each do |i|
    (0..bufsize-2).each do |offset|
      it "put_short(#{offset}, #{i}).get_short(#{offset}) == #{i}" do
        expect(Buffer.alloc_in(bufsize).put_short(offset, i).get_short(offset)).to eq i
      end
    end
  end
end
describe "Buffer#put_ushort" do
  bufsize = 4
  [ 0, 1, 128, 32767, 65535, 0xfee1, 0xdead, 0xbeef, 0xcafe ].each do |i|
    (0..bufsize-2).each do |offset|
      it "put_ushort(#{offset}, #{i}).get_ushort(#{offset}) == #{i}" do
        expect(Buffer.alloc_in(bufsize).put_ushort(offset, i).get_ushort(offset)).to eq i
      end
    end
  end
end
describe "Buffer#put_int" do
  bufsize = 8
  [0, 1, 128, 32767, 0x7ffffff ].each do |i|
    (0..bufsize-4).each do |offset|
      it "put_int(#{offset}, #{i}).get_int(#{offset}) == #{i}" do
        expect(Buffer.alloc_in(bufsize).put_int(offset, i).get_int(offset)).to eq i
      end
    end
  end
end
describe "Buffer#put_uint" do
  bufsize = 8
  [ 0, 1, 128, 32767, 65535, 0xfee1dead, 0xcafebabe, 0xffffffff ].each do |i|
    (0..bufsize-4).each do |offset|
      it "put_uint(#{offset}, #{i}).get_uint(#{offset}) == #{i}" do
        expect(Buffer.alloc_in(bufsize).put_uint(offset, i).get_uint(offset)).to eq i
      end
    end
  end
end
describe "Buffer#put_long" do
  bufsize = 16
  [0, 1, 128, 32767, 0x7ffffff ].each do |i|
    (0..bufsize-LongSize).each do |offset|
      it "put_long(#{offset}, #{i}).get_long(#{offset}) == #{i}" do
        expect(Buffer.alloc_in(bufsize).put_long(offset, i).get_long(offset)).to eq i
      end
    end
  end
end
describe "Buffer#put_ulong" do
  bufsize = 16
  [ 0, 1, 128, 32767, 65535, 0xfee1dead, 0xcafebabe, 0xffffffff ].each do |i|
    (0..bufsize-LongSize).each do |offset|
      it "put_ulong(#{offset}, #{i}).get_ulong(#{offset}) == #{i}" do
        expect(Buffer.alloc_in(bufsize).put_ulong(offset, i).get_ulong(offset)).to eq i
      end
    end
  end
end
describe "Buffer#put_long_long" do
  bufsize = 16
  [0, 1, 128, 32767, 0x7ffffffffffffff ].each do |i|
    (0..bufsize-8).each do |offset|
      it "put_long_long(#{offset}, #{i}).get_long_long(#{offset}) == #{i}" do
        expect(Buffer.alloc_in(bufsize).put_long_long(offset, i).get_long_long(offset)).to eq i
      end
    end
  end
end
describe "Buffer#put_ulong_long" do
  bufsize = 16
  [ 0, 1, 128, 32767, 65535, 0xdeadcafebabe, 0x7fffffffffffffff ].each do |i|
    (0..bufsize-8).each do |offset|
      it "put_ulong_long(#{offset}, #{i}).get_ulong_long(#{offset}) == #{i}" do
        expect(Buffer.alloc_in(bufsize).put_ulong_long(offset, i).get_ulong_long(offset)).to eq i
      end
    end
  end
end
describe "Buffer#put_pointer" do
  it "put_pointer(0, p).get_pointer(0) == p" do
    p = MemoryPointer.new :ulong_long
    p.put_uint(0, 0xdeadbeef)
    buf = Buffer.alloc_inout 8
    p2 = buf.put_pointer(0, p).get_pointer(0)
    expect(p2).to_not be_nil
    expect(p2).to eq p
    expect(p2.get_uint(0)).to eq 0xdeadbeef
  end
end