require 'tempfile'
require 'zlib'

describe "JRUBY-6208: GzipWriter creates corrupted gzip stream" do
  if RUBY_VERSION >= "1.9.0"
    it "works should not try to convert binary to default_external" do
      t = Tempfile.new(['out', 'gz'])
      gzfile = t.path
      content = "\x80"
      Zlib::GzipWriter.open(gzfile) do |gz|
        gz.print(content)
      end
      subject = Zlib::GzipReader.open(gzfile) { |gz| gz.read }
      subject.should == content
    end
  end
end
