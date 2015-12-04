require 'test-unit'
require 'pr/zlib'

class TC_Zlib_Zstream < Test::Unit::TestCase
   def self.startup
   end

   def setup
      @zstream = Zlib::ZStream.new
      @zfunc   = Zlib::ZStreamFuncs.new
      @zstream.zstream_init(@zfunc)
      @src = Array.new(128, 0.chr)
   end

   def test_flags
      assert_respond_to(@zstream, :flags)
      assert_respond_to(@zstream, :flags=)
   end

   def test_buf
      assert_respond_to(@zstream, :buf)
      assert_respond_to(@zstream, :buf=)
   end

   def test_input
      assert_respond_to(@zstream, :input)
      assert_respond_to(@zstream, :input=)
   end

   def test_stream
      assert_respond_to(@zstream, :stream)
      assert_respond_to(@zstream, :stream=)
   end

   def test_func
      assert_respond_to(@zstream, :func)
      assert_respond_to(@zstream, :func=)
   end

   def test_raise_zlib_error_basic
      assert_respond_to(@zstream, :raise_zlib_error)
   end

   def test_raise_zlib_error_stream_end
      assert_raise(Zlib::StreamEnd){ @zstream.raise_zlib_error(Z_STREAM_END, nil) }
      assert_raise_message('stream end'){ @zstream.raise_zlib_error(Z_STREAM_END, nil) }
   end

   def test_raise_zlib_error_need_dict
      assert_raise(Zlib::NeedDict){ @zstream.raise_zlib_error(Z_NEED_DICT, nil) }
      assert_raise_message('need dictionary'){ @zstream.raise_zlib_error(Z_NEED_DICT, nil) }
   end

   def test_raise_zlib_error_stream_error
      assert_raise(Zlib::StreamError){ @zstream.raise_zlib_error(Z_STREAM_ERROR, nil) }
      assert_raise_message('stream error'){ @zstream.raise_zlib_error(Z_STREAM_ERROR, nil) }
   end

   def test_raise_zlib_error_data_error
      assert_raise(Zlib::DataError){ @zstream.raise_zlib_error(Z_DATA_ERROR, nil) }
      assert_raise_message('data error'){ @zstream.raise_zlib_error(Z_DATA_ERROR, nil) }
   end

   def test_raise_zlib_error_buf_error
      assert_raise(Zlib::BufError){ @zstream.raise_zlib_error(Z_BUF_ERROR, nil) }
      assert_raise_message('buffer error'){ @zstream.raise_zlib_error(Z_BUF_ERROR, nil) }
   end

   def test_raise_zlib_error_version_error
      assert_raise(Zlib::VersionError){ @zstream.raise_zlib_error(Z_VERSION_ERROR, nil) }
      assert_raise_message('incompatible version'){ @zstream.raise_zlib_error(Z_VERSION_ERROR, nil) }
   end

   def test_raise_zlib_error_mem_error
      assert_raise(Zlib::MemError){ @zstream.raise_zlib_error(Z_MEM_ERROR, nil) }
      assert_raise_message('insufficient memory'){ @zstream.raise_zlib_error(Z_MEM_ERROR, nil) }
   end

   def test_raise_zlib_error_errno
      assert_raise(SystemCallError){ @zstream.raise_zlib_error(Z_ERRNO, nil) }
      assert_raise_message('unknown error - file error'){ @zstream.raise_zlib_error(Z_ERRNO, nil) }
   end

   def test_raise_zlib_error_unknown
      #notify('I think there might be a problem here - need to investigate')
      assert_raise(Zlib::Error){ @zstream.raise_zlib_error(999, nil) }
      assert_raise_message('unknown error - file error'){ @zstream.raise_zlib_error(Z_ERRNO, nil) }
   end

   def test_zstream_expand_buffer_basic
      assert_respond_to(@zstream, :zstream_expand_buffer)
      assert_nothing_raised{ @zstream.zstream_expand_buffer }
   end

   # @zstream.buf set after call
   def test_zstream_expand_buffer
      assert_nil(@zstream.buf)
      assert_nil(@zstream.zstream_expand_buffer)
      assert_kind_of(Bytef_str, @zstream.buf)
   end

   def test_zstream_expand_buffer_expected_errors
      #notify("Modify zstream_expand_buffer method to explicitly handle a nil @stream")
      assert_raise(ArgumentError){ @zstream.zstream_expand_buffer(1) }
   end

   def test_zstream_append_buffer_basic
      assert_respond_to(@zstream, :zstream_append_buffer)
      assert_nothing_raised{ @zstream.zstream_append_buffer(@src, @src.length) }
   end

   # @zstream.buf set after call
   def test_zstream_append_buffer
      assert_nil(@zstream.buf)
      assert_nil(@zstream.zstream_append_buffer(@src, @src.length))
      assert_kind_of(Bytef_arr, @zstream.buf)
   end

   def test_zstream_append_buffer_expected_errors
      assert_raise(ArgumentError){ @zstream.zstream_append_buffer }
   end

   def test_zstream_detach_buffer_basic
      assert_respond_to(@zstream, :zstream_detach_buffer)
      assert_nothing_raised{ @zstream.zstream_detach_buffer }
   end

   def test_zstream_detach_buffer
      assert_kind_of(String, @zstream.zstream_detach_buffer)
      assert_not_nil(@zstream.buf)
   end

   def test_zstream_shift_buffer_basic
      @zstream.buf = Bytef.new(0.chr * Zlib::ZSTREAM_INITIAL_BUFSIZE)
      assert_respond_to(@zstream, :zstream_shift_buffer)
      assert_nothing_raised{ @zstream.zstream_shift_buffer(1) }
   end

   def teardown
      @zstream = nil
      @src = nil
   end

   def self.shutdown
   end
end
