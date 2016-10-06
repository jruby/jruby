require 'tempfile'
require 'zlib'

describe "A GzipWriter instance" do
  before do
    @tempfile = Tempfile.new("GH-4202")
  end

  it "supports all respond_to? arities" do
    gzw = Zlib::GzipWriter.open(@tempfile.path)
    gzw.respond_to?(:path).should == true
    gzw.respond_to?(:path, false).should == true
  end
end

describe "A GzipReader instance" do
  it "supports all respond_to? arities" do
    gzw = Zlib::GzipWriter.open(@tempfile.path)
    gzw << "content"
    gzw.close
    gzr = Zlib::GzipReader.open(@tempfile.path)
    gzr.respond_to?(:path).should == true
    gzr.respond_to?(:path, false).should == true
  end
end
