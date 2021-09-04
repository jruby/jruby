# coding: utf-8

describe "Array#pack('c')" do
  it 'does not raise RangeError when argument is bignum' do
    expect([0xfffffffffffffffff].pack("c")).to eq("\xFF".force_encoding('ASCII-8BIT'))
  end
end

describe "Array#pack('C')" do
  it 'does not raise RangeError when argument is bignum' do
    expect([0xfffffffffffffffff].pack("C")).to eq("\xFF".force_encoding('ASCII-8BIT'))
  end
end
