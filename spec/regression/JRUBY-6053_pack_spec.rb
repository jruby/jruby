require 'rspec'

describe 'JRUBY-6053: Array#pack' do
  it 'returns 2 bytes for "b2"' do
    ["1"].pack("b2").should == "\x01\x00"
  end

  it 'returns 0 bytes for "b0"' do
    ["1"].pack("b0").should == ""
  end

  it 'returns 0 bytes for "b1"' do
    ["1"].pack("b1").should == "\x01"
  end

  it 'returns 0 bytes for "b3"' do
    ["1"].pack("b3").should == "\x01\x00"
  end

  it 'returns 0 bytes for "b4"' do
    ["1"].pack("b4").should == "\x01\x00\x00"
  end

  it 'returns 2 bytes for "B2"' do
    ["1"].pack("B2").should == "\x80\x00"
  end
end
