require 'test/unit'
require 'zlib'
require 'stringio'
require 'tempfile'

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
      actual = "test compression string\n" * 5

      # This is a base64-encoded representation of a zipped text file.
      # This is not the ZIP archive itself; rather, it is the compressed representation
      # of a deflated file inside the archive.
      data = "K0ktLlFIzs8tKEotLs7Mz1MoLinKzEvnKqGxOABQSwECFwMUAAIACAAxKng3dpOMHR0AAAB4AAAACAANAAAAAAABAAAApIEAAAAAdGVzdC50eHRVVAUAAz76R0dVeAAAUEsFBgAAAAABAAEAQwAAAFgAAAAAAA=="
              
      inflater = Zlib::Inflate.new(-Zlib::MAX_WBITS)
      
      stream = StringIO.new(data.unpack('m*')[0])
      assert_equal(actual, inflater.inflate(stream.read(nil, '')), "Unexpected result of decompression.")
      assert(inflater.finished?, "Inflater should be finished after inflating all input.")
  end

  def test_gzip_reader_writer
    @filename = "____temp_zlib_file";
    
    Zlib::GzipWriter.open(@filename) { |z| z.puts 'HEH' }
    Zlib::GzipReader.open(@filename) do |z|
      assert_equal("HEH\n", z.gets)
      assert_nil z.getc
      assert z.eof?
    end
    File.unlink(@filename)
    
    Zlib::GzipWriter.open(@filename) { |z| z.write "HEH\n" }
    Zlib::GzipReader.open(@filename) do |z|
      assert_equal("HEH\n", z.gets)
      assert_nil z.getc
      assert z.eof?
    end
    File.unlink(@filename)
    

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
    corrupt = StringIO.new
    corrupt.write('borkborkbork')
    begin
      Zlib::GzipReader.new(corrupt)
      flunk()
    rescue Zlib::GzipReader::Error
    end
  end
  
  def test_deflate_positive_winbits
    d =  Zlib::Deflate.new(Zlib::DEFAULT_COMPRESSION, Zlib::MAX_WBITS)
    d << 'hello'
    res = d.finish
    assert_equal("x\234\313H\315\311\311\a\000\006,\002\025", res)
  end
  
   # negative winbits means no header and no checksum.
   def test_deflate_negative_winbits
     d =  Zlib::Deflate.new(Zlib::DEFAULT_COMPRESSION, -Zlib::MAX_WBITS)
     d << 'hello'
     res = d.finish
     assert_equal("\313H\315\311\311\a\000", res)
  end

  # JRUBY-2228
  def test_gzipreader_descriptor_leak
    @filename = "____temp_zlib_file";
    Zlib::GzipWriter.open(@filename) { |z| z.puts 'HEH' }

    ios = [] # to prevent opened files GC'ed
    assert_nothing_raised() {
      2048.times {
        z = Zlib::GzipReader.open(@filename)
        z.close
        ios << z
      }
    }
  end

  def test_wrap
    content = StringIO.new "", "r+"
    
    Zlib::GzipWriter.wrap(content) do |io|
      io.write "hello\nworld\n"
    end

    content = StringIO.new content.string, "rb"

    gin = Zlib::GzipReader.new(content)
    assert_equal("hello\n", gin.gets)
    assert_equal("world\n", gin.gets)
    assert_nil gin.gets
    assert gin.eof?
    gin.close
  end
  
  def test_each_line_no_block
    @filename = "____temp_zlib_file";
    Zlib::GzipWriter.open(@filename) { |io| io.write "hello\nworld\n" }
    lines = []
    z = Zlib::GzipReader.open(@filename)
    z.each_line do |line|
      lines << line
    end
    z.close
    
    assert_equal(2, lines.size, lines.inspect)
    assert_equal("hello\n", lines.first)
    assert_equal("world\n", lines.last)
  end
  
  def test_each_line_block
    @filename = "____temp_zlib_file";
    Zlib::GzipWriter.open(@filename) { |io| io.write "hello\nworld\n" }
    lines = []
    Zlib::GzipReader.open(@filename) do |z|
      z.each_line do |line|
        lines << line
      end
    end
    assert_equal(2, lines.size, lines.inspect)
  end
  
  def test_empty_line
    @filename = "____temp_zlib_file";
    Zlib::GzipWriter.open(@filename) { |io| io.write "hello\nworld\n\ngoodbye\n" }
    lines = nil
    Zlib::GzipReader.open(@filename) do |z|
      lines = z.readlines
    end
    assert_equal(4, lines.size, lines.inspect)
  end

  def test_inflate_empty_string
    assert_raise(Zlib::BufError) { Zlib::Inflate.inflate('') }
  end

  def test_inflate_bad_data
    assert_raise(Zlib::DataError) { Zlib::Inflate.inflate('        ')}
  end

  def test_inflate_finish
    z = Zlib::Inflate.new
    z << Zlib::Deflate.deflate('foo')
    assert_equal('foo', z.finish)

    assert(z.finished?)
    assert(!z.closed?)
    assert(!z.ended?)
  end

  def test_inflate_close
    z = Zlib::Inflate.new
    z << Zlib::Deflate.deflate('foo')
    z.close

    assert (z.closed?)
    assert (z.ended?)

    assert_raise(Zlib::Error) { z.finish }
    assert_raise(Zlib::Error) { z.finished? }
  end

  def test_inflate_end
    z = Zlib::Inflate.new
    z << Zlib::Deflate.deflate('foo')
    z.end

    assert (z.closed?)
    assert (z.ended?)

    assert_raise(Zlib::Error) { z.finish }
    assert_raise(Zlib::Error) { z.finished? }
  end

  def test_inflate_incomplete
    z = Zlib::Inflate.new
    z << Zlib::Deflate.deflate('foo')[0, 1]
    assert_raise(Zlib::BufError) { z.finish }

    z = Zlib::Inflate.new
    z << Zlib::Deflate.deflate('foo')[0, 1]
    assert_raise(Zlib::BufError) { z.inflate(nil) }
  end

  def test_inflate_finished
    z = Zlib::Inflate.new
    assert(!z.finished?)
    z << Zlib::Deflate.deflate('foo')
    assert(z.finished?)

    # on incomplete data
    z = Zlib::Inflate.new
    z << Zlib::Deflate.deflate('foo')[0, 1]
    assert(!z.finished?)
  end

  def test_inflate_flush_next_in
    z = Zlib::Inflate.new
    z << Zlib::Deflate.deflate('foo')
    assert_equal("", z.flush_next_in)
    assert_equal("foo", z.finish)
    assert_equal("", z.flush_next_in)
  end

  def test_inflate_sync
    z = Zlib::Inflate.new
    z << Zlib::Deflate.deflate('foo')
    assert (!z.sync(''))
    assert (!z.sync_point?)
    assert_equal("foo", z.finish)
    assert (!z.sync(''))
    assert (!z.sync_point?)
  end

  def test_sync
    z = Zlib::Deflate.new
    s = z.deflate("foo" * 1000, Zlib::FULL_FLUSH)
    z.avail_out = 0
    z.params(Zlib::NO_COMPRESSION, Zlib::FILTERED)
    s << z.deflate("bar" * 1000, Zlib::FULL_FLUSH)
    z.avail_out = 0
    z.params(Zlib::BEST_COMPRESSION, Zlib::HUFFMAN_ONLY)
    s << z.deflate("baz" * 1000, Zlib::FINISH)

    z = Zlib::Inflate.new
    assert_raise(Zlib::DataError) { z << "\0" * 100 }
    assert_equal(false, z.sync(""))
    assert_equal(false, z.sync_point?)

    z = Zlib::Inflate.new
    assert_raise(Zlib::DataError) { z << "\0" * 100 + s }
    assert_equal(true, z.sync(""))
    #assert_equal(true, z.sync_point?)
    assert(z.avail_in>0)

    z = Zlib::Inflate.new
    assert_equal(false, z.sync("\0" * 100))
    assert_equal(false, z.sync_point?)

    z = Zlib::Inflate.new
    assert_equal(true, z.sync("\0" * 100 + s))
    #assert_equal(true, z.sync_point?)
  end

  def test_inflate_broken_data_with_sync
    d = Zlib::Deflate.new
    i = Zlib::Inflate.new

    foo = d.deflate("foo", Zlib::SYNC_FLUSH)
    noise = "noise"+d.deflate("*", Zlib::SYNC_FLUSH)
    bar = d.deflate("bar", Zlib::SYNC_FLUSH)

    begin
      i << (foo+noise+bar)
    rescue Zlib::DataError
    end

    i.sync(d.finish)

    begin
     i.finish
    rescue Zlib::DataError  # failed in checking checksum because of broken data
    end

    assert_equal("foobar", i.flush_next_out)
  end

  # JRUBY-4502: 1.4 raises native exception at gz.read
  def test_corrupted_data
    zip = "\037\213\b\000,\334\321G\000\005\000\235\005\000$\n\000\000"
    io = StringIO.new(zip)
    # JRuby cannot check corrupted data format at GzipReader.new for now
    # because of different input buffer handling.
    assert_raise(Zlib::DataError) do
      gz = Zlib::GzipReader.new(io)     # CRuby raises here
                                        # if size of input is less that 2048

      gz.read                           # JRuby raises here
    end
  end

  def test_inflate_after_finish
    z = Zlib::Inflate.new
    data = "x\234c`\200\001\000\000\n\000\001"
    unzipped = z.inflate data
    z.finish  # this is a precondition

    out = z.inflate('uncompressed_data')
    out << z.finish
    assert_equal('uncompressed_data', out)
    z << ('uncompressed_data') << nil
    assert_equal('uncompressed_data', z.finish)
  end

  def test_inflate_pass_through
    main_data = "x\234K\313\317\a\000\002\202\001E"
    result = ""
    z = Zlib::Inflate.new
    # add bytes, one by one
    (main_data * 2).each_byte { |d| result << z.inflate(d.chr)}
    assert_equal("foo", result)
    # the first chunk is inflated to its completion,
    # the second chunk is just passed through.
    result << z.finish
    assert_equal("foo" + main_data, result)
  end

  # JRUBY-4503: cruby-zlib does not require 'close'
  def test_gzip_writer_restricted_io
    z = Object.new
    def z.write(arg)
      (@buf ||= []) << arg
    end
    def z.buf
      @buf
    end
    assert_nil z.buf
    Zlib::GzipWriter.wrap(z) { |io| io.write("hello") }
    assert_not_nil z.buf
  end

  # JRUBY-4503: cruby-zlib does not require 'close'
  def test_gzip_reader_restricted_io
    z = Object.new
    def z.read(size)
      @buf ||= TestZlib.create_gzip_stream("hello")
      @buf.slice!(0, size)
    end
    called = false
    Zlib::GzipReader.wrap(z) { |io|
      assert_equal("hello", io.read)
      called = true
    }
    assert(called)
  end

  # JRUBY-4503: trailer CRC check failed
  def test_gzip_reader_trailer_from_buffer
    z = Object.new
    def z.read(size)
      @buf ||= TestZlib.create_gzip_stream("hello")
      # emulate sliced buffer reading
      @buf.slice!(0, 1)
    end
    called = false
    Zlib::GzipReader.wrap(z) { |io|
      assert_equal("hello", io.read)
      called = true
    }
    assert(called)
  end

  def test_gzip_reader_check_corrupted_trailer

    data = TestZlib.create_gzip_stream("hello")

    assert_raise(Zlib::GzipFile::CRCError) do
      _data = data.dup
      _data[_data.size-5] = 'X'  # checksum
      gz = Zlib::GzipReader.new(StringIO.new(_data))
      gz.read
      gz.finish
    end

    assert_raise(Zlib::GzipFile::LengthError) do
      _data = data.dup
      _data[_data.size-4] = 'X'  # length
      gz = Zlib::GzipReader.new(StringIO.new(_data))
      gz.read
      gz.finish
    end

    assert_raise(Zlib::GzipFile::NoFooter) do
      _data = data.dup
      _data = _data.slice!(0, _data.size-1)
      gz = Zlib::GzipReader.new(StringIO.new(_data))
      gz.read
      gz.finish
    end

    assert_raise(Zlib::GzipFile::NoFooter) do
      _data = data.dup
      _data[_data.size-5] = 'X'  # checksum
      _data = _data.slice!(0, _data.size-1)
      gz = Zlib::GzipReader.new(StringIO.new(_data))
      gz.read
      gz.finish
    end
  end

  def self.create_gzip_stream(string)
    s = StringIO.new
    Zlib::GzipWriter.wrap(s) { |io|
      io.write("hello")
    }
    s.string
  end

  def test_dup
    d1 = Zlib::Deflate.new

    data = "foo" * 10 
    d1 << data
    d2 = d1.dup

    d1 << "bar"
    d2 << "goo"

    data1 = d1.finish
    data2 = d2.finish

    i = Zlib::Inflate.new

    assert_equal(data+"bar",
                 i.inflate(data1) + i.finish);

    i.reset
    assert_equal(data+"goo",
                 i.inflate(data2) + i.finish);
  end

  def test_adler32_combine
    one = Zlib.adler32("fo")
    two = Zlib.adler32("o")
    begin
      assert_equal(0x02820145, Zlib.adler32_combine(one, two, 1))
    rescue NotImplementedError
      skip "adler32_combine is not implemented"
    end
  end

  def test_crc32_combine
    one = Zlib.crc32("fo")
    two = Zlib.crc32("o")
    begin
      assert_equal(0x8c736521, Zlib.crc32_combine(one, two, 1))
    rescue NotImplementedError
      skip "crc32_combine is not implemented"
    end
  end

  def test_writer_sync
    marker = "\x00\x00\xff\xff"

    sio = StringIO.new("")

    if RUBY_VERSION >= '1.9.0'
      sio.set_encoding "ASCII-8BIT"
    end

    Zlib::GzipWriter.wrap(sio) { |z|
      z.write 'a'
      z.sync = true
      z.write 'b'           # marker
      z.write 'c'           # marker
      z.sync = false
      z.write 'd'
      z.write 'e'
      assert_equal(false, z.sync);
    }

    data = sio.string

    i = Zlib::Inflate.new(Zlib::MAX_WBITS + 16)

    assert_equal("ab", i.inflate(data.slice!(0, data.index(marker)+4)))
    assert_equal("c", i.inflate(data.slice!(0, data.index(marker)+4)))
    assert_equal("de", i.inflate(data))
  end

  def test_writer_flush
    marker = "\x00\x00\xff\xff"

    sio = StringIO.new("")
    Zlib::GzipWriter.wrap(sio) { |z|
      z.write 'a'
      z.write 'b'           # marker
      z.flush
      z.write 'c'           # marker
      z.flush
      z.write 'd'
      z.write 'e'
      assert_equal(false, z.sync);
    }

    data = sio.string

    if RUBY_VERSION >= '1.9.0'
      #data.index(marker) will return nil
    else
      i = Zlib::Inflate.new(Zlib::MAX_WBITS + 16)
      assert_equal("ab", i.inflate(data.slice!(0, data.index(marker)+4)))
      assert_equal("c", i.inflate(data.slice!(0, data.index(marker)+4)))
      assert_equal("de", i.inflate(data))
    end
  end

  if RUBY_VERSION >= '1.9.0'
  def test_error_input
    t = Tempfile.new("test_zlib_gzip_reader_open")
    t.close
    e = assert_raise(Zlib::GzipFile::Error) {
      Zlib::GzipReader.open(t.path)
    }
    assert_equal("not in gzip format", e.message)
    assert_nil(e.input)
    open(t.path, "wb") {|f| f.write("foo")}
    e = assert_raise(Zlib::GzipFile::Error) {
      Zlib::GzipReader.open(t.path)
    }
    assert_equal("not in gzip format", e.message)
    assert_equal("foo", e.input)
    open(t.path, "wb") {|f| f.write("foobarzothoge")}
    e = assert_raise(Zlib::GzipFile::Error) {
      Zlib::GzipReader.open(t.path)
    }
    assert_equal("not in gzip format", e.message)
    assert_equal("foobarzothoge", e.input)
  end
  end
