########################################################################
# test_zlib.rb
#
# Test case for the Zlib module.
########################################################################
require 'test-unit'
require 'pr/zlib'

class TC_Zlib < Test::Unit::TestCase
   def self.startup
   end

   def setup
      @zstream_funcs = Zlib::ZStreamFuncs.new
   end

   def test_ruby_zlib_version
      assert_equal('0.6.0', Zlib::VERSION)
      assert_equal('0.6.0', Zlib::RUBY_ZLIB_VERSION)
   end

   def test_zlib_version
      assert_equal('1.2.3', Zlib::ZLIB_VERSION)
      assert_equal('1.0.2', Zlib::PR_ZLIB_VERSION)
   end

   def test_zlib_included_constants
      assert_not_nil(Zlib::BINARY)
      assert_not_nil(Zlib::ASCII)
      assert_not_nil(Zlib::UNKNOWN)
   end

   def test_zlib_included_compression_constants
      assert_not_nil(Zlib::NO_COMPRESSION)
      assert_not_nil(Zlib::BEST_SPEED)
      assert_not_nil(Zlib::BEST_COMPRESSION)
      assert_not_nil(Zlib::DEFAULT_COMPRESSION)
   end

   def test_zlib_included_encoding_constants
      assert_not_nil(Zlib::FILTERED)
      assert_not_nil(Zlib::HUFFMAN_ONLY)
      assert_not_nil(Zlib::DEFAULT_STRATEGY)
      assert_not_nil(Zlib::MAX_WBITS)
      assert_not_nil(Zlib::DEF_MEM_LEVEL)
      assert_not_nil(Zlib::MAX_MEM_LEVEL)
      assert_not_nil(Zlib::NO_FLUSH)
      assert_not_nil(Zlib::SYNC_FLUSH)
      assert_not_nil(Zlib::FULL_FLUSH)
      assert_not_nil(Zlib::FINISH)
   end

   def test_zlib_os_constants
      assert_equal(0x00, Zlib::OS_MSDOS)
      assert_equal(0x01, Zlib::OS_AMIGA)
      assert_equal(0x02, Zlib::OS_VMS)
      assert_equal(0x03, Zlib::OS_UNIX)
      assert_equal(0x05, Zlib::OS_ATARI)
      assert_equal(0x06, Zlib::OS_OS2)
      assert_equal(0x07, Zlib::OS_MACOS)
      assert_equal(0x0a, Zlib::OS_TOPS20)
      assert_equal(0x0b, Zlib::OS_WIN32)
   end

   def test_zlib_zstream_flag_constants
      assert_equal(0x1, Zlib::ZSTREAM_FLAG_READY)
      assert_equal(0x2, Zlib::ZSTREAM_FLAG_IN_STREAM)
      assert_equal(0x4, Zlib::ZSTREAM_FLAG_FINISHED)
      assert_equal(0x8, Zlib::ZSTREAM_FLAG_CLOSING)
      assert_equal(0x10, Zlib::ZSTREAM_FLAG_UNUSED)
   end

   def test_zlib_zstream_buffer_constants
      assert_equal(1024, Zlib::ZSTREAM_INITIAL_BUFSIZE)
      assert_equal(16384, Zlib::ZSTREAM_AVAIL_OUT_STEP_MAX)
      assert_equal(2048, Zlib::ZSTREAM_AVAIL_OUT_STEP_MIN)
   end

   def test_zlib_version_module_function
      assert_respond_to(Zlib, :zlib_version)
   end

   def test_adler32_module_function_basic
      assert_respond_to(Zlib, :adler32)
      assert_nothing_raised{ Zlib.adler32 }
   end

   def test_adler32_module_function
      assert_equal(1, Zlib.adler32)
      assert_equal(73204161, Zlib.adler32('test'))
      assert_equal(1, Zlib.adler32(nil, 3))
      assert_equal(73728451, Zlib.adler32('test', 3))
   end

  def test_adler32_module_function_expected_errors
    assert_raise(RangeError){ Zlib.adler32('test', 2**128) }
  end

   def test_crc32_module_function_basic
      assert_respond_to(Zlib, :crc32)
      assert_nothing_raised{ Zlib.crc32 }
   end

   def test_crc32_module_function
      assert_equal(0, Zlib.crc32)
      assert_equal(3632233996, Zlib.crc32('test'))
      assert_equal(0, Zlib.crc32(nil, 3))
      assert_equal(3402289634, Zlib.crc32('test', 3))
   end

  def test_crc32_module_function_expected_errors
    assert_raise(RangeError){ Zlib.crc32('test', 2**128) }
  end

   def test_crc_table_module_function_basic
      assert_respond_to(Zlib, :crc_table)
      assert_nothing_raised{ Zlib.crc_table }
      assert_kind_of(Array, Zlib.crc_table)
   end

   def test_zstream_funcs_struct
      assert_kind_of(Zlib::ZStreamFuncs, @zstream_funcs)
      assert_respond_to(@zstream_funcs, :reset)
      assert_respond_to(@zstream_funcs, :end)
      assert_respond_to(@zstream_funcs, :run)
   end

   def test_error_class
      assert_not_nil(Zlib::Error)
      assert_kind_of(StandardError, Zlib::Error.new)
   end

   def test_stream_end_class
      assert_not_nil(Zlib::StreamEnd)
      assert_kind_of(Zlib::Error, Zlib::StreamEnd.new)
   end

   def test_need_dict_class
      assert_kind_of(Zlib::Error, Zlib::NeedDict.new)
   end

   def test_data_error_class
      assert_kind_of(Zlib::Error, Zlib::DataError.new)
   end

   def test_stream_error_class
      assert_kind_of(Zlib::Error, Zlib::StreamError.new)
   end

   def test_mem_error_class
      assert_kind_of(Zlib::Error, Zlib::MemError.new)
   end

   def test_buf_error_class
      assert_kind_of(Zlib::Error, Zlib::BufError.new)
   end

   def test_version_error_class
      assert_kind_of(Zlib::Error, Zlib::VersionError.new)
   end

   def teardown
      @zstream_funcs = nil
   end

   def self.shutdown
   end
end
