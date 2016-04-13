########################################################################
# test_zlib_gzip_file.rb
#
# Test case for the Zlib::GzipFile base class.
########################################################################
require 'pr/zlib'
require 'test-unit'

class TC_Zlib_GzipFile < Test::Unit::TestCase
   def self.startup
   end

   def setup
      @gz_file = Zlib::GzipFile.new
   end

   def test_gzip_file_constants
      assert_equal(Zlib::GzipFile::GZ_MAGIC1, 0x1f)
      assert_equal(Zlib::GzipFile::GZ_MAGIC2, 0x8b)
      assert_equal(Zlib::GzipFile::GZ_METHOD_DEFLATE, 8)
      assert_equal(Zlib::GzipFile::GZ_FLAG_MULTIPART, 0x2)
      assert_equal(Zlib::GzipFile::GZ_FLAG_EXTRA, 0x4)
      assert_equal(Zlib::GzipFile::GZ_FLAG_ORIG_NAME, 0x8)
      assert_equal(Zlib::GzipFile::GZ_FLAG_COMMENT, 0x10)
      assert_equal(Zlib::GzipFile::GZ_FLAG_ENCRYPT, 0x20)
      assert_equal(Zlib::GzipFile::GZ_FLAG_UNKNOWN_MASK, 0xc0)
      assert_equal(Zlib::GzipFile::GZ_EXTRAFLAG_FAST, 0x4)
      assert_equal(Zlib::GzipFile::GZ_EXTRAFLAG_SLOW, 0x2)
   end

   def test_gzfile_is_finished
      assert_respond_to(@gz_file, :GZFILE_IS_FINISHED)
   end

   def test_gzfile_close_basic
      assert_respond_to(@gz_file, :gzfile_close)
   end

   def test_to_io_basic
      assert_respond_to(@gz_file, :to_io)
   end

   def test_crc_basic
      assert_respond_to(@gz_file, :crc)
   end

   def test_mtime_basic
      assert_respond_to(@gz_file, :mtime)
   end

   def test_level_basic
      assert_respond_to(@gz_file, :level)
   end

   def test_os_code_basic
      assert_respond_to(@gz_file, :os_code)
   end

   def test_orig_name_basic
      assert_respond_to(@gz_file, :orig_name)
   end

   def test_comment_basic
      assert_respond_to(@gz_file, :comment)
   end

   def test_close_basic
      assert_respond_to(@gz_file, :close)
   end

   def test_finish_basic
      assert_respond_to(@gz_file, :finish)
   end

   def test_is_closed_basic
      assert_respond_to(@gz_file, :closed?)
   end

   def test_sync_get_basic
      assert_respond_to(@gz_file, :sync)
   end

   def test_sync_set_basic
      assert_respond_to(@gz_file, :sync=)
   end

   def teardown
      @gz_file = nil
   end

   def self.shutdown
   end
end