end

# Test for MAX_WBITS + 16
class TestZlibDeflateGzip < Test::Unit::TestCase
  def test_deflate_gzip
    d = Zlib::Deflate.new(Zlib::DEFAULT_COMPRESSION, Zlib::MAX_WBITS + 16)
    d << "foo"
    s = d.finish
    assert_equal("foo", Zlib::GzipReader.new(StringIO.new(s)).read)
  end

  def test_deflate_gzip_compat
    z = Zlib::Deflate.new(Zlib::DEFAULT_COMPRESSION, Zlib::MAX_WBITS + 16)
    s = z.deflate("foo") + z.finish
    assert_equal("foo", Zlib::Inflate.new(Zlib::MAX_WBITS + 16).inflate(s))
  end

  def test_initialize
    z = Zlib::Deflate.new(8, 15+16)
    s = z.deflate("foo", Zlib::FINISH)
    assert_equal("foo", Zlib::Inflate.new(15+16).inflate(s))

    z = Zlib::Deflate.new(8, 15+16)
    s = z.deflate("foo")
    s << z.deflate(nil, Zlib::FINISH)
    assert_equal("foo", Zlib::Inflate.new(15+16).inflate(s))

    assert_raise(Zlib::StreamError) { Zlib::Deflate.new(10000) }
  end

  def test_addstr
    z = Zlib::Deflate.new(8, 15+16)
    z << "foo"
    s = z.deflate(nil, Zlib::FINISH)
    assert_equal("foo", Zlib::Inflate.new(15+16).inflate(s))
  end

  def test_flush
    z = Zlib::Deflate.new(8, 15+16)
    z << "foo"
    s = z.flush
    z << "bar"
    s << z.flush_next_in
    z << "baz"
    s << z.flush_next_out
    s << z.deflate("qux", Zlib::FINISH)
    assert_equal("foobarbazqux", Zlib::Inflate.new(15+16).inflate(s))
  end

  def test_avail
    z = Zlib::Deflate.new(8, 15+16)
    assert_equal(0, z.avail_in)
    assert_equal(0, z.avail_out)
    z << "foo"
    z.avail_out += 100
    z << "bar"
    s = z.finish
    assert_equal("foobar", Zlib::Inflate.new(15+16).inflate(s))
  end

  def test_total
    z = Zlib::Deflate.new(8, 15+16)
    1000.times { z << "foo" }
    s = z.finish
    assert_equal(3000, z.total_in)
    assert_operator(3000, :>, z.total_out)
    assert_equal("foo" * 1000, Zlib::Inflate.new(15+16).inflate(s))
  end

  def test_data_type
    z = Zlib::Deflate.new(8, 15+16)
    assert([Zlib::ASCII, Zlib::BINARY, Zlib::UNKNOWN].include?(z.data_type))
  end

  def test_adler
    z = Zlib::Deflate.new(8, 15+16)
    z << "foo"
    s = z.finish
    assert_equal(0x8c736521, z.adler)
  end

  def test_finished_p
    z = Zlib::Deflate.new(8, 15+16)
    assert_equal(false, z.finished?)
    z << "foo"
    assert_equal(false, z.finished?)
    s = z.finish
    assert_equal(true, z.finished?)
    z.close
    assert_raise(Zlib::Error) { z.finished? }
  end

  def test_closed_p
    z = Zlib::Deflate.new(8, 15+16)
    assert_equal(false, z.closed?)
    z << "foo"
    assert_equal(false, z.closed?)
    s = z.finish
    assert_equal(false, z.closed?)
    z.close
    assert_equal(true, z.closed?)
  end

  def test_params
    z = Zlib::Deflate.new(8, 15+16)
    z << "foo"
    z.params(Zlib::DEFAULT_COMPRESSION, Zlib::DEFAULT_STRATEGY)
    z << "bar"
    s = z.finish
    assert_equal("foobar", Zlib::Inflate.new(15+16).inflate(s))

    data = ('a'..'z').to_a.join
    z = Zlib::Deflate.new(Zlib::NO_COMPRESSION, Zlib::MAX_WBITS+16,
                          Zlib::DEF_MEM_LEVEL, Zlib::DEFAULT_STRATEGY)
    z << data[0, 10]
    z.params(Zlib::BEST_COMPRESSION, Zlib::DEFAULT_STRATEGY)
    z << data[10 .. -1]
    assert_equal(data, Zlib::Inflate.new(15+16).inflate(z.finish))

    z = Zlib::Deflate.new(8, 15+16)
    s = z.deflate("foo", Zlib::FULL_FLUSH)
    z.avail_out = 0
    z.params(Zlib::NO_COMPRESSION, Zlib::FILTERED)
    s << z.deflate("bar", Zlib::FULL_FLUSH)
    z.avail_out = 0
    z.params(Zlib::BEST_COMPRESSION, Zlib::HUFFMAN_ONLY)
    s << z.deflate("baz", Zlib::FINISH)
    assert_equal("foobarbaz", Zlib::Inflate.new(15+16).inflate(s))

    z = Zlib::Deflate.new(8, 15+16)
    assert_raise(Zlib::StreamError) { z.params(10000, 10000) }
    z.close # without this, outputs `zlib(finalizer): the stream was freed prematurely.'
  end

  def test_reset
    z = Zlib::Deflate.new(Zlib::NO_COMPRESSION, 15+16)
    z << "foo"
    z.reset
    z << "bar"
    s = z.finish
    assert_equal("bar", Zlib::Inflate.new(15+16).inflate(s))
  end

  def test_close
    z = Zlib::Deflate.new(8, 15+16)
    z.close
    assert_raise(Zlib::Error) { z << "foo" }
    assert_raise(Zlib::Error) { z.reset }
  end

  COMPRESS_MSG = '0000000100100011010001010110011110001001101010111100110111101111'
  def test_deflate_no_flush
    d = Zlib::Deflate.new(8, 15+16)
    d.deflate(COMPRESS_MSG, Zlib::SYNC_FLUSH) # for header output
    assert(d.deflate(COMPRESS_MSG, Zlib::NO_FLUSH).empty?)
    assert(!d.finish.empty?)
    d.close
  end

  def test_deflate_sync_flush
    d = Zlib::Deflate.new(8, 15+16)
    assert_nothing_raised do
      d.deflate(COMPRESS_MSG, Zlib::SYNC_FLUSH)
    end
    assert(!d.finish.empty?)
    d.close
  end

  def test_deflate_sync_flush_inflate
    d = Zlib::Deflate.new(8, 15+16)
    i = Zlib::Inflate.new(15+16)
    "a".upto("z") do |c|
        assert_equal(c, i.inflate(d.deflate(c, Zlib::SYNC_FLUSH)))
    end 
    i.inflate(d.finish)
    i.close
    d.close
  end

  def test_deflate_full_flush
    d = Zlib::Deflate.new(8, 15+16)
    assert_nothing_raised do
      d.deflate(COMPRESS_MSG, Zlib::FULL_FLUSH)
    end
    assert(!d.finish.empty?)
    d.close
  end

  def test_deflate_flush_finish
    d = Zlib::Deflate.new(8, 15+16)
    d.deflate("init", Zlib::SYNC_FLUSH) # for flushing header
    assert(!d.deflate(COMPRESS_MSG, Zlib::FINISH).empty?)
    d.close
  end

  def test_deflate_raise_after_finish
    d = Zlib::Deflate.new(8, 15+16)
    d.deflate("init")
    d.finish
    assert_raise(Zlib::StreamError) do
      d.deflate('foo')
    end
    #
    d = Zlib::Deflate.new(8, 15+16)
    d.deflate("init", Zlib::FINISH)
    assert_raise(Zlib::StreamError) do
      d.deflate('foo')
    end
  end
