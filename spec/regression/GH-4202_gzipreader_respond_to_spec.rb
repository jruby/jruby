require 'tempfile'
require 'zlib'

describe "A GzipWriter instance" do
  before do
    @tempfile = Tempfile.new("GH-4202")
  end

  after do
    @tempfile.close! rescue nil
  end

  it "supports all respond_to? arities" do
    gzw = Zlib::GzipWriter.open(@tempfile.path)
    expect(gzw.respond_to?(:path)).to be true
    expect(gzw.respond_to?(:path, false)).to be true
  end
end

describe "A GzipReader instance" do
  before do
    @tempfile = Tempfile.new("GH-4202")
  end

  after do
    @tempfile.close! rescue nil
  end

  it "supports all respond_to? arities" do
    gzw = Zlib::GzipWriter.open(@tempfile.path)
    gzw << "content"
    gzw.close
    gzr = Zlib::GzipReader.open(@tempfile.path)
    expect(gzr.respond_to?(:path)).to be true
    expect(gzr.respond_to?(:path, false)).to be true
  end
end
