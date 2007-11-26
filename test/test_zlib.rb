require 'test/unit'
require 'zlib'

class TestZlib < Test::Unit::TestCase
  def teardown;  File.unlink @filename if @filename; end

  def test_inflate_deflate
    s = "test comression string"
    [Zlib::NO_COMPRESSION, Zlib::BEST_SPEED, Zlib::BEST_COMPRESSION, Zlib::DEFAULT_COMPRESSION].each do |level|
      assert_equal(s, Zlib::Inflate.inflate(Zlib::Deflate.deflate(s, level)))
    end
  end
  
  # Zlib::Inflate uses org.jruby.util.ZlibInflate for low-level decompression logic, which is built around
  # java.util.zip.Inflater.
  #
  # Inflater, and hence ZlibInflate, can run in one of two basic modes:
  #
  # - "wrapped" IO stream: when working with an input stream that will contain
  #   Zlib headers and checksum
  # - "unwrapped" IO stream: input streams that do not contain Zlib headers
  #   and checksum
  #
  # This tests whether an instance of Zlib::Inflate correctly reports its completion status
  # after decompressing the full contents of an unwrapped input stream.
  #
  def test_inflate_should_be_finished_after_decompressing_full_unwrapped_stream
      require 'base64'
      require 'stringio'
      
      actual = "test compression string\n" * 5

      # This is a base64-encoded representation of a zipped text file.
      # This is not the ZIP archive itself; rather, it is the compressed representation
      # of a deflated file inside the archive.
      data = "K0ktLlFIzs8tKEotLs7Mz1MoLinKzEvnKqGxOABQSwECFwMUAAIACAAxKng3dpOMHR0AAAB4AAAACAANAAAAAAABAAAApIEAAAAAdGVzdC50eHRVVAUAAz76R0dVeAAAUEsFBgAAAAABAAEAQwAAAFgAAAAAAA=="
              
      inflater = Zlib::Inflate.new(-Zlib::MAX_WBITS)
      
      stream = StringIO.new(Base64.decode64(data))
      assert_equal(actual, inflater.inflate(stream.read(nil, '')), "Unexpected result of decompression.")
      assert(inflater.finished?, "Inflater should be finished after inflating all input.")
  end

  def test_gzip_reader_writer
    @filename = "____temp_zlib_file";
    Zlib::GzipWriter.open(@filename) { |z| z.puts 'HEH' }
    Zlib::GzipReader.open(@filename) { |z| assert_equal("HEH\n", z.gets) }

    z = Zlib::GzipWriter.open(@filename)
    z.puts 'HOH'
    z.puts 'foo|bar'
    z.close

    z = Zlib::GzipReader.open(@filename)
    assert_equal("HOH\n", z.gets)
    assert_equal("foo|", z.gets("|"))
    assert_equal("bar\n", z.gets)
    z.close
  end
  
  def test_native_exception_from_zlib_on_broken_header
    require 'stringio'
    corrupt = StringIO.new
    corrupt.write('borkborkbork')
    begin
      Zlib::GzipReader.new(corrupt)
      flunk()
    rescue Zlib::GzipReader::Error
    end
  end
end