end

# Test for MAX_WBITS + 16
class TestZlibInflateGzip < Test::Unit::TestCase
  def test_inflate_gzip
    Zlib::GzipWriter.wrap(sio = StringIO.new("")) { |gz| gz << "foo" }
    assert_equal("foo", Zlib::Inflate.new(Zlib::MAX_WBITS + 16).inflate(sio.string))
    i = Zlib::Inflate.new(Zlib::MAX_WBITS + 16)
    i << sio.string
    assert_equal("foo", i.finish)
  end

  def test_initialize
    assert_raise(Zlib::StreamError) { Zlib::Inflate.new(-1) }

    z = Zlib::Deflate.new(8, 15+16)
    s = z.deflate("foo") + z.finish
    z = Zlib::Inflate.new(15+16)
    z << s << nil
    assert_equal("foo", z.finish)
  end

  def test_inflate
    z = Zlib::Deflate.new(8, 15+16)
    s = z.deflate("foo") + z.finish
    z = Zlib::Inflate.new(15+16)
    s = z.inflate(s)
    s << z.inflate(nil)
    assert_equal("foo", s)
    z.inflate("foo") # ???
    z << "foo" # ???
  end

  def test_incomplete_finish
    gzip = "\x1f\x8b\x08\x00\x1a\x96\xe0\x4c\x00\x03\xcb\x48\xcd\xc9\xc9\x07\x00\x86\xa6\x10\x36\x05\x00\x00"
    z = Zlib::Inflate.new(Zlib::MAX_WBITS + 16)
    assert_equal("hello", z.inflate(gzip))
    assert_raise(Zlib::BufError) do
      z.finish
    end
  end

  def test_wrong_length
    gzip = "\x1f\x8b\x08\x00\x1a\x96\xe0\x4c\x00\x03\xcb\x48\xcd\xc9\xc9\x07\x00\x86\xa6\x10\x36\x04\x00\x00\x00"
    z = Zlib::Inflate.new(Zlib::MAX_WBITS + 16)
    assert_raise(Zlib::DataError) do
      z.inflate(gzip)
    end
  end

  def test_wrong_length_split_trailer
    gzip = "\x1f\x8b\x08\x00\x1a\x96\xe0\x4c\x00\x03\xcb\x48\xcd\xc9\xc9\x07\x00\x86\xa6\x10\x36\x04\x00\x00"
    z = Zlib::Inflate.new(Zlib::MAX_WBITS + 16)
    assert_equal("hello", z.inflate(gzip))
    assert_raise(Zlib::DataError) do
      z.inflate("\x00")
    end
  end
