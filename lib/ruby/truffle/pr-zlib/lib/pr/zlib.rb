# zlib.rb -- An interface for rbzlib
# Copyright (C) UENO Katsuhiro 2000-2003
#
# Ruby translation by Park Heesob

require_relative 'rbzlib'
include Rbzlib

module Zlib

  RUBY_ZLIB_VERSION = '0.6.0'
  PR_ZLIB_VERSION   = '1.0.2'

  class Error < StandardError
  end

  class StreamEnd < Error
  end

  class NeedDict < Error
  end

  class DataError < Error
  end

  class StreamError < Error
  end

  class MemError < Error
  end

  class BufError < Error
  end

  class VersionError < Error
  end

  VERSION = RUBY_ZLIB_VERSION
  ZLIB_VERSION = ZLIB_VERSION

  BINARY  = Z_BINARY
  ASCII   = Z_ASCII
  UNKNOWN = Z_UNKNOWN

  NO_COMPRESSION      = Z_NO_COMPRESSION
  BEST_SPEED          = Z_BEST_SPEED
  BEST_COMPRESSION    = Z_BEST_COMPRESSION
  DEFAULT_COMPRESSION = Z_DEFAULT_COMPRESSION

  FILTERED         = Z_FILTERED
  HUFFMAN_ONLY     = Z_HUFFMAN_ONLY
  DEFAULT_STRATEGY = Z_DEFAULT_STRATEGY
  MAX_WBITS        = MAX_WBITS
  DEF_MEM_LEVEL    = DEF_MEM_LEVEL
  MAX_MEM_LEVEL    = MAX_MEM_LEVEL
  NO_FLUSH         = Z_NO_FLUSH
  SYNC_FLUSH       = Z_SYNC_FLUSH
  FULL_FLUSH       = Z_FULL_FLUSH
  FINISH           = Z_FINISH

  OS_CODE   = OS_CODE
  OS_MSDOS  = 0x00
  OS_AMIGA  = 0x01
  OS_VMS    = 0x02
  OS_UNIX   = 0x03
  OS_ATARI  = 0x05
  OS_OS2    = 0x06
  OS_MACOS  = 0x07
  OS_TOPS20 = 0x0a
  OS_WIN32  = 0x0b

  ZSTREAM_FLAG_READY     = 0x1
  ZSTREAM_FLAG_IN_STREAM = 0x2
  ZSTREAM_FLAG_FINISHED  = 0x4
  ZSTREAM_FLAG_CLOSING   = 0x8
  ZSTREAM_FLAG_UNUSED    = 0x10

  ZSTREAM_INITIAL_BUFSIZE     =  1024
  ZSTREAM_AVAIL_OUT_STEP_MAX  = 16384
  ZSTREAM_AVAIL_OUT_STEP_MIN  =  2048

  ZStreamFuncs = Struct.new(:reset, :end, :run)
  DeflateFuncs = ZStreamFuncs.new(:deflateReset, :deflateEnd, :deflate)
  InflateFuncs = ZStreamFuncs.new(:inflateReset, :inflateEnd, :inflate)

  class ZStream
    attr_accessor :flags, :buf, :input, :stream, :func

    def raise_zlib_error(err, msg)
      msg = zError(err) if msg.nil? || msg==''

      case err
        when Z_STREAM_END
          raise StreamEnd, msg
        when Z_NEED_DICT
          raise NeedDict, msg
        when Z_STREAM_ERROR
          raise StreamError, msg
        when Z_DATA_ERROR
          raise DataError, msg
        when Z_BUF_ERROR
          raise BufError, msg
        when Z_VERSION_ERROR
          raise VersionError, msg
        when Z_MEM_ERROR
          raise MemError, msg
        when Z_ERRNO
          raise SystemCallError, msg
        else
          raise Error, "unknown zlib error #errend: #msgend"
      end
    end

    def zstream_expand_buffer()
      if @buf.nil?
        @buf = Bytef.new(0.chr * ZSTREAM_INITIAL_BUFSIZE)
        @stream.next_out = Bytef.new(@buf)
        @stream.avail_out = ZSTREAM_INITIAL_BUFSIZE
        return
      end

      if (@buf.length - @buf.offset >= ZSTREAM_AVAIL_OUT_STEP_MAX)
        @stream.avail_out = ZSTREAM_AVAIL_OUT_STEP_MAX
      else
        inc = @buf.offset / 2
        if (inc < ZSTREAM_AVAIL_OUT_STEP_MIN)
            inc = ZSTREAM_AVAIL_OUT_STEP_MIN
        end
        if @buf.length < @buf.offset + inc
          @buf.buffer << 0.chr * (@buf.offset + inc - @buf.length)
        end
        @stream.avail_out = (inc < ZSTREAM_AVAIL_OUT_STEP_MAX) ?
            inc : ZSTREAM_AVAIL_OUT_STEP_MAX
      end
      @stream.next_out = Bytef.new(@buf,@buf.offset)
    end

    def zstream_append_buffer(src, len)
      if @buf.nil?
        @buf = Bytef.new(src[0,len],len)
        @stream.next_out = Bytef.new(@buf)
        @stream.avail_out = 0
        return
      end
      if (@buf.length < @buf.offset + len)
        @buf.buffer << (0.chr * (@buf.offset + len - @buf.length))
        @stream.avail_out = 0
      else
        if (@stream.avail_out >= len)
           @stream.avail_out -= len
        else
           @stream.avail_out = 0
        end
      end
      @buf.buffer[@buf.offset,len] = src[0,len]
      @buf += len
      @stream.next_out = Bytef.new(@buf, @buf.offset)
    end

    def zstream_detach_buffer()
      if @buf.nil?
       dst = ''
      else
       dst = @buf.buffer[0,@buf.offset]
      end

      @buf = Bytef.new(0.chr * ZSTREAM_INITIAL_BUFSIZE)
      @stream.next_out = Bytef.new(@buf)
      @stream.avail_out = ZSTREAM_INITIAL_BUFSIZE
      @buf_filled = 0

      return dst
    end

    def zstream_shift_buffer(len)
      if (@buf.offset <= len)
        return zstream_detach_buffer()
      end

      dst = @buf.buffer[0, len]
      @buf -= len
      @buf.buffer[0,@buf.offset] = @buf.buffer[len,@buf.offset]
      @stream.next_out = Bytef.new(@buf,@buf.offset)
      @stream.avail_out = @buf.length - @buf.offset
      if (@stream.avail_out > ZSTREAM_AVAIL_OUT_STEP_MAX)
       @stream.avail_out = ZSTREAM_AVAIL_OUT_STEP_MAX
      end
      return dst
    end

    def zstream_buffer_ungetc(c)
      if (@buf.nil? || (@buf.length - @buf.offset).zero?)
       zstream_expand_buffer()
      end
      @buf.buffer[0,0] = c.chr
      @buf += 1
      if (@stream.avail_out > 0)
       @stream.next_out+=1
       @stream.avail_out-=1
      end
    end

    def zstream_append_input(src, len)
      return if (len <= 0)
      src = src.current if src.class != String
      if @input.nil?
        @input = src[0,len]
      else
        @input << src[0,len]
      end
    end

    def zstream_discard_input(len)
      if (@input.nil? || @input.length <= len)
        @input = nil
      else
        @input[0,len] = ''
      end
    end

    def zstream_reset_input()
      @input = nil
    end

    def zstream_passthrough_input()
      if @input
       zstream_append_buffer(@input,@input.length)
       @input = nil
      end
    end

    def zstream_detach_input()
      if @input.nil?
       dst = ''
      else
       dst = @input
      end
      @input = nil
      return dst
    end

    def zstream_reset()
      err = send(@func.reset,@stream)
      if (err != Z_OK)
        raise_zlib_error(err, @stream.msg)
      end
      @flags = ZSTREAM_FLAG_READY
      @buf = nil
      @buf_filled = 0
      @stream.next_out = 0
      @stream.avail_out = 0
      zstream_reset_input()
    end

    def zstream_end()
      if (!ZSTREAM_IS_READY())
        warn("attempt to close uninitialized zstream; ignored.")
        return nil
      end
      if (@flags & ZSTREAM_FLAG_IN_STREAM).nonzero?
        warn("attempt to close unfinished zstream; reset forced.")
        zstream_reset()
      end

      zstream_reset_input()
      err = send(@func.end,@stream)
      if (err != Z_OK)
        raise_zlib_error(err, @stream.msg)
      end
      @flags = 0
      return nil
    end

    def zstream_sync(src, len)
      if @input
       @stream.next_in = Bytef.new(@input)
       @stream.avail_in = @input.length
       err = inflateSync(@stream)
       if (err == Z_OK)
           zstream_discard_input(@input.length - @stream.avail_in)
           zstream_append_input(src, len)
           return true
       end
       zstream_reset_input()
       if (err != Z_DATA_ERROR)
           rest = @stream.next_in.buffer[0,@stream.avail_in]
           raise_zlib_error(err, @stream.msg)
       end
      end

      return false if (len <= 0)

      @stream.next_in = src
      @stream.avail_in = len
      err = inflateSync(@stream)
      if (err == Z_OK)
       zstream_append_input(@stream.next_in, @stream.avail_in)
       return true
      end
      if (err != Z_DATA_ERROR)
       rest = @stream.next_in.buffer[0,@stream.avail_in]
       raise_zlib_error(err, @stream.msg)
      end
      return false
    end

    def zstream_init(func)
      @flags = 0
      @buf = nil
      @input = nil
      @stream = Z_stream.new
      @stream.msg = ''
      @stream.next_in = nil
      @stream.avail_in = 0
      @stream.next_out = nil
      @stream.avail_out = 0
      @func = func
    end

    def zstream_run(src, len, flush)
      if(@input.nil? && len==0)
        @stream.next_in = ''
        @stream.avail_in = 0
      else
        zstream_append_input(src,len)
        @stream.next_in = Bytef.new(@input)
        @stream.avail_in = @input.length
        guard = @input
      end
      if(@stream.avail_out.zero?)
        zstream_expand_buffer()
      end

      loop do
        n = @stream.avail_out
        err = send(@func.run,@stream,flush)
        @buf += n - @stream.avail_out
        if(err == Z_STREAM_END)
          @flags &= ~ZSTREAM_FLAG_IN_STREAM
          @flags |= ZSTREAM_FLAG_FINISHED
          break
        end
        if (err != Z_OK)
          if (flush != Z_FINISH && err == Z_BUF_ERROR && @stream.avail_out > 0)
            @flags |= ZSTREAM_FLAG_IN_STREAM
            break
          end
          @input = nil
          if (@stream.avail_in > 0)
            zstream_append_input(@stream.next_in, @stream.avail_in)
          end
          raise_zlib_error(err, @stream.msg)
        end
        if (@stream.avail_out > 0)
          @flags |= ZSTREAM_FLAG_IN_STREAM
          break
        end
        zstream_expand_buffer()
      end

      @input = nil
      if (@stream.avail_in > 0)
        zstream_append_input(@stream.next_in, @stream.avail_in)
        guard = nil
      end
    end

    def ZSTREAM_READY()
      (@flags |= ZSTREAM_FLAG_READY)
    end

    def ZSTREAM_IS_READY()
      !(@flags & ZSTREAM_FLAG_READY).zero?
    end

    def ZSTREAM_IS_FINISHED()
      !(@flags & ZSTREAM_FLAG_FINISHED).zero?
    end

    def ZSTREAM_IS_CLOSING()
      !(@flags & ZSTREAM_FLAG_CLOSING).zero?
    end

  end

  class ZStream
    @@final = proc do |z|
      proc do
        if z && z.ZSTREAM_IS_READY()
          err = send(z.func.end, z.stream)
          if (err == Z_STREAM_ERROR)
            warn("the stream state was inconsistent.")
          end
          if (err == Z_DATA_ERROR)
            warn("the stream was freed prematurely.")
          end
        end
      end
    end

    attr_reader :z

    def avail_out()
      @z.stream.avail_out
    end

    def avail_out=(size)
      if @z.buf.nil?
        @z.buf = Bytef.new(0.chr * size)
        @z.stream.next_out = Bytef.new(@z.buf)
        @z.stream.avail_out = size
      elsif @z.stream.avail_out != size
        if @z.buf.offset + size > @z.buf.length
          @z.buf.buffer << 0.chr * (@z.buf.offset + size - @z.buf.length)
        end
        @z.stream.next_out = Bytef.new(@z.buf,@z.buf.offset)
        @z.stream.avail_out = size
      end
    end

    def avail_in
      @z.input.nil? ? 0 : @z.input.length
    end

    def total_in
      raise GzipFile::Error,"closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @z.stream.total_in
    end

    def total_out
      raise GzipFile::Error,"closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @z.stream.total_out
    end

    def data_type
      @z.stream.data_type
    end

    def adler
      @z.stream.adler
    end

    def finished?
      @z.ZSTREAM_IS_FINISHED()
    end
    alias stream_end? :finished?

    def closed?
      @z.ZSTREAM_IS_READY()
    end
    alias ended? :closed?

    def close()
      if (!@z.ZSTREAM_IS_READY())
         warn("attempt to close uninitialized zstream ignored.")
         return nil
      end
      if (@z.flags & ZSTREAM_FLAG_IN_STREAM).nonzero?
         warn("attempt to close unfinished zstream reset forced.")
         @z.input = nil
      end

      @z.input = nil
      err = send(@z.func.end,@z.stream)
      if (err != Z_OK)
         raise_zlib_error(err, @z.stream.msg)
      end
      @z.flags = 0
    end
    alias end :close

    def reset()
      err = send(@z.func.reset,@z.stream)
      if err != Z_OK
        raise_zlib_error(err, @z.stream.msg)
      end
      @z.flags = ZSTREAM_FLAG_READY
      @z.buf = nil
      @z.stream.next_out = 0
      @z.stream.avail_out = 0
      @z.input = nil
    end

    def finish()
      @z.zstream_run("", 0, Z_FINISH)
      @z.zstream_detach_buffer()
    end

    def flush_next_in
      @z.zstream_detach_input
    end

    def flush_next_out
      @z.zstream_detach_buffer
    end

    def initialize
      @z = nil
      ObjectSpace.define_finalizer self, @@final.call(@z)
    end

  end

  class Deflate < ZStream

    def self.deflate_run(src)
      @z.zstream_run(src,src.length,Z_FINISH)
      return @z.zstream_detach_buffer()
    end

    def self.deflate(src,level=Z_DEFAULT_COMPRESSION)
      @z = ZStream.new
      @z.zstream_init(DeflateFuncs)
      err = deflateInit(@z.stream, level)
      if (err != Z_OK)
       raise_zlib_error(err, @z.stream.msg)
      end
      @z.ZSTREAM_READY()

      begin
        dst = deflate_run(src)
      ensure
        @z.zstream_end()
      end
      dst
    end

    def initialize(level=Z_DEFAULT_COMPRESSION,wbits=MAX_WBITS,memlevel=DEF_MEM_LEVEL,strategy=Z_DEFAULT_STRATEGY)
      @z = ZStream.new
      @z.zstream_init(DeflateFuncs)
      err = deflateInit2(@z.stream,level,Z_DEFLATED,wbits,memlevel,strategy)
      if (err != Z_OK)
       raise_zlib_error(err, @z.stream.msg)
      end
      @z.ZSTREAM_READY()
    end

    def initialize_copy(orig)
      z1 = @z
      z2 = orig.z
      err = deflateCopy(z1.stream, z2.stream)
      if (err != Z_OK)
        raise_zlib_error(err, 0)
      end
      z1.flags = z2.flags
    end

    def do_deflate(src,flush)
      if src.nil?
        @z.zstream_run('',0,Z_FINISH)
        return
      end
      if (flush != Z_NO_FLUSH || (src && src.length>0))
        @z.zstream_run(src,src.length,flush)
      end
    end
    private :do_deflate

    def deflate(src,flush=Z_NO_FLUSH)
      do_deflate(src,flush)
      @z.zstream_detach_buffer
    end

    def <<(src)
      do_deflate(src,Z_NO_FLUSH)
      self
    end

    def flush(v_flush)
      if(v_flush != Z_NO_FLUSH)
        @z.zstream_run("", 0, flush)
      end
      @z.zstream_detach_buffer()
    end

    def params(level=Z_DEFAULT_COMPRESSION,strategy=Z_DEFAULT_STRATEGY)
      err = deflateParams(@z.stream, level, strategy)
      while (err == Z_BUF_ERROR)
        warn("deflateParams() returned Z_BUF_ERROR")
         @z.zstream_expand_buffer()
         err = deflateParams(@z.stream, level, strategy)
      end
      if (err != Z_OK)
         raise_zlib_error(err, @z.stream.msg)
      end

      nil
    end

    def set_dictionary(dic)
      err = deflateSetDictionary(@z.stream,dic,dic.length)
      if (err != Z_OK)
        raise_zlib_error(err, @z.stream.msg)
      end
    end

  end

  class Inflate < ZStream

    def self.inflate_run(src)
      @z.zstream_run(src,src.length,Z_SYNC_FLUSH)
      @z.zstream_run('',0,Z_FINISH)
      @z.zstream_detach_buffer()
    end

    def self.inflate(src)
      @z = ZStream.new
      @z.zstream_init(InflateFuncs)
      err = inflateInit(@z.stream)
      if (err != Z_OK)
       raise_zlib_error(err, @z.stream.msg)
      end
      @z.ZSTREAM_READY()
      begin
        dst = inflate_run(src)
      ensure
        @z.zstream_end
      end
      dst
    end

    def do_inflate(src)
      if(src.nil?)
        @z.zstream_run("", 0, Z_FINISH)
        return
      end
      if (src.length>0)
        @z.zstream_run(src,src.length,Z_SYNC_FLUSH)
      end
    end
    private :do_inflate

    def initialize(wbits=MAX_WBITS)
      @z = ZStream.new
      @z.zstream_init(InflateFuncs)
      err = inflateInit2(@z.stream, wbits)
      if (err != Z_OK)
       raise_zlib_error(err, @z.stream.msg)
      end
      @z.ZSTREAM_READY()
    end

    def inflate(src)
      if (@z.ZSTREAM_IS_FINISHED())
       if src.nil?
         dst = @z.zstream_detach_buffer()
        else
          @z.zstream_append_buffer(src,src.lenth)
          dst = ''
       end
      else
       do_inflate(src)
       dst = @z.zstream_detach_buffer()
       if (@z.ZSTREAM_IS_FINISHED())
         @z.zstream_passthrough_input()
       end
      end
      if block_given?
	      yield dst
      else
        dst
      end
    end

    def <<(src)
      if @z.ZSTREAM_IS_FINISHED()
        if src
          @z.zstream_append_buffer(src,src.length)
        end
      else
        do_inflate(src)
        if @z.ZSTREAM_IS_FINISHED()
          @z.zstream_passthrough_input()
        end
      end
      self
    end

    def sync
      raise GzipFile::Error,"closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      return @z.zstream_sync(src,src.length)
    end

    def sync_point?()
      err = inflateSyncPoint(@z.stream)
      return true if err == 1

      if err != Z_OK
        raise_zlib_error(err, @z.stream.msg)
      end

      false
    end

    def set_dictionary(dic)
      src = dic
      err = inflateSetDictionary(@z.stream,src,src.length)

      if err != Z_OK
        raise_zlib_error(err, @z.stream.msg)
      end

      dic
    end

  end

  class GzipFile
    GZ_MAGIC1            = 0x1f
    GZ_MAGIC2            = 0x8b
    GZ_METHOD_DEFLATE    = 8
    GZ_FLAG_MULTIPART    = 0x2
    GZ_FLAG_EXTRA        = 0x4
    GZ_FLAG_ORIG_NAME    = 0x8
    GZ_FLAG_COMMENT      = 0x10
    GZ_FLAG_ENCRYPT      = 0x20
    GZ_FLAG_UNKNOWN_MASK = 0xc0

    GZ_EXTRAFLAG_FAST    = 0x4
    GZ_EXTRAFLAG_SLOW    = 0x2

    OS_CODE = OS_UNIX

    GZFILE_FLAG_SYNC             = ZSTREAM_FLAG_UNUSED
    GZFILE_FLAG_HEADER_FINISHED  = (ZSTREAM_FLAG_UNUSED << 1)
    GZFILE_FLAG_FOOTER_FINISHED  = (ZSTREAM_FLAG_UNUSED << 2)

    def GZFILE_IS_FINISHED(gz)
      gz.z.ZSTREAM_IS_FINISHED() && (gz.z.buf.nil? || gz.z.buf.offset.zero?)
    end

    GZFILE_READ_SIZE  = 2048

    class Error < Zlib::Error
    end

    class NoFooter < Error
    end

    class CRCError < Error
    end

    class LengthError < Error
    end

    Gzfile = Struct.new(:z,:io,:level,:mtime,:os_code,:orig_name,:comment,:crc,:lineno,:ungetc,:end)

    def gzfile_close(closeflag)
      io = @gz.io
      send(@gz.end)

      @gz.io = nil
      @gz.orig_name = nil
      @gz.comment = nil

      if closeflag && defined?(io.close)
        io.close
      end
    end

    def gzfile_ensure_close()
      if @gz.z.ZSTREAM_IS_READY()
        gzfile_close(true)
      end
      nil
    end

    def self.wrap(io, level=Z_DEFAULT_COMPRESSION, strategy=Z_DEFAULT_STRATEGY)
      obj = new(io,level,strategy)
      if block_given?
        begin
          yield(obj)
        ensure
          obj.gzfile_ensure_close()
        end
      else
        return obj
      end
    end

    def to_io
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @gz.io
    end

    def crc
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @gz.crc
    end

    def mtime
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      Time.at(@gz.mtime)
    end

    def level
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @gz.level
    end

    def os_code
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @gz.os_code
    end

    def orig_name
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @gz.orig_name ? @gz.orig_name.dup : nil
    end

    def comment
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @gz.comment ? @gz.comment.dup : nil
    end

    def close
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      gzfile_close(true)
      @gz.io
    end

    def finish
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      gzfile_close(false)
      @gz.io
    end

    def closed?
      @gz.io.nil?
    end

    def sync
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      !(@gz.z.flags & GZFILE_FLAG_SYNC).zero?
    end

    def sync=(mode)
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      if(mode)
        @gz.z.flags |= GZFILE_FLAG_SYNC
      else
        @gz.z.flags &= ~GZFILE_FLAG_SYNC
      end
      mode
    end

    def self.gzfile_s_open(filename,mode,level,strategy,&blk)
      io = File.open(filename,mode)
      self.wrap(io,level,strategy,&blk)
    end

    private

    def gzfile_new(funcs,endfunc)
      @gz = Gzfile.new
      @gz.z = ZStream.new
      @gz.z.zstream_init(funcs)
      @gz.io = nil
      @gz.level = 0
      @gz.mtime = 0
      @gz.os_code = OS_CODE
      @gz.orig_name = nil
      @gz.comment = nil
      @gz.crc = crc32(0,nil,0)
      @gz.lineno = 0
      @gz.ungetc = 0
      @gz.end = endfunc
      self
    end

    def gzfile_reset()
      @gz.z.zstream_reset
      @gz.crc = crc32(0,nil,0)
      @gz.lineno = 0
      @gz.ungetc = 0
    end

    def gzfile_get16(src)
      src.unpack('v').first
    end

    def gzfile_get32(src)
      src.unpack('V').first
    end

    def gzfile_set32(n)
      [n].pack('V')
    end

    def gzfile_make_header
      buf = 0.chr * 10
      flags = 0
      extraflags = 0
      if @gz.orig_name
        flags |= GZ_FLAG_ORIG_NAME
      end
      if @gz.comment
        flags |= GZ_FLAG_COMMENT
      end
      if @gz.mtime.zero?
        @gz.mtime = Time.now.to_i
      end
      if (@gz.level == Z_BEST_SPEED)
        extraflags |= GZ_EXTRAFLAG_FAST
      elsif (@gz.level == Z_BEST_COMPRESSION)
        extraflags |= GZ_EXTRAFLAG_SLOW
      end
      buf[0] = GZ_MAGIC1.chr
      buf[1] = GZ_MAGIC2.chr
      buf[2] = GZ_METHOD_DEFLATE.chr
      buf[3] = flags.chr
      buf[4,4] = gzfile_set32(@gz.mtime)
      buf[8] = extraflags.chr
      buf[9] = @gz.os_code.chr
      @gz.z.zstream_append_buffer(buf,buf.length)

      if @gz.orig_name
        @gz.z.zstream_append_buffer(@gz.orig_name,@gz.orig_name.length)
        @gz.z.zstream_append_buffer("\0", 1)
      end
      if @gz.comment
        @gz.z.zstream_append_buffer(@gz.comment,@gz.comment.length)
        @gz.z.zstream_append_buffer("\0", 1)
      end

      @gz.z.flags |= GZFILE_FLAG_HEADER_FINISHED
    end

    def gzfile_make_footer()
      buf = 0.chr * 8
      buf[0,4] = gzfile_set32(@gz.crc)
      buf[4,4] = gzfile_set32(@gz.z.stream.total_in)
      @gz.z.zstream_append_buffer(buf, buf.length)
      @gz.z.flags |= GZFILE_FLAG_FOOTER_FINISHED
    end

    def gzfile_read_header()
      if !gzfile_read_raw_ensure(10)
       raise GzipFile::Error, "not in gzip format"
      end

      head = @gz.z.input

      if (head[0].ord != GZ_MAGIC1 || head[1].ord != GZ_MAGIC2)
         raise GzipFile::Error, "not in gzip format"
      end
      if (head[2].ord != GZ_METHOD_DEFLATE)
         raise GzipFile::Error, "unsupported compression method #{head[2].ord}"
      end

      flags = head[3].ord
      if (flags & GZ_FLAG_MULTIPART).nonzero?
         raise GzipFile::Error, "multi-part gzip file is not supported"
      elsif (flags & GZ_FLAG_ENCRYPT).nonzero?
         raise GzipFile::Error, "encrypted gzip file is not supported"
      elsif (flags & GZ_FLAG_UNKNOWN_MASK).nonzero?
         raise GzipFile::Error, "unknown flags 0x%02x" % flags
      end

      if (head[8].ord & GZ_EXTRAFLAG_FAST).nonzero?
         @gz.level = Z_BEST_SPEED
      elsif (head[8].ord & GZ_EXTRAFLAG_SLOW).nonzero?
         @gz.level = Z_BEST_COMPRESSION
      else
         @gz.level = Z_DEFAULT_COMPRESSION
      end

      @gz.mtime = gzfile_get32(head[4,4])
      @gz.os_code = head[9].ord
      @gz.z.zstream_discard_input(10)

      if (flags & GZ_FLAG_EXTRA).nonzero?
         if !gzfile_read_raw_ensure(2)
           raise GzipFile::Error, "unexpected end of file"
         end
         len = gzfile_get16(@gz.z.input)
         if !gzfile_read_raw_ensure(2 + len)
           raise GzipFile::Error, "unexpected end of file"
         end
         @gz.z.zstream_discard_input(2 + len)
      end
      if (flags & GZ_FLAG_ORIG_NAME).nonzero?
         ap = gzfile_read_raw_until_zero(0)
         len = ap
         @gz.orig_name = @gz.z.input[0,len]
         @gz.z.zstream_discard_input(len + 1)
      end
      if (flags & GZ_FLAG_COMMENT).nonzero?
         ap = gzfile_read_raw_until_zero(0)
         len = ap
         @gz.comment = @gz.z.input[0,len]
         @gz.z.zstream_discard_input(len + 1)
      end

      if (@gz.z.input && @gz.z.input.length > 0)
        @gz.z.zstream_run(0, 0, Z_SYNC_FLUSH)
      end
    end

    def gzfile_check_footer()
      @gz.z.flags |= GZFILE_FLAG_FOOTER_FINISHED

      if (!gzfile_read_raw_ensure(8))
        raise NoFooter, "footer is not found"
      end
      crc = gzfile_get32(@gz.z.input)
      length = gzfile_get32(@gz.z.input[4,4])
      @gz.z.stream.total_in += 8
      @gz.z.zstream_discard_input(8)
      if (@gz.crc != crc)
        raise CRCError, "invalid compressed data -- crc error"
      end
      if (@gz.z.stream.total_out != length)
        raise LengthError, "invalid compressed data -- length error"
      end
    end

    def gzfile_calc_crc(str)
      if (str.length <= @gz.ungetc)
        @gz.ungetc -= str.length
      else
        @gz.crc = crc32(@gz.crc, str[@gz.ungetc,str.length - @gz.ungetc],
         str.length - @gz.ungetc)
        @gz.ungetc = 0
      end
    end

  end

  class GzipWriter < GzipFile

    def mtime=(mtime)
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()

      if (@gz.z.flags & GZFILE_FLAG_HEADER_FINISHED).nonzero?
        raise GzipFile::Error,"header is already written"
      end

      @gz.mtime = mtime.to_i
    end

    def orig_name=(str)
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()

      if (@gz.z.flags & GZFILE_FLAG_HEADER_FINISHED).nonzero?
        raise GzipFile::Error,"header is already written"
      end

      ap = str[0.chr]
      @gz.orig_name = ap ? str[0,ap] : str.dup
    end

    def comment=(str)
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()

      if (@gz.z.flags & GZFILE_FLAG_HEADER_FINISHED).nonzero?
        raise GzipFile::Error,"header is already written"
      end

      @gz.comment = str.dup
    end

    def pos
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @gz.z.stream.total_in
    end

    alias tell :pos

    def self.open(filename, level=Z_DEFAULT_COMPRESSION, strategy=Z_DEFAULT_STRATEGY, &blk)
      GzipWriter.gzfile_s_open(filename, 'wb', level, strategy, &blk)
    end

    def initialize(io, level=nil, strategy=nil)
      level = Z_DEFAULT_COMPRESSION if level.nil?
      strategy = Z_DEFAULT_STRATEGY if strategy.nil?

      gzfile_new(DeflateFuncs, :gzfile_writer_end)
      @gz.level = level

      err = deflateInit2(
         @gz.z.stream,
         @gz.level,
         Z_DEFLATED,
         -MAX_WBITS,
         DEF_MEM_LEVEL,
         strategy
      )

      if err != Z_OK
        raise_zlib_error(err, @gz.stream.msg)
      end

      @gz.io = io
      @gz.z.ZSTREAM_READY()
    end

    def flush(v_flush=Z_SYNC_FLUSH)
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()

      if v_flush != Z_NO_FLUSH
        @gz.z.zstream_run("", 0, v_flush)
      end

      gzfile_write_raw()

      if defined?(@gz.io.flush)
        @gz.io.flush()
      end

      self
    end

    def write(str)
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      str = str.to_s
      gzfile_write(str, str.length)
      str.length
    end

    def putc(ch)
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      gzfile_write(ch.chr, 1)
      ch
    end

    def <<(str)
      @gz.io << str
    end

    def printf(*arg)
      @gz.io.printf(*arg)
    end

    def print(*arg)
      @gz.io.print(*arg)
    end

    def puts(str)
      @gz.io.puts(str)
    end

    def gzfile_write_raw
      if (@gz.z.buf.offset > 0)
        str = @gz.z.zstream_detach_buffer()
        @gz.io.write(str)
        if ((@gz.z.flags & GZFILE_FLAG_SYNC).nonzero? && defined?(@gz.io.flush))
          @gz.io.flush()
        end
      end
    end

    private :gzfile_write_raw

    def gzfile_write(str,len)
      if (@gz.z.flags & GZFILE_FLAG_HEADER_FINISHED).zero?
        gzfile_make_header()
      end

      if (len > 0 || (@gz.z.flags & GZFILE_FLAG_SYNC))
        @gz.crc = crc32(@gz.crc, str, len)
        @gz.z.zstream_run(str, len, (@gz.z.flags & GZFILE_FLAG_SYNC).nonzero? ?
            Z_SYNC_FLUSH : Z_NO_FLUSH)
      end
      gzfile_write_raw()
    end

    private :gzfile_write

    def gzfile_writer_end_run
      if (@gz.z.flags & GZFILE_FLAG_HEADER_FINISHED).zero?
        gzfile_make_header()
      end
      @gz.z.zstream_run("", 0, Z_FINISH)
      gzfile_make_footer()
      gzfile_write_raw()

      return nil
    end

    def gzfile_writer_end
      return if @gz.z.ZSTREAM_IS_CLOSING()
      @gz.z.flags |= ZSTREAM_FLAG_CLOSING
      begin
        gzfile_writer_end_run()
      ensure
        @gz.z.zstream_end()
      end
    end
  end

  class GzipReader < GzipFile
    include Enumerable

    def lineno
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @gz.lineno
    end

    def lineno=(lineno)
      raise GzipFile::Error, "closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      @gz.lineno = lineno
    end

    def eof
      raise GzipFile::Error,"closed gzip stream" unless @gz.z.ZSTREAM_IS_READY()
      GZFILE_IS_FINISHED(@gz)
    end
    alias eof? :eof

    def pos
      if @gz.z.buf.nil?
        @gz.z.stream.total_out
      else
        @gz.z.stream.total_out - @gz.z.buf.offset
      end
    end
    alias tell :pos

    def self.open(filename,level=Z_DEFAULT_COMPRESSION,strategy=Z_DEFAULT_STRATEGY,&blk)
      GzipReader.gzfile_s_open(filename,"rb",level=Z_DEFAULT_COMPRESSION,strategy=Z_DEFAULT_STRATEGY,&blk)
    end

    def initialize(io,level=Z_DEFAULT_COMPRESSION,strategy=Z_DEFAULT_STRATEGY)
      gzfile_new(InflateFuncs, :gzfile_reader_end)
      @gz.level = level
      err = inflateInit2(@gz.z.stream, -MAX_WBITS)
      if (err != Z_OK)
        raise_zlib_error(err, @gz.stream.msg)
      end
      @gz.io = io
      @gz.z.ZSTREAM_READY()
      gzfile_read_header()
      self
    end

    def rewind()
      gzfile_reader_rewind()
      return 0
    end

    def unused()
      gzfile_reader_get_unused()
    end

    def read(len=nil)
      if len.nil?
        return gzfile_read_all()
      end

      if len < 0
        raise ArgumentError, "negative length #{len} given"
      end

      return gzfile_read(len)
    end

    def getc()
      dst = gzfile_read(1)
      dst ? dst[0] : dst
    end

    def readchar()
      dst = getc()
      if dst.nil?
        raise EOFError, "end of file reached"
      end
      dst
    end

    def each_byte()
      while (c = getc)
        yield(c)
      end
      nil
    end

    def ungetc(ch)
      gzfile_ungetc(ch)
      nil
    end

    def gets(rs=$/)
      dst = gzreader_gets(rs)
      $_ = dst if dst
      dst
    end

    def readline(rs=$/)
      gets(rs)
    end

    def each(rs=$/)
      while (str = gzreader_gets(rs))
        yield(str)
      end
      self
    end
    alias each_line :each

    def readlines(rs=$/)
      dst = []
      while str = gzreader_gets(rs)
        dst.push(str)
      end
      dst
    end

    private

    def gzfile_reader_end_run()
      if (GZFILE_IS_FINISHED(@gz) && (@gz.z.flags &
        GZFILE_FLAG_FOOTER_FINISHED).zero?)
        gzfile_check_footer()
      end
      nil
    end

    def gzfile_reader_end()
      return if @gz.z.ZSTREAM_IS_CLOSING()
      @gz.z.flags |= ZSTREAM_FLAG_CLOSING
      begin
        gzfile_reader_end_run()
      ensure
        @gz.z.zstream_end()
      end
    end

    def gzfile_ungetc(c)
      @gz.z.zstream_buffer_ungetc(c)
      @gz.ungetc+=1
    end

    def gzfile_reader_rewind
      n = @gz.z.stream.total_in
      if @gz.z.input
        n += @gz.z.input.length
      end
      @gz.io.seek(-n,1)
      gzfile_reset()
    end

    def gzfile_reader_get_unused()
      return nil if (!@gz.z.ZSTREAM_IS_READY())
      return nil if (!GZFILE_IS_FINISHED(@gz))
      if (@gz.z.flags & GZFILE_FLAG_FOOTER_FINISHED).zero?
        gzfile_check_footer()
      end
      return nil if @gz.z.input.nil?
      @gz.z.input.dup
    end

    def rscheck(rsptr,rslen,rs)
      raise RuntimeError, "rs modified" if rs != rsptr
    end

    def gzreader_gets(rs=$/)
      if rs && rs.class != String
        raise TypeError,"wrong argument type #{rs.class} (expected String)"
      end
      if rs.nil?
        dst = gzfile_read_all()
        @gz.lineno += 1 if dst.length.nonzero?
        return dst
      end
      if rs.length.zero?
        rsptr = "\n\n"
        rslen = 2
        rspara = true
      else
        rsptr = rs
        rslen = rs.length
        rspara = false
      end
      if rspara
        gzreader_skip_linebreaks
      end
      while (@gz.z.buf.offset < rslen)
        if (@gz.z.ZSTREAM_IS_FINISHED())
          @gz.lineno+=1 if (@gz.z.buf.offset > 0)
          return gzfile_read(rslen)
        end
        gzfile_read_more()
      end

      ap = 0
      n = rslen
      loop do
        if (n > @gz.z.buf.offset)
          break if (@gz.z.ZSTREAM_IS_FINISHED())
          gzfile_read_more()
          ap = n - rslen
        end

        rscheck(rsptr, rslen, rs) if (!rspara)
        res = @gz.z.buf.buffer[ap,@gz.z.buf.offset - n + 1].index(rsptr[0])

        if res.nil?
          n = @gz.z.buf.offset + 1
        else
          n += (res - ap)
          ap = res
          break if (rslen == 1 || @gz.z.buf.buffer[ap,rslen]==rsptr)
          ap+=1
          n+=1
        end
      end

      @gz.lineno+=1
      dst = gzfile_read(n)
      if (rspara)
        gzreader_skip_linebreaks()
      end
      dst
    end

    def gzfile_read(len)
      if len < 0
        raise ArgumentError, "negative length #{len} given"
      end

      if len.zero?
        return ""
      end

      while (!@gz.z.ZSTREAM_IS_FINISHED() && @gz.z.buf.offset < len)
        gzfile_read_more()
      end

      if (GZFILE_IS_FINISHED(@gz))
        if (@gz.z.flags & GZFILE_FLAG_FOOTER_FINISHED).zero?
          gzfile_check_footer()
        end
        return nil
      end

      dst = @gz.z.zstream_shift_buffer(len)
      gzfile_calc_crc(dst)
      dst
    end

    def gzfile_read_all()
      while (!@gz.z.ZSTREAM_IS_FINISHED())
        gzfile_read_more()
      end
      if (GZFILE_IS_FINISHED(@gz))
        if (@gz.z.flags & GZFILE_FLAG_FOOTER_FINISHED).zero?
          gzfile_check_footer()
        end
        return ''
      end

      dst = @gz.z.zstream_detach_buffer()
      gzfile_calc_crc(dst)
      dst
    end

    def gzfile_read_raw()
      str = @gz.io.read(GZFILE_READ_SIZE)
      if str && str.class != String
        raise TypeError,"wrong argument type #{rs.class} (expected String)"
      end
      str
    end

    def gzfile_read_raw_ensure(size)
      while @gz.z.input.nil? || @gz.z.input.length < size
        str = gzfile_read_raw()
        return false if str.nil?
        @gz.z.zstream_append_input(str,str.length)
      end
      true
    end

    def gzfile_read_raw_until_zero(offset)
      ap = nil

      loop do
        ap = @gz.z.input[offset, @gz.z.input.length-offset].index(0.chr)
        break if ap
        str = gzfile_read_raw()

        raise Error, "unexpected end of file" if str.nil?

        offset = @gz.z.input.length
        @gz.z.zstream_append_input(str,str.length)
      end

      ap
    end

    def gzfile_read_more
      while (!@gz.z.ZSTREAM_IS_FINISHED())
        str = gzfile_read_raw()
        if str.nil?
          if (!@gz.z.ZSTREAM_IS_FINISHED())
            raise Error, "unexpected end of file"
          end
          break
        end
        if (str.length > 0)
          @gz.z.zstream_run(str, str.length,Z_SYNC_FLUSH)
        end
        break if (@gz.z.buf.offset > 0)
      end
      @gz.z.buf.offset
    end
  end

  module_function

  def zlib_version
    zlibVersion()
  end

  def adler32(string=nil, adler=nil)
    if adler
      check_long_range adler
      sum = adler
    elsif string.nil?
      sum = 0
    else
      sum = Rbzlib.adler32(0,nil)
    end

    if string.nil?
      sum = Rbzlib.adler32(sum,nil)
    else
      sum = Rbzlib.adler32(sum,string,string.length)
    end
    sum
  end

  def crc32(string=nil, crc=nil)
    if crc
      check_long_range crc
      sum = crc
    elsif string.nil?
      sum = 0
    else
      sum = Rbzlib.crc32(0,nil)
    end

    if string.nil?
      sum = Rbzlib.crc32(sum,nil)
    else
      sum = Rbzlib.crc32(sum,string,string.length)
    end
    sum
  end

  def crc_table
    get_crc_table
  end

  private

  LONG_MAX = 2**64 - 1
  LONG_MIN = -2**63

  def self.check_long_range(num)
    # the error says 'unsigned', but this seems to be the range actually accepted
    raise RangeError, 'bignum too big to convert into `unsigned long\'' if num < LONG_MIN || num > LONG_MAX
  end

end
