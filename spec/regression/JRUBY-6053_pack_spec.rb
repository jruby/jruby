require 'rspec'

describe 'JRUBY-6053: Array#pack' do
  it 'returns 2 bytes for "b2"' do
    expect(["1"].pack("b2")).to eq("\x01\x00")
  end

  it 'returns 0 bytes for "b0"' do
    expect(["1"].pack("b0")).to eq("")
  end

  it 'returns 0 bytes for "b1"' do
    expect(["1"].pack("b1")).to eq("\x01")
  end

  it 'returns 0 bytes for "b3"' do
    expect(["1"].pack("b3")).to eq("\x01\x00")
  end

  it 'returns 0 bytes for "b4"' do
    expect(["1"].pack("b4")).to eq("\x01\x00\x00")
  end

  it 'returns 2 bytes for "B2"' do
    expect(["1"].pack("B2")).to eq("\x80\x00".force_encoding('ASCII-8BIT'))
  end
end