end

# Test for MAX_WBITS + 32
class TestZlibInflateAuto < Test::Unit::TestCase
  def test_inflate_auto_detection_zip
    s = Zlib::Deflate.deflate("foo")
    assert_equal("foo", Zlib::Inflate.new(Zlib::MAX_WBITS + 32).inflate(s))
    i = Zlib::Inflate.new(Zlib::MAX_WBITS + 32)
    i << s
    assert_equal("foo", i.finish)
  end

  def test_inflate_auto_detection_gzip
    Zlib::GzipWriter.wrap(sio = StringIO.new("")) { |gz| gz << "foo" }
    assert_equal("foo", Zlib::Inflate.new(Zlib::MAX_WBITS + 32).inflate(sio.string))
    i = Zlib::Inflate.new(Zlib::MAX_WBITS + 32)
    i << sio.string
    assert_equal("foo", i.finish)
  end

  def test_corrupted_header
    gz = Zlib::GzipWriter.new(StringIO.new(s = ""))
    gz.orig_name = "X"
    gz.comment = "Y"
    gz.print("foo")
    gz.finish
    # 14: magic(2) + method(1) + flag(1) + mtime(4) + exflag(1) + os(1) + orig_name(2) + comment(2)
    1.upto(14) do |idx|
      assert_raise(Zlib::BufError, idx) do
        z = Zlib::Inflate.new(Zlib::MAX_WBITS + 32)
        p z.inflate(s[0, idx]) + z.finish
      end
    end
  end

  def test_split_header
    gz = Zlib::GzipWriter.new(StringIO.new(s = ""))
    gz.orig_name = "X"
    gz.comment = "Y"
    gz.print("foo")
    gz.finish
    # 14: magic(2) + method(1) + flag(1) + mtime(4) + exflag(1) + os(1) + orig_name(2) + comment(2)
    z = Zlib::Inflate.new(Zlib::MAX_WBITS + 32)
    assert_equal(
      "foo",
      [
        z.inflate(s.slice!(0, 5)),
        z.inflate(s.slice!(0, 5)),
        z.inflate(s.slice!(0, 5)),
        z.inflate(s.slice!(0, 5)),
        z.inflate(s.slice!(0, 5)),
        z.inflate(s.slice!(0, 5))
      ].join + z.finish
    )
  end

  def test_initialize
    assert_raise(Zlib::StreamError) { Zlib::Inflate.new(-1) }

    s = Zlib::Deflate.deflate("foo")
    z = Zlib::Inflate.new(15+32)
    z << s << nil
    assert_equal("foo", z.finish)
  end

  def test_inflate
    s = Zlib::Deflate.deflate("foo")
    z = Zlib::Inflate.new(15+32)
    s = z.inflate(s)
    s << z.inflate(nil)
    assert_equal("foo", s)
    z.inflate("foo") # ???
    z << "foo" # ???
  end

  def test_incomplete_finish
    gzip = "\x1f\x8b\x08\x00\x1a\x96\xe0\x4c\x00\x03\xcb\x48\xcd\xc9\xc9\x07\x00\x86\xa6\x10\x36\x05\x00\x00"
    z = Zlib::Inflate.new(Zlib::MAX_WBITS + 32)
    assert_equal("hello", z.inflate(gzip))
    assert_raise(Zlib::BufError) do
      z.finish
    end
  end

  def test_wrong_length
    gzip = "\x1f\x8b\x08\x00\x1a\x96\xe0\x4c\x00\x03\xcb\x48\xcd\xc9\xc9\x07\x00\x86\xa6\x10\x36\x04\x00\x00\x00"
    z = Zlib::Inflate.new(Zlib::MAX_WBITS + 32)
    assert_raise(Zlib::DataError) do
      z.inflate(gzip)
    end
  end

  def test_dictionary
    dict = "hello"
    str = "hello, hello!"

    d = Zlib::Deflate.new
    d.set_dictionary(dict)
    comp_str = d.deflate(str)
    comp_str << d.finish
    comp_str.size

    i = Zlib::Inflate.new(Zlib::MAX_WBITS + 32)

    begin
      i.inflate(comp_str)
      rescue Zlib::NeedDict
    end
    #i.reset

    i.set_dictionary(dict)
    i << ""
    assert_equal(str, i.finish)
  end

  def test_wrong_length_split_trailer
    gzip = "\x1f\x8b\x08\x00\x1a\x96\xe0\x4c\x00\x03\xcb\x48\xcd\xc9\xc9\x07\x00\x86\xa6\x10\x36\x04\x00\x00"
    z = Zlib::Inflate.new(Zlib::MAX_WBITS + 32)
    assert_equal("hello", z.inflate(gzip))
    assert_raise(Zlib::DataError) do
      z.inflate("\x00")
    end
  end

  def test_deflate_full_flush
    z = Zlib::Deflate.new(8, 15)
    s = z.deflate("f", Zlib::FULL_FLUSH)
    s << z.deflate("b", Zlib::FINISH)
    assert_equal("x\332J\003\000\000\000\377\377K\002\000\0010\000\311", s)
  end
end
