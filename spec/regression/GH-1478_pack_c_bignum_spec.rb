describe "Array#pack('c')" do
  it 'does not raise RangeError when argument is bignum' do
    [0xfffffffffffffffff].pack("c").should == "\xFF"
  end
end unless RUBY_VERSION < '1.9'

describe "Array#pack('C')" do
  it 'does not raise RangeError when argument is bignum' do
    [0xfffffffffffffffff].pack("C").should == "\xFF"
  end
end unless RUBY_VERSION < '1.9'
