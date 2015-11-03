require 'zlib'

class IoLikeObject
  attr_reader :body

  def initialize
    @body = []
  end

  def write(str)
    @body << str
  end
end

describe Zlib::GzipWriter do
  GZIP_MAGIC_1 = 0x1F
  GZIP_MAGIC_2 = 0X8B

  it "doesn't corrupt the output" do
    io_like_object = IoLikeObject.new
    gzip = Zlib::GzipWriter.new(io_like_object)

    gzip.mtime = Time.now
    gzip.write('something')
    gzip.flush

    # save off a copy of the first chunk
    # (NOTE: the shared buffer at the root of this bug also means we need to concat to force a true copy)
    first_chunk = '' + io_like_object.body[0]
    gzip.write('else')
    gzip.flush

    # first chunk should still be intact (specifically: it should not be overwritten with the second)
    first_chunk.should == io_like_object.body[0]

    gzip.close

    # extra sanity check of the gzip "magic bytes"
    io_like_object.body[0].bytes.to_a[0..1].should == [GZIP_MAGIC_1, GZIP_MAGIC_2]
  end
end