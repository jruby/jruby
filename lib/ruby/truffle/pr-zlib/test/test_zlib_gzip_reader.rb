########################################################################
# test_zlib_gzip_reader.rb
#
# Tests for the GzipReader class.
########################################################################
require 'test-unit'
require 'fileutils'
require 'pr/zlib'

class TC_GzipReader < Test::Unit::TestCase
  def self.startup
    Dir.chdir('test') if File.basename(Dir.pwd) != 'test'
    File.open('test.txt', 'wb'){ |fh| fh.puts 'Test file' }
    system('gzip *.txt')
    @@gz_file = 'test.txt.gz'
  end

  def setup
    @handle = File.open(@@gz_file)
    @gz_reader = Zlib::GzipReader.new(@handle)
  end

  def test_constructor_expected_errors
    assert_raise(ArgumentError){ Zlib::GzipReader.new }
    assert_raise(NoMethodError){ Zlib::GzipReader.new(1) }
  end

  def test_lineno_get_basic
    assert_respond_to(@gz_reader, :lineno)
    assert_nothing_raised{ @gz_reader.lineno }
  end

  def test_lineno_get
    assert_kind_of(Fixnum, @gz_reader.lineno)
    assert_equal(0, @gz_reader.lineno)
  end

  def test_lineno_set_basic
    assert_respond_to(@gz_reader, :lineno=)
    assert_nothing_raised{ @gz_reader.lineno = 0 }
  end

  def test_lineno_set
    assert_kind_of(Fixnum, @gz_reader.lineno = 0)
    assert_equal(0, @gz_reader.lineno = 0)
  end

  def test_eof_basic
    assert_respond_to(@gz_reader, :eof)
    assert_nothing_raised{ @gz_reader.eof }
  end

  def test_pos_basic
    assert_respond_to(@gz_reader, :pos)
    assert_nothing_raised{ @gz_reader.pos }
  end

  def test_pos
    assert_kind_of(Fixnum, @gz_reader.pos)
  end

  def test_rewind_basic
    assert_respond_to(@gz_reader, :rewind)
    assert_nothing_raised{ @gz_reader.rewind }
  end

  def test_rewind
    assert_equal(0, @gz_reader.rewind)
  end

  def test_unused_basic
    assert_respond_to(@gz_reader, :unused)
    assert_nothing_raised{ @gz_reader.unused }
  end

  def test_unused
    assert_nil(@gz_reader.unused)
  end

  def test_read_basic
    assert_respond_to(@gz_reader, :read)
    assert_nothing_raised{ @gz_reader.read }
  end

  def test_read
    assert_equal("Test file\n", @gz_reader.read)
  end

  def test_read_with_length
    assert_equal("Test", @gz_reader.read(4))
  end

  def test_read_expected_errors
    assert_raise(ArgumentError){ @gz_reader.read(-1) }
  end

  def test_getc_basic
    assert_respond_to(@gz_reader, :getc)
    assert_nothing_raised{ @gz_reader.getc }
  end

  def test_getc
    expected = RUBY_VERSION.to_f >= 1.9 ? 'T' : 84
    assert_equal(expected, @gz_reader.getc)
  end

  def test_readchar_basic
    assert_respond_to(@gz_reader, :readchar)
    assert_nothing_raised{ @gz_reader.readchar }
  end

  def test_readchar
    expected = RUBY_VERSION.to_f >= 1.9 ? 'T' : 84
    assert_equal(expected, @gz_reader.readchar)
  end

  def test_each_byte_basic
    assert_respond_to(@gz_reader, :each_byte)
    assert_nothing_raised{ @gz_reader.each_byte{} }
  end

  def test_each_byte
    assert_nil(@gz_reader.each_byte{})
  end

  def test_ungetc_basic
    assert_respond_to(@gz_reader, :ungetc)
    assert_nothing_raised{ @gz_reader.ungetc(99) }
  end

  def test_ungetc
    assert_nil(@gz_reader.ungetc(99))
  end

  def test_gets_basic
    assert_respond_to(@gz_reader, :gets)
    assert_nothing_raised{ @gz_reader.gets }
  end

  def test_gets
    assert_equal("Test file\n", @gz_reader.gets)
    omit('Skipping $_ test')
    assert_equal("Test file\n", $_)
  end

  def test_readline_basic
    assert_respond_to(@gz_reader, :readline)
    assert_nothing_raised{ @gz_reader.readline }
  end

  def test_readline
    assert_equal("Test file\n", @gz_reader.readline)
  end

  def test_each_basic
    assert_respond_to(@gz_reader, :each)
    assert_nothing_raised{ @gz_reader.each{} }
  end

  def test_each
    assert_not_nil(@gz_reader.each{})
  end

  def test_readlines_basic
    assert_respond_to(@gz_reader, :readlines)
    assert_nothing_raised{ @gz_reader.readlines }
  end

  def test_readlines
    assert_equal(["Test file\n"], @gz_reader.readlines)
  end

  def teardown
    @handle.close if @handle && !@handle.closed?
    @handle = nil
    @gz_reader = nil
  end

  def self.shutdown
    File.delete(@@gz_file) if File.exist?(@@gz_file)
    @@gz_file = nil
  end
end
