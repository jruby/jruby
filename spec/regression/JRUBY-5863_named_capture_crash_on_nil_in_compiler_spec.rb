require 'rspec'
require 'stringio'
require 'zlib'

describe 'JRUBY-6559: GZipReader rewind with buffered input' do
  it "rewinds properly when the input is buffered internally" do
    zio = StringIO.new
    io = Zlib::GzipWriter.new zio
    io.write 'aaaa'
    io.finish
    zio << "junk" # this is buffered by Zlib
    zio.rewind

    io = Zlib::GzipReader.new(zio)
    expect(io.read).to eq('aaaa')
    io.rewind
    expect(io.read).to eq('aaaa')
  end
end
