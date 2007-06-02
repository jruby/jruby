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
