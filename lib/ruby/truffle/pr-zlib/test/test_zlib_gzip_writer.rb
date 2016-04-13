########################################################################
# test_zlib_gzip_writer.rb
#
# Test case for the Zlib::GzipWriter class.
########################################################################
require 'pr/zlib'
require 'test-unit'

class TC_Zlib_GzipWriter < Test::Unit::TestCase
  def self.startup
    @@file = 'gzip_writer_test.gz'
  end

  def setup
    @handle = File.new(@@file, 'w')
    @writer = Zlib::GzipWriter.new(@handle)
    @time   = Time.now
  end

  def test_constructor
    assert_nothing_raised{ @writer = Zlib::GzipWriter.new(@handle) }
    assert_nothing_raised{ @writer = Zlib::GzipWriter.new(@handle, nil) }
    assert_nothing_raised{ @writer = Zlib::GzipWriter.new(@handle, nil, nil) }
  end

  def test_constructor_expected_errors
    assert_raise(ArgumentError){ Zlib::GzipWriter.new }
  end

  def test_level
    assert_respond_to(@writer, :level)
    assert_equal(Z_DEFAULT_COMPRESSION, @writer.level)
  end

  def test_mtime_get_basic
    assert_respond_to(@writer, :mtime)
    assert_nothing_raised{ @writer.mtime }
  end

  def test_mtime_get
    assert_kind_of(Time, @writer.mtime)
    assert_equal(Time.at(0), @writer.mtime)
  end

  def test_mtime_set_basic
    assert_respond_to(@writer, :mtime=)
    assert_nothing_raised{ @writer.mtime = @time }
  end

  def test_mtime_set
    assert_equal(@time, @writer.mtime = @time)
  end

  def test_orig_name_get_basic
    assert_respond_to(@writer, :orig_name)
    assert_nothing_raised{ @writer.orig_name }
  end

  def test_orig_name_get
    assert_nil(@writer.orig_name)
  end

  def test_orig_name_set_basic
    assert_respond_to(@writer, :orig_name=)
    assert_nothing_raised{ @writer.orig_name = 'test' }
  end

  def test_orig_name_set
    assert_equal('test', @writer.orig_name = 'test')
    assert_equal('test', @writer.orig_name)
  end

  def test_comment_get_basic
    assert_respond_to(@writer, :comment)
    assert_nothing_raised{ @writer.comment }
  end

  def test_comment_get
    assert_nil(@writer.comment)
  end

  def test_comment_set_basic
    assert_respond_to(@writer, :comment=)
    assert_nothing_raised{ @writer.comment = 'test' }
  end

  def test_comment_set
    assert_equal('test', @writer.comment = 'test')
    assert_equal('test', @writer.comment)
  end

  def test_pos_basic
    assert_respond_to(@writer, :pos)
    assert_nothing_raised{ @writer.pos }
    assert_kind_of(Fixnum, @writer.pos)
  end

  def test_pos
    assert_equal(0, @writer.pos)
    assert_nothing_raised{ @writer.write('test') }
    assert_equal(4, @writer.pos)
  end

  def test_tell_alias
    assert_alias_method(@writer, :pos, :tell)
  end

  def test_open_basic
    assert_respond_to(Zlib::GzipWriter, :open)
    assert_nothing_raised{ Zlib::GzipWriter.open(@@file){} }
  end

  def test_flush_basic
    assert_respond_to(@writer, :flush)
    assert_nothing_raised{ @writer.flush }
  end

  def test_flush
    assert_equal(@writer, @writer.flush)
  end

  def test_write_basic
    assert_respond_to(@writer, :write)
    assert_nothing_raised{ @writer.write('test') }
  end

  def test_write
    assert_equal(4, @writer.write('test'))
    assert_equal(3, @writer.write(999))
  end

  def test_write_expected_errors
    assert_raise(ArgumentError){ @writer.write }
  end

  def test_putc_basic
    assert_respond_to(@writer, :putc)
    assert_nothing_raised{ @writer.putc(97) }
  end

  def test_putc
    assert_equal(97, @writer.putc(97))
  end

  def test_putc_expected_errors
    assert_raise(ArgumentError){ @writer.putc }
  end

  def test_append_basic
    assert_respond_to(@writer, :<<)
    assert_nothing_raised{ @writer << 'test' }
  end

  def test_append_expected_errors
    assert_raise(ArgumentError){ @writer.send(:<<) }
  end

  def test_printf_basic
    assert_respond_to(@writer, :printf)
    assert_nothing_raised{ @writer.printf('%s', 'test') }
  end

  def test_printf_expected_errors
    assert_raise(ArgumentError){ @writer.printf }
  end

  def test_puts_basic
    assert_respond_to(@writer, :puts)
    assert_nothing_raised{ @writer.puts('test') }
  end

  def test_puts_expected_errors
    assert_raise(ArgumentError){ @writer.puts }
    assert_raise(ArgumentError){ @writer.puts('test1', 'test2') }
  end

  def teardown
    @handle.close if @handle && !@handle.closed?
    @handle = nil
    @time = nil
  end

  def self.shutdown
    File.delete(@@file) if File.exist?(@@file)
  end
end
