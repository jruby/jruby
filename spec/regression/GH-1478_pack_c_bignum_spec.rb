# coding: utf-8

describe "Array#pack('c')" do
  it 'does not raise RangeError when argument is bignum' do
    [0xfffffffffffffffff].pack("c").should == "\xFF".force_encoding('ASCII-8BIT')
  end
end

describe "Array#pack('C')" do
  it 'does not raise RangeError when argument is bignum' do
    [0xfffffffffffffffff].pack("C").should == "\xFF".force_encoding('ASCII-8BIT')
  end
end
