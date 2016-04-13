########################################################################
# test_rbzlib.rb
#
# Test case for the Rbzlib module.
########################################################################
require 'pr/rbzlib'
require 'test-unit'

class TC_Rbzlib < Test::Unit::TestCase
  include Rbzlib

  def setup
    @fixnum = 7
    @buffer = 0.chr * 16
  end

  def test_version
    assert_equal('1.2.3', ZLIB_VERSION)
  end

  def test_fixnum_ord
    assert_respond_to(7, :ord)
    assert_equal(7, 7.ord)
  end

  def test_misc_constants
    assert_equal(9, MAX_MEM_LEVEL)
    assert_equal(8, DEF_MEM_LEVEL)
    assert_equal(15, MAX_WBITS)
    assert_equal(MAX_WBITS, DEF_WBITS)
    assert_equal(0, STORED_BLOCK)
    assert_equal(1, STATIC_TREES)
    assert_equal(2, DYN_TREES)
    assert_equal(3, MIN_MATCH)
    assert_equal(258, MAX_MATCH)
    assert_equal(0x20, PRESET_DICT)
    assert_equal(65521, BASE)
    assert_equal(5552, NMAX)
    assert_equal(0, OS_CODE)
    assert_equal(1, SEEK_CUR)
    assert_equal(2, SEEK_END)
  end

  def test_sync_contants
    assert_equal(0, Z_NO_FLUSH)
    assert_equal(1, Z_PARTIAL_FLUSH)
    assert_equal(2, Z_SYNC_FLUSH)
    assert_equal(3, Z_FULL_FLUSH)
    assert_equal(4, Z_FINISH)
    assert_equal(5, Z_BLOCK)
  end

  def test_stream_constants
    assert_equal(0, Z_OK)
    assert_equal(1, Z_STREAM_END)
    assert_equal(2, Z_NEED_DICT)
    assert_equal(-1, Z_ERRNO)
    assert_equal(-1, Z_EOF)
    assert_equal(-2, Z_STREAM_ERROR)
    assert_equal(-3, Z_DATA_ERROR)
    assert_equal(-4, Z_MEM_ERROR)
    assert_equal(-5, Z_BUF_ERROR)
    assert_equal(-6, Z_VERSION_ERROR)
    assert_equal(16384, Z_BUFSIZE)
  end

  def test_compression_constants
    assert_equal(0, Z_NO_COMPRESSION)
    assert_equal(1, Z_BEST_SPEED)
    assert_equal(9, Z_BEST_COMPRESSION)
    assert_equal(-1, Z_DEFAULT_COMPRESSION)
    assert_equal(8, Z_DEFLATED)
  end

  def test_encoding_constants
    assert_equal(1, Z_FILTERED)
    assert_equal(2, Z_HUFFMAN_ONLY)
    assert_equal(3, Z_RLE)
    assert_equal(4, Z_FIXED)
    assert_equal(0, Z_DEFAULT_STRATEGY)
  end

  def test_crc_constants
    assert_equal(0x01, ASCII_FLAG)
    assert_equal(0x02, HEAD_CRC)
    assert_equal(0x04, EXTRA_FIELD)
    assert_equal(0x08, ORIG_NAME)
    assert_equal(0x10, COMMENT_)
    assert_equal(0xE0, RESERVED)
  end

  def test_zError
    assert_respond_to(self, :zError)
    assert_equal('stream end', self.zError(Z_STREAM_END))
  end

  def test_zlibVersion
    assert_respond_to(self, :zlibVersion)
    assert_equal(ZLIB_VERSION, self.zlibVersion)
  end

  def test_z_error
    assert_respond_to(self, :z_error)
    assert_raise(RuntimeError){ self.z_error('hello') }
  end

  def test_adler32
    assert_respond_to(Rbzlib, :adler32)
    assert_equal(1, Rbzlib.adler32(32, nil))
    assert_equal(0, Rbzlib.adler32(0, @buffer))
    assert_equal(1048577, Rbzlib.adler32(1, @buffer))
    assert_equal(10485770, Rbzlib.adler32(10, @buffer))
    assert_equal(33554464, Rbzlib.adler32(32, @buffer))
  end

  def test_adler32_expected_errors
    assert_raise(ArgumentError){ Rbzlib.adler32 }
    assert_raise(ArgumentError){ Rbzlib.adler32('test') }
  end

  def test_get_crc_table
    assert_respond_to(Rbzlib, :get_crc_table)
  end

  def test_gz_open
    assert_respond_to(Rbzlib, :gz_open)
  end

  def teardown
    @fixnum = 0
    @buffer = nil
  end
end
