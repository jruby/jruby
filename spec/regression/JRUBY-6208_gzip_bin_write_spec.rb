require 'tempfile'
require 'zlib'

describe "JRUBY-6208: GzipWriter creates corrupted gzip stream" do
  it "should not try to convert binary to default_external" do
    t = Tempfile.new(['out', 'gz'])
    gzfile = t.path
    content = (0..255).to_a.pack('c*')
    content.force_encoding("BINARY")
    Zlib::GzipWriter.open(gzfile) do |gz|
      gz.print(content)
    end
    subject = Zlib::GzipReader.open(gzfile) { |gz| gz.read(1024) }
    expect(subject).to eq(content)
  ensure
    t.close! rescue nil
  end
end
