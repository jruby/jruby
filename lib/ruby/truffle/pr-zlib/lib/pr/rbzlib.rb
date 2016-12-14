#  rbrzlib -- pure ruby version of 'zlib' general purpose compression library
#  version 1.2.3, July 18th, 2005
#
#  Copyright (C) 1995-2005 Jean-loup Gailly and Mark Adler
#
#  This software is provided 'as-is', without any express or implied
#  warranty.  In no event will the authors be held liable for any damages
#  arising from the use of this software.
#
#  Permission is granted to anyone to use this software for any purpose,
#  including commercial applications, and to alter it and redistribute it
#  freely, subject to the following restrictions:
#
#  1. The origin of this software must not be misrepresented; you must not
#     claim that you wrote the original software. If you use this software
#     in a product, an acknowledgment in the product documentation would be
#     appreciated but is not required.
#  2. Altered source versions must be plainly marked as such, and must not be
#     misrepresented as being the original software.
#  3. This notice may not be removed or altered from any source distribution.
#
#  Jean-loup Gailly        Mark Adler
#  jloup@gzip.org          madler@alumni.caltech.edu
#
#  The data format used by the zlib library is described by RFCs (Request for
#  Comments) 1950 to 1952 in the files http://www.ietf.org/rfc/rfc1950.txt
#  (zlib format), rfc1951.txt (deflate format) and rfc1952.txt (gzip format).
#
#  Ruby translation
#  Copyright (C) 2009 by Park Heesob phasis@gmail.com
#

class Fixnum
  def ord
    self
  end
end

module Rbzlib
  MAX_MEM_LEVEL = 9
  DEF_MEM_LEVEL = 8
  MAX_WBITS     = 15
  DEF_WBITS     = MAX_WBITS

  Z_stream = Struct.new(
    :next_in,
    :avail_in,
    :total_in,
    :next_out,
    :avail_out,
    :total_out,
    :msg,
    :state,
    :data_type,
    :adler,
    :reserved
  )

  Gz_header = Struct.new(
    :text,
    :time,
    :xflags,
    :os,
    :extra,
    :extra_len,
    :extra_max,
    :name,
    :name_max,
    :comment,
    :comm_max,
    :hcrc,
    :done
  )

  Z_NO_FLUSH      = 0
  Z_PARTIAL_FLUSH = 1
  Z_SYNC_FLUSH    = 2
  Z_FULL_FLUSH    = 3
  Z_FINISH        = 4
  Z_BLOCK         = 5

  Z_OK            = 0
  Z_STREAM_END    = 1
  Z_NEED_DICT     = 2
  Z_ERRNO         = -1
  Z_STREAM_ERROR  = -2
  Z_DATA_ERROR    = -3
  Z_MEM_ERROR     = -4
  Z_BUF_ERROR     = -5
  Z_VERSION_ERROR = -6

  Z_NO_COMPRESSION      = 0
  Z_BEST_SPEED          = 1
  Z_BEST_COMPRESSION    = 9
  Z_DEFAULT_COMPRESSION = -1

  Z_FILTERED         = 1
  Z_HUFFMAN_ONLY     = 2
  Z_RLE              = 3
  Z_FIXED            = 4
  Z_DEFAULT_STRATEGY = 0

  Z_BINARY   = 0
  Z_ASCII    = 1
  Z_TEXT     = 1
  Z_UNKNOWN  = 2

  Z_DEFLATED   = 8

  STORED_BLOCK = 0
  STATIC_TREES = 1
  DYN_TREES    = 2

  MIN_MATCH   = 3
  MAX_MATCH   = 258
  PRESET_DICT = 0x20

  ZLIB_VERSION = '1.2.3'

  Z_errbase = Z_NEED_DICT

  @@z_errmsg = [
    'need dictionary',
    'stream end',
    '',
    'file error',
    'stream error',
    'data error',
    'insufficient memory',
    'buffer error',
    'incompatible version',
    ''
  ]

  @@z_verbose = 1

  def zError(err)
    @@z_errmsg[Z_NEED_DICT - err]
  end

  def zlibVersion
    ZLIB_VERSION
  end

  def z_error(m)
    raise RuntimeError, m
  end

  class Bytef
    def self.new(buffer, offset=0)
      if(buffer.class == Array)
        Bytef_arr.new(buffer,offset)
      else
        Bytef_str.new(buffer,offset)
      end
    end
  end

  class Bytef_str
    attr_accessor :buffer, :offset

    def initialize(buffer, offset=0)
      if buffer.class == String
        @buffer = buffer
        @offset = offset
        @buffer.force_encoding('ASCII-8BIT')
      else
        @buffer = buffer.buffer
        @offset = offset
      end
    end

    def length
      @buffer.length
    end

    def +(inc)
      @offset += inc
      self
    end

    def -(dec)
      @offset -= dec
      self
    end

    def [](idx)
      @buffer.getbyte(idx + @offset)
    end

    def []=(idx, val)
      @buffer.setbyte(idx + @offset,val.ord)
    end

    def get()
      @buffer.getbyte(@offset)
    end

    def set(val)
      @buffer.setbyte(@offset,val.ord)
    end

    def current
      @buffer[@offset..-1]
    end
  end

  class Bytef_arr < Bytef_str

    def initialize(buffer, offset=0)
        @buffer = buffer
        @offset = offset
    end

    def [](idx)
      @buffer[idx + @offset]
    end

    def []=(idx, val)
      @buffer[idx + @offset] = val
    end

    def get()
      @buffer[@offset]
    end

    def set(val)
      @buffer[@offset] = val
    end
  end

  class Posf < Bytef_str
    def +(inc)
      @offset += inc * 2
      self
    end

    def -(dec)
      @offset -= dec * 2
      self
    end

    def [](idx)
      @buffer[(idx * 2) + @offset, 2].unpack('v').first
    end

    def []=(idx, val)
      @buffer[(idx * 2) + @offset, 2] = [val].pack('v')
    end

    def get()
      @buffer[@offset, 2].unpack('v').first
    end

    def set(val)
      @buffer[@offset, 2] = [val].pack('v')
    end
  end

  BASE = 65521
  NMAX = 5552

  module_function

  # Compute the Adler-32 checksum of a data stream
  def adler32(adler, buf, len=0)
    return 1 if buf.nil?

    len = buf.length if len == 0
    sum2 = (adler >> 16) & 0xFFFF
    adler &= 0xffff

    if len == 1
      adler += buf[0].ord

      if adler >= BASE
        adler -= BASE
      end

      sum2 += adler

      if sum2 >= BASE
        sum2 -= BASE
      end

      return adler | (sum2 << 16)
    end

    if len < 16
      i = 0

      while len > 0
        len -= 1
        adler += buf[i].ord
        i += 1
        sum2 += adler
      end

      if adler >= BASE
        adler -= BASE
      end

      sum2 %= BASE

      return adler | (sum2 << 16)
    end

    i = 0

    while len >= NMAX
      len -= NMAX
      n = NMAX / 16

      loop do
        for j in 0 .. 15
          adler += buf[i+j].ord
          sum2 += adler
        end
        i += 16
        n -= 1
        break if n == 0
      end
      adler %= BASE
      sum2 %= BASE
    end

    if len != 0
      while (len >= 16)
        len -= 16
        for j in 0 .. 15
          adler += buf[i+j].ord
          sum2 += adler
        end
        i += 16
      end
      while len != 0
        len -= 1
        adler += buf[i].ord
        i += 1
        sum2 += adler
      end
      adler %= BASE
      sum2 %= BASE
    end

    return adler | (sum2 << 16)
  end

  @@crc_table = [
    0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419,
    0x706af48f, 0xe963a535, 0x9e6495a3, 0x0edb8832, 0x79dcb8a4,
    0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd, 0xe7b82d07,
    0x90bf1d91, 0x1db71064, 0x6ab020f2, 0xf3b97148, 0x84be41de,
    0x1adad47d, 0x6ddde4eb, 0xf4d4b551, 0x83d385c7, 0x136c9856,
    0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9,
    0xfa0f3d63, 0x8d080df5, 0x3b6e20c8, 0x4c69105e, 0xd56041e4,
    0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b,
    0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940, 0x32d86ce3,
    0x45df5c75, 0xdcd60dcf, 0xabd13d59, 0x26d930ac, 0x51de003a,
    0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423, 0xcfba9599,
    0xb8bda50f, 0x2802b89e, 0x5f058808, 0xc60cd9b2, 0xb10be924,
    0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d, 0x76dc4190,
    0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589, 0x06b6b51f,
    0x9fbfe4a5, 0xe8b8d433, 0x7807c9a2, 0x0f00f934, 0x9609a88e,
    0xe10e9818, 0x7f6a0dbb, 0x086d3d2d, 0x91646c97, 0xe6635c01,
    0x6b6b51f4, 0x1c6c6162, 0x856530d8, 0xf262004e, 0x6c0695ed,
    0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6, 0x12b7e950,
    0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf, 0x15da2d49, 0x8cd37cf3,
    0xfbd44c65, 0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2,
    0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a,
    0x346ed9fc, 0xad678846, 0xda60b8d0, 0x44042d73, 0x33031de5,
    0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa, 0xbe0b1010,
    0xc90c2086, 0x5768b525, 0x206f85b3, 0xb966d409, 0xce61e49f,
    0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17,
    0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad, 0xedb88320, 0x9abfb3b6,
    0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af, 0x04db2615,
    0x73dc1683, 0xe3630b12, 0x94643b84, 0x0d6d6a3e, 0x7a6a5aa8,
    0xe40ecf0b, 0x9309ff9d, 0x0a00ae27, 0x7d079eb1, 0xf00f9344,
    0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb,
    0x196c3671, 0x6e6b06e7, 0xfed41b76, 0x89d32be0, 0x10da7a5a,
    0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5,
    0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252, 0xd1bb67f1,
    0xa6bc5767, 0x3fb506dd, 0x48b2364b, 0xd80d2bda, 0xaf0a1b4c,
    0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55, 0x316e8eef,
    0x4669be79, 0xcb61b38c, 0xbc66831a, 0x256fd2a0, 0x5268e236,
    0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f, 0xc5ba3bbe,
    0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7, 0xb5d0cf31,
    0x2cd99e8b, 0x5bdeae1d, 0x9b64c2b0, 0xec63f226, 0x756aa39c,
    0x026d930a, 0x9c0906a9, 0xeb0e363f, 0x72076785, 0x05005713,
    0x95bf4a82, 0xe2b87a14, 0x7bb12bae, 0x0cb61b38, 0x92d28e9b,
    0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4, 0xf1d4e242,
    0x68ddb3f8, 0x1fda836e, 0x81be16cd, 0xf6b9265b, 0x6fb077e1,
    0x18b74777, 0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c,
    0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45, 0xa00ae278,
    0xd70dd2ee, 0x4e048354, 0x3903b3c2, 0xa7672661, 0xd06016f7,
    0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc, 0x40df0b66,
    0x37d83bf0, 0xa9bcae53, 0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9,
    0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605,
    0xcdd70693, 0x54de5729, 0x23d967bf, 0xb3667a2e, 0xc4614ab8,
    0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1, 0x5a05df1b,
    0x2d02ef8d
  ]

  # Tables of CRC-32s of all single-byte values, made by make_crc_table().
  #
  def get_crc_table
    @@crc_table
  end

  # Compute the CRC-32 of a data stream.
  #
  def crc32(crc, buf, len=0)
    return 0 if buf.nil?
    len = buf.length if len == 0
    crc = crc ^ 0xffffffff
    i = 0

    # Commented out library code here because method specs were failing
    # while len >= 8
    #   while i < 8
    #     crc = @@crc_table[(crc ^ buf[i].ord) & 0xff] ^ (crc >> 8)
    #     i += 1
    #   end
    #   len -= 8
    # end

    if len != 0
      loop do
        crc = @@crc_table[(crc ^ buf[i].ord) & 0xff] ^ (crc >> 8)
        i += 1
        len -= 1
        break if len == 0
      end
    end

    crc ^ 0xffffffff
  end

  OS_CODE = 0

  SEEK_SET = 0
  SEEK_CUR = 1
  SEEK_END = 2
  Z_EOF = -1
  Z_BUFSIZE = 16384

  @@gz_magic = "\x1F\x8B"

  ASCII_FLAG  = 0x01
  HEAD_CRC    = 0x02
  EXTRA_FIELD = 0x04
  ORIG_NAME   = 0x08
  COMMENT_    = 0x10
  RESERVED    = 0xE0

  Gz_stream = Struct.new(
    :stream,
    :z_err,
    :z_eof,
    :file,
    :inbuf,
    :outbuf,
    :crc,
    :msg,
    :path,
    :transparent,
    :mode,
    :start,
    :in,
    :out,
    :back,
    :last
  )

  # Opens a gzip (.gz) file for reading or writing. The mode parameter
  # is as in fopen ("rb" or "wb"). The file is given either by file descriptor
  # or path name (if fd == -1).
  #
  # gz_open returns NULL if the file could not be opened or if there was
  # insufficient memory to allocate the (de)compression state; errno
  # can be checked to distinguish the two cases (if errno is zero, the
  # zlib error is Z_MEM_ERROR).
  #
  def gz_open(path, mode, fd)
    return nil if path.nil? || mode.nil?

    s = Gz_stream.new
    s.stream = Z_stream.new

    level = Z_DEFAULT_COMPRESSION
    strategy = Z_DEFAULT_STRATEGY

    s.stream.next_in = nil
    s.stream.next_out = nil
    s.stream.avail_in = 0
    s.stream.avail_out = 0
    s.stream.msg = ''

    s.file = nil
    s.z_err = Z_OK
    s.z_eof = false
    s.inbuf = nil
    s.outbuf = nil
    s.in = 0
    s.out = 0
    s.back = Z_EOF
    s.crc = crc32(0, nil)
    s.msg = ''
    s.transparent = false
    s.path = path.dup
    s.mode = nil

    fmode = ''

    mode.each_byte do |c|
      s.mode = 'r' if c == ?r.ord
      s.mode = 'w' if c == ?w.ord || c == ?a.ord

      if c >= ?0.ord && c <= ?9.ord
        level = c - ?0.ord
      elsif c == ?f.ord
        strategy = Z_FILTERED
      elsif c == ?h.ord
        strategy = Z_HUFFMAN_ONLY
      elsif c == ?R.ord
        strategy = Z_RLE
      else
        fmode += c.chr
      end
    end

    if s.mode.nil?
      destroy(s)
      return nil
    end

    if s.mode == 'w'
      err = deflateInit2(
        s.stream,
        level,
        Z_DEFLATED,
        -MAX_WBITS,
        DEF_MEM_LEVEL,
        strategy
      )

      s.outbuf = 0.chr * Z_BUFSIZE
      s.stream.next_out = Bytef.new(s.outbuf)

      if err != Z_OK || s.outbuf.nil?
        destroy(s)
        return nil
      end
    else
      s.inbuf = 0.chr * Z_BUFSIZE
      s.stream.next_in = Bytef.new(s.inbuf)

      err = inflateInit2_(s.stream, -MAX_WBITS, ZLIB_VERSION, s.stream.size)

      if err != Z_OK || s.inbuf.nil?
        destroy(s)
        return nil
      end
    end

    s.stream.avail_out = Z_BUFSIZE

    s.file = fd < 0 ? File.new(path, fmode) : IO.new(fd, fmode)

    if s.mode == 'w'
      gzheader = 0.chr * 10
      gzheader[0] = @@gz_magic[0]
      gzheader[1] = @@gz_magic[1]
      gzheader[2] = Z_DEFLATED.chr
      gzheader[3] = 0.chr
      gzheader[4] = 0.chr
      gzheader[5] = 0.chr
      gzheader[6] = 0.chr
      gzheader[7] = 0.chr
      gzheader[8] = 0.chr
      gzheader[9] = OS_CODE.chr
      s.file.write(gzheader)
      s.start = 10
    else
      check_header(s)
      s.start = s.file.pos - s.stream.avail_in
    end

    return s
  end

  # Opens a gzip (.gz) file for reading or writing.
  #
  def gzopen(path,mode)
    return gz_open(path, mode, -1)
  end

  # Associate a gzFile with the file descriptor fd. fd is not dup'ed here
  # to mimic the behavio(u)r of fdopen.
  #
  def gzdopen(fd,mode)
    return nil if fd < 0
    name = "<fd:#{fd}"
    return gz_open(name, mode, fd)
  end

  # Update the compression level and strategy
  #
  def gzsetparams(file,level,strategy)
    s = file

    if s.nil? || s.mode != 'w'
      return Z_STREAM_ERROR
    end

    if s.stream.avail_out == 0
      s.stream.next_out = Bytef.new(s.outbuf)
      written = s.file.write(s.outbuf)

      if written != Z_BUFSIZE
        s.z_err = Z_ERRNO
      end

      s.stream.avail_out = Z_BUFSIZE
    end

    return deflateParams(s.stream, level, strategy)
  end

  # Read a byte from a gz_stream; update next_in and avail_in. Return EOF
  # for end of file.
  # IN assertion: the stream s has been sucessfully opened for reading.
  #
  def get_byte(s)
    return Z_EOF if s.z_eof

    if s.stream.avail_in == 0
       begin
          s.inbuf = s.file.read(Z_BUFSIZE)
          s.stream.avail_in = s.inbuf.length if s.inbuf
       rescue
          s.inbuf = nil
          s.z_err = Z_ERRNO
       end

       if s.inbuf.nil?
          s.z_eof = true
          return Z_EOF
       end
       s.stream.next_in = Bytef.new(s.inbuf)
    end

    s.stream.avail_in-=1
    _get_byte = s.stream.next_in.get
    s.stream.next_in+=1

    return _get_byte
  end

  # Reads a long in LSB order from the given gz_stream. Sets z_err in case
  # of error.
  def getLong(s)
    x = 0.chr * 4
    x[0] = (get_byte(s)).chr
    x[1] = (get_byte(s)).chr
    x[2] = (get_byte(s)).chr
    c = get_byte(s)
    x[3] = (c).chr

    s.z_err = Z_DATA_ERROR if (c == Z_EOF)

    return (x.unpack('v').first)
  end

  # Check the gzip header of a gz_stream opened for reading. Set the stream
  # mode to transparent if the gzip magic header is not present; set s->err
  # to Z_DATA_ERROR if the magic header is present but the rest of the header
  # is incorrect.
  #
  # IN assertion: the stream s has already been created sucessfully;
  #    s->stream.avail_in is zero for the first time, but may be non-zero
  #    for concatenated .gz files.
  #
  def check_header(s)
    len = s.stream.avail_in

    if len < 2
      if len != 0
        s.inbuf[0] = s.stream.next_in[0]
      end

      begin
        buf = s.file.read(Z_BUFSIZE >> len)
        if buf
          s.inbuf[len,buf.length] = buf
          len = buf.length
        else
          len = 0
        end
      rescue
        len = 0
        s.z_err = Z_ERRNO
      end

      s.stream.avail_in += len
      s.stream.next_in = Bytef.new(s.inbuf)

      if s.stream.avail_in < 2
        s.transparent = !s.stream.avail_in == 0
        return
      end
    end

    if s.stream.next_in[0] != @@gz_magic[0].ord ||
      s.stream.next_in[1] != @@gz_magic[1].ord
    then
      s.transparent = true
      return
    end

    s.stream.avail_in -= 2
    s.stream.next_in += 2

    method = get_byte(s)
    flags = get_byte(s)

    if (method != Z_DEFLATED) || (flags & RESERVED) != 0
      s.z_err = Z_DATA_ERROR
      return
    end

    for len in 0 .. 5
      get_byte(s)
    end

    if (flags & EXTRA_FIELD) != 0
      len = (get_byte(s))
      len += ((get_byte(s)) << 8)
      while len != 0 || (get_byte(s) != Z_EOF)
        len -= 1
      end
    end

    if (flags & ORIG_NAME) != 0
      loop do
        c = get_byte(s)
        break if c == 0 || (c == Z_EOF)
      end
    end

    if (flags & COMMENT_) != 0
      loop do
        c = get_byte(s)
        break if c == 0 || (c == Z_EOF)
      end
    end

    if (flags & HEAD_CRC) != 0
      get_byte(s)
      get_byte(s)
    end

    s.z_err = s.z_eof ? Z_DATA_ERROR : Z_OK
  end

  # Cleanup then free the given gz_stream. Return a zlib error code.
  def destroy(s)
    err = Z_OK
    return Z_STREAM_ERROR if s.nil?

    if s.stream.state
      if s.mode == 'w'
        err = deflateEnd(s.stream)
      elsif s.mode == 'r'
        err = inflateEnd(s.stream)
      end
    end

    begin
      s.file.close if s.file
      s.file = nil
    rescue
      err = Z_ERRNO
    end

    if s.z_err < 0
      err = s.z_err
    end

    return err
  end

  # Reads the given number of uncompressed bytes from the compressed file.
  # gzread returns the number of bytes actually read (0 for end of file).
  def gzread(file,buf,len)
    s = file
    start = Bytef.new(buf)

    if s.nil? || s.mode != 'r'
      return Z_STREAM_ERROR
    end

    return -1 if (s.z_err == Z_DATA_ERROR) || (s.z_err == Z_ERRNO)
    return 0 if (s.z_err == Z_STREAM_END)

    next_out = Bytef.new(buf)
    s.stream.next_out = Bytef.new(buf)
    s.stream.avail_out = len

    if s.stream.avail_out != 0 && s.back != Z_EOF
      next_out.set(s.back)
      next_out += 1
      s.stream.next_out += 1
      s.stream.avail_out -= 1
      s.back = Z_EOF
      s.out += 1
      start += 1

      if s.last
        s.z_err = Z_STREAM_END
        return 1
      end
    end

    while s.stream.avail_out != 0
      if s.transparent
        n = s.stream.avail_in

        if n > s.stream.avail_out
          n = s.stream.avail_out
        end

        if n > 0
          s.stream.next_out.buffer[s.stream.next_out.offset,n] = s.stream.next_in.current[0,n]
          next_out += n
          s.stream.next_out.offset = next_out.offset
          s.stream.next_in += n
          s.stream.avail_out -= n
          s.stream.avail_in -= n
        end

        if s.stream.avail_out > 0
          buff = s.file.read(s.stream.avail_out)
          if buff
            next_out.buffer[next_out.offset,buff.length] = buff
            s.stream.avail_out -= buff.length
          end
        end

        len -= s.stream.avail_out
        s.in += len
        s.out += len

        if len == 0
          s.z_eof = true
        end

        return len
      end

      if s.stream.avail_in == 0 && !s.z_eof
        begin
          buf = s.file.read(Z_BUFSIZE)
          if buf
            s.inbuf[0,buf.length] = buf
            s.stream.avail_in = buf.length
          else
            s.stream.avail_in = 0
          end
        rescue
          s.z_err = Z_ERRNO
        end

        if s.stream.avail_in == 0
          s.z_eof = true
          break if(s.z_err == Z_ERRNO)
        end
        s.stream.next_in = Bytef.new(s.inbuf)
      end

      s.in += s.stream.avail_in
      s.out += s.stream.avail_out
      s.z_err = inflate(s.stream, Z_NO_FLUSH)
      s.in -= s.stream.avail_in
      s.out -= s.stream.avail_out

      if s.z_err == Z_STREAM_END
        s.crc = crc32(s.crc, start.current,s.stream.next_out.offset - start.offset)
        start = s.stream.next_out.dup
        if getLong(s) != s.crc
          s.z_err = Z_DATA_ERROR
        else
          getLong(s)
          check_header(s)
          if s.z_err == Z_OK
            inflateReset(s.stream)
            s.crc = crc32(0, nil)
          end
        end
      end

      break if s.z_err != Z_OK || s.z_eof
    end

    s.crc = crc32(s.crc, start.current,s.stream.next_out.offset - start.offset)

    if len == s.stream.avail_out && (s.z_err == Z_DATA_ERROR || s.z_err = Z_ERRNO)
      return -1
    end

    return len - s.stream.avail_out
  end

  # Reads one byte from the compressed file. gzgetc returns this byte
  # or -1 in case of end of file or error.
  #
  def gzgetc(file)
    c = 0.chr
    if (gzread(file,c,1) == 1)
      return c
    else
      return -1
    end
  end

  # Push one byte back onto the stream.
  #
  def gzungetc(c, file)
    s = file
    return Z_EOF if s.nil? || s.mode != 'r' || c == Z_EOF || s.back != Z_EOF
    s.back = c
    s.out -= 1
    s.last = (s.z_err == Z_STREAM_END)
    s.z_err = Z_OK if s.last
    s.z_eof = false
    return c
  end

  # Reads bytes from the compressed file until len-1 characters are
  # read, or a newline character is read and transferred to buf, or an
  # end-of-file condition is encountered.  The string is then terminated
  # with a null character.
  #
  # Returns buf, or Z_NULL in case of error.
  #
  def gzgets(file,buf,len)
    return nil if buf.nil? || (len <= 0)

    i = 0
    gzchar = 0.chr

    loop do
      len-=1
      bytes = gzread(file, gzchar, 1)
      buf[i] = gzchar[0]
      i += 1
      break if len == 0 || (bytes != 1) || (gzchar == (13).chr)
    end

    buf[i..-1] = ''
    buf.chomp!(0.chr)

    if i == 0 && (len > 0)
      return nil
    else
      return buf
    end
  end

  #   Writes the given number of uncompressed bytes into the compressed file.
  # gzwrite returns the number of bytes actually written (0 in case of error).
  def gzwrite(file,buf,len)
    s = file
    if s.nil? || (s.mode != 'w')
      return Z_STREAM_ERROR
    end

    s.stream.next_in = Bytef.new(buf)
    s.stream.avail_in = len
    while s.stream.avail_in != 0
      if s.stream.avail_out == 0
        s.stream.next_out = Bytef.new(s.outbuf)
        written = s.file.write(s.outbuf)
        if (written != Z_BUFSIZE)
          s.z_err = Z_ERRNO
          break
        end
        s.stream.avail_out = Z_BUFSIZE
      end
      s.in += s.stream.avail_in
      s.out += s.stream.avail_out
      s.z_err = deflate(s.stream, Z_NO_FLUSH)
      s.in -= s.stream.avail_in
      s.out -= s.stream.avail_out
      break if (s.z_err != Z_OK)
    end
    s.crc = crc32(s.crc, buf, len)
    return (len - s.stream.avail_in)
  end

  #   Writes c, converted to an unsigned char, into the compressed file.
  # gzputc returns the value that was written, or -1 in case of error.
  def gzputc(file,c)
    if (gzwrite(file,c,1) == 1)
      return c
    else
      return -1
    end
  end

  #    Writes the given null-terminated string to the compressed file, excluding
  # the terminating null character.
  #    gzputs returns the number of characters written, or -1 in case of error.
  def gzputs(file,s)
    return gzwrite(file,s,s.length)
  end

  #   Flushes all pending output into the compressed file. The parameter
  # flush is as in the deflate() function.
  def do_flush(file,flush)
    done = false
    s = file

    if (s.nil?) || (s.mode != 'w')
      return Z_STREAM_ERROR
    end

    s.stream.avail_in = 0

    loop do
      len = Z_BUFSIZE - s.stream.avail_out

      if len != 0
        written = s.file.write(s.outbuf[0,len])
        if (written != len)
          s.z_err = Z_ERRNO
          return Z_ERRNO
        end
        s.stream.next_out = Bytef.new(s.outbuf)
        s.stream.avail_out = Z_BUFSIZE
      end

      break if (done)
      s.out += s.stream.avail_out
      s.z_err = deflate(s.stream, flush)
      s.out -= s.stream.avail_out

      if len == 0 && (s.z_err == Z_BUF_ERROR)
        s.z_err = Z_OK
      end

      done = s.stream.avail_out != 0 || (s.z_err == Z_STREAM_END)
      break if (s.z_err != Z_OK) && (s.z_err != Z_STREAM_END)

    end

    if (s.z_err = Z_STREAM_END)
      return Z_OK
    else
      return s.z_err
    end
  end

  # Flush output file.
  def gzflush(file,flush)
    s = file
    err = do_flush(file, flush)

    if err != 0
      return err
    end

    if (s.z_err == Z_STREAM_END)
      return Z_OK
    else
      return s.z_err
    end
  end

  # Rewinds input file.
  def gzrewind(file)
    s = file

    if s.nil? || (s.mode != 'r')
      return -1
    end

    s.z_err = Z_OK
    s.z_eof = false
    s.stream.avail_in = 0
    s.stream.next_in = Bytef.new(s.inbuf)
    s.crc = crc32(0,nil)
    if !s.transparent
      inflateReset(s.stream)
    end
    s.in = 0
    s.out = 0
    return s.file.seek(s.start, SEEK_SET)
  end

  #    Sets the starting position for the next gzread or gzwrite on the given
  # compressed file. The offset represents a number of bytes in the
  #    gzseek returns the resulting offset location as measured in bytes from
  # the beginning of the uncompressed stream, or -1 in case of error.
  #    SEEK_END is not implemented, returns error.
  #    In this version of the library, gzseek can be extremely slow.
  def gzseek(file,offset,whence)
    s = file

    if s.nil? || (whence == SEEK_END) || (s.z_err == Z_ERRNO) ||
           (s.z_err == Z_DATA_ERROR)
      return(-1)
    end

    if (s.mode == 'w')
      if (whence == SEEK_SET)
        offset -= s.in
      end
      if (offset < 0)
        return(-1)
      end

      if (s.inbuf.nil?)
        s.inbuf = 0.chr*Z_BUFSIZE
      end

      while (offset > 0)
        size = Z_BUFSIZE
        if (offset < Z_BUFSIZE)
          size = (offset)
        end

        size = gzwrite(file, s.inbuf, size)
        if size == 0
          return(-1)
        end

        offset -= size
      end

      return(s.in)
    end

    if (whence == SEEK_CUR)
      offset += s.out
    end
    if (offset < 0)
      return(-1)
    end

    if s.transparent
      s.back = Z_EOF
      s.stream.avail_in = 0
      s.stream.next_in = Bytef.new(s.inbuf)
      s.file.seek(offset, SEEK_SET)

      s.in = offset
      s.out = offset
      return offset
    end

    if (offset >= s.out)
      offset -= s.out
    elsif (gzrewind(file) < 0)
      return(-1)
    end

    if offset != 0 && s.outbuf.nil?
      s.outbuf = 0.chr * Z_BUFSIZE
    end
    if(offset != 0 && s.back != Z_EOF)
      s.back = Z_EOF
      s.out += 1
      offset -= 1
      s.z_err = Z_STREAM_END if s.last
    end
    while (offset > 0)
      size = Z_BUFSIZE
      if (offset < Z_BUFSIZE)
        size = (offset)
      end
      size = gzread(file, s.outbuf, size)

      if (size <= 0)
        return(-1)
      end
      offset -= size
    end

    return(s.out)
  end

  #   Returns the starting position for the next gzread or gzwrite on the
  # given compressed file. This position represents a number of bytes in the
  # uncompressed data stream.
  def gztell(file)
    return gzseek(file, 0, SEEK_CUR)
  end

  #   Returns 1 when EOF has previously been detected reading the given
  # input stream, otherwise zero.
  def gzeof(file)
    s = file

    return false if s.nil? || (s.mode!='r')
    return s.z_eof if s.z_eof
    return s.z_err == Z_STREAM_END
  end

  #   Returns 1 if reading and doing so transparently, otherwise zero.
  def gzdirect(file)
    s = file

    return false if (s.nil? || s.mode != 'r')
    return s.transparent
  end

  # Outputs a long in LSB order to the given file
  def putLong(s,x)
    4.times{
      c = x & 0xFF
      s.putc(c)
      x = x >> 8
    }
  end

   # Flushes all pending output if necessary, closes the compressed file
   # and deallocates all the (de)compression state.
   #
   def gzclose(file)
      s = file

      if s.nil?
         return Z_STREAM_ERROR
      end

      if s.mode == 'w'
         err = do_flush(file, Z_FINISH)
         if err != Z_OK
            return destroy(f)
         end

         putLong(s.file, s.crc)
         putLong(s.file, s.in & 0xffffffff)
      end

      return destroy(file)
   end

  #   Returns the error message for the last error which occurred on the
  # given compressed file. errnum is set to zlib error number. If an
  # error occurred in the file system and not in the compression library,
  # errnum is set to Z_ERRNO and the application may consult errno
  def gzerror(file,errnum)
    s = file
    if s.nil?
      errnum = Z_STREAM_ERROR
      return zError(Z_STREAM_ERROR)
    end

    errnum = s.z_err
    if (errnum == Z_OK)
      return zError(Z_OK)
    end

    m = s.stream.msg
    if (errnum == Z_ERRNO)
      m = ''
    end
    if (m == '')
      m = zError(s.z_err)
    end
    s.msg = s.path + ': ' + m
    return s.msg
  end

  # Clear the error and end-of-file flags, and do the same for the real file.
  def gzclearerr(file)
    s = file
    return if s.nil?
    s.z_err = Z_OK if(s.z_err != Z_STREAM_END)
    s.z_eof = false

  end

  ZNIL = 0
  TOO_FAR = 4096
  MIN_LOOKAHEAD = (MAX_MATCH+MIN_MATCH+1)

  Config = Struct.new(:good_length,:max_lazy,:nice_length,:max_chain,:func)
  @@configuration_table = [
    [0,  0,   0,   0,    :deflate_stored],
    [4,  4,   8,   4,    :deflate_fast],
    [4,  5,   16,  8,    :deflate_fast],
    [4,  6,   32,  32,   :deflate_fast],

    [4,  4,   16,  16,   :deflate_slow],
    [8,  16,  32,  32,   :deflate_slow],
    [8,  16,  128, 128,  :deflate_slow],
    [8,  32,  128, 256,  :deflate_slow],
    [32, 128, 258, 1024, :deflate_slow],
    [32, 258, 258, 4096, :deflate_slow]].map{|i|Config.new(*i)}

  EQUAL = 0

  # Insert string str in the dictionary and set match_head to the previous head
  # of the hash chain (the most recent string with same hash key). Return
  # the previous length of the hash chain.
  # If this file is compiled with -DFASTEST, the compression level is forced
  # to 1, and no hash chains are maintained.
  # IN  assertion: all calls to to INSERT_STRING are made with consecutive
  #    input characters and the first MIN_MATCH bytes of str are valid
  #    (except for the last MIN_MATCH-1 bytes of the input file).
  def INSERT_STRING(s,str,match_head)
    s.ins_h = ((s.ins_h << s.hash_shift) ^ (s.window[str + 2].ord)) & s.hash_mask

    match_head = s.head[s.ins_h]
    s.prev[(str) & s.w_mask] = match_head
    s.head[s.ins_h] = str
    return match_head
  end

  # Initialize the hash table (avoiding 64K overflow for 16 bit systems).
  # prev[] will be initialized on the fly.
  def deflateInit2_(strm,level,method,windowBits,memLevel,strategy,version,stream_size)
    wrap = 1
    my_version = ZLIB_VERSION

    if (version  ==  '') || (version[1] != my_version[1]) || (stream_size!=Z_stream.new.size)
      return Z_VERSION_ERROR
    end

    strm.msg = ''
    if (level  ==  Z_DEFAULT_COMPRESSION)
      level = 6
    end
    if (windowBits < 0)
      wrap = 0
      windowBits = -windowBits
    elsif (windowBits > 15)
      wrap = 2
      windowBits -= 16
    end
    if (memLevel < 1) || (memLevel > MAX_MEM_LEVEL) || (method != Z_DEFLATED) ||
           (windowBits < 8) || (windowBits > 15) || (level < 0) || (level > 9) ||
           (strategy < 0) || (strategy > Z_FIXED)
      return Z_STREAM_ERROR
    end

    windowBits = 9 if(windowBits==8)
    s = Deflate_state.new
    s.dyn_ltree = Array.new(HEAP_SIZE).map{|i|Ct_data.new()}
    s.dyn_dtree = Array.new(2*D_CODES+1).map{|i|Ct_data.new()}
    s.bl_tree = Array.new(2*BL_CODES+1).map{|i|Ct_data.new()}
    s.bl_count = Array.new(MAX_BITS+1,0)
    s.heap = Array.new(2*L_CODES+1,0)
    s.depth = Array.new(2*L_CODES+1,0)
    strm.state = s
    s.strm = strm

    s.wrap = wrap
    s.gzhead = nil
    s.w_bits = windowBits
    s.w_size = 1 << s.w_bits
    s.w_mask = s.w_size - 1

    s.hash_bits = memLevel + 7
    s.hash_size = 1 << s.hash_bits
    s.hash_mask = s.hash_size - 1
    s.hash_shift =  ((s.hash_bits+MIN_MATCH-1) / MIN_MATCH)

    s.window = 0.chr * (s.w_size * 2)
    s.prev   = Array.new(s.w_size,0)
    s.head   = Array.new(s.hash_size,0)

    s.lit_bufsize = 1 << (memLevel + 6)

    overlay = 0.chr * (s.lit_bufsize * (2+2))
    s.pending_buf = Bytef.new(overlay)
    s.pending_buf_size = (s.lit_bufsize) * (2+2)

    if s.window.nil? || s.prev.nil? || s.head.nil? || s.pending_buf.nil?
      s.status = FINISH_STATE
      strm.msg = @@z_errmsg[Z_errbase-Z_MEM_ERROR]
      deflateEnd(strm)
      return Z_MEM_ERROR
    end
    s.d_buf = Posf.new(overlay,s.lit_bufsize)
    s.l_buf = Bytef.new(overlay,(1+2) * s.lit_bufsize)

    s.level = level
    s.strategy = strategy
    s.method = method
    deflateReset(strm)
  end

  def deflateInit2(strm,level,method,windowBits,memLevel,strategy)
    deflateInit2_(strm, level, method, windowBits,
                   memLevel, strategy, ZLIB_VERSION,strm.size)
  end

  def deflateInit_(strm,level,version,stream_size)
    if strm.nil?
      return Z_STREAM_ERROR
    else
      return deflateInit2_(strm, level, Z_DEFLATED, MAX_WBITS,
                   DEF_MEM_LEVEL, Z_DEFAULT_STRATEGY, version, stream_size)
    end
  end

  def deflateInit(strm,level)
    deflateInit2_(strm, level, Z_DEFLATED, MAX_WBITS,
         DEF_MEM_LEVEL, Z_DEFAULT_STRATEGY, ZLIB_VERSION,strm.size)
  end

  #
  def deflateSetDictionary(strm,dictionary,dictLength)
    length = dictLength
    offset = 0
    hash_head = 0

    if (strm.nil? || strm.state.nil? || dictionary.nil? || strm.state.wrap==2 ||
           (strm.state.wrap==1 && strm.state.status != INIT_STATE))
      return Z_STREAM_ERROR
    end

    s = strm.state
    strm.adler = adler32(strm.adler, dictionary,dictLength) if s.wrap != 0
    if (length < MIN_MATCH)
      return Z_OK
    end
    _MAX_DIST = (s.w_size - MIN_LOOKAHEAD)
    if (length > _MAX_DIST)
      length = _MAX_DIST

      offset += dictLength - length
    end

    s.window[0,length] = dictionary[offset,length]
    s.strstart = length
    s.block_start = length

    s.ins_h = s.window[0].ord

    s.ins_h = ((s.ins_h << s.hash_shift) ^ (s.window[1].ord)) & s.hash_mask

    for n in 0 .. (length - MIN_MATCH)
      hash_head = INSERT_STRING(s, n, hash_head)
    end
    return Z_OK
  end

  #
  def deflateReset(strm)
    if (strm.nil? || strm.state.nil?)
      return Z_STREAM_ERROR
    end

    strm.total_out = 0
    strm.total_in = 0
    strm.msg = ''
    strm.data_type = Z_UNKNOWN

    s = strm.state
    s.pending = 0
    s.pending_out = Bytef.new(s.pending_buf)

    if (s.wrap < 0)
      s.wrap = -s.wrap
    end
    s.status = s.wrap != 0 ? INIT_STATE : BUSY_STATE
    strm.adler = (s.wrap==2) ? crc32(0,nil) : adler32(0,nil)
    s.last_flush = Z_NO_FLUSH

    _tr_init(s)
    lm_init(s)

    return Z_OK
  end

  #
  def deflateSetHeader(strm,head)
    if (strm.nil? || strm.state.nil?)
      return Z_STREAM_ERROR
    end
    return Z_STREAM_ERROR if(strm.state.wrap != 2)
    strm.state.gzhead = head
    return Z_OK
  end

  #
  def deflatePrime(strm,bits,value)
    if (strm.nil? || strm.state.nil?)
      return Z_STREAM_ERROR
    end
    strm.state.bi_valid = bits
    strm.state.bi_buf = (value & ((1<<bits)-1))
    return Z_OK
  end

  #
  def deflateParams(strm,level,strategy)
    err = Z_OK
    if strm.state.nil?
      return Z_STREAM_ERROR
    end

    s = strm.state

    if (level == Z_DEFAULT_COMPRESSION)
      level = 6
    end
    if (level < 0) || (level > 9) || (strategy < 0) || (strategy > Z_HUFFMAN_ONLY)
      return Z_STREAM_ERROR
    end
    func = @@configuration_table[s.level].func

    if (func != @@configuration_table[level].func) && strm.total_in != 0
        err = deflate(strm, Z_PARTIAL_FLUSH)
    end
    if (s.level != level)
      s.level = level
      s.max_lazy_match   = @@configuration_table[level].max_lazy
      s.good_match       = @@configuration_table[level].good_length
      s.nice_match       = @@configuration_table[level].nice_length
      s.max_chain_length = @@configuration_table[level].max_chain
    end
    s.strategy = strategy
    return err
  end

  #
  def deflateTune(strm, good_length, max_lazy, nice_length, max_chain)
    if (strm.nil? || strm.state.nil?)
      return Z_STREAM_ERROR
    end
    s = strm.state
    s.good_match = good_length
    s.max_lazy_match = max_lazy
    s.nice_match = nice_length
    s.max_chain_length = max_chain
    return Z_OK
  end

  # For the default windowBits of 15 and memLevel of 8, this function returns
  # a close to exact, as well as small, upper bound on the compressed size.
  # They are coded as constants here for a reason--if the #define's are
  # changed, then this function needs to be changed as well.  The return
  # value for 15 and 8 only works for those exact settings.
  #
  # For any setting other than those defaults for windowBits and memLevel,
  # the value returned is a conservative worst case for the maximum expansion
  # resulting from using fixed blocks instead of stored blocks, which deflate
  # can emit on compressed data for some combinations of the parameters.
  #
  # This function could be more sophisticated to provide closer upper bounds
  # for every combination of windowBits and memLevel, as well as wrap.
  # But even the conservative upper bound of about 14% expansion does not
  # seem onerous for output buffer allocation.
  def deflateBound(strm, sourceLen)
    destLen = sourceLen +
              ((sourceLen + 7) >> 3) + ((sourceLen + 63) >> 6) + 11

    if (strm.nil? || strm.state.nil?)
      return destLen
    end

    s = strm.state
    if (s.w_bits != 15 || s.hash_bits != 8 + 7)
      return destLen
    end

    return compressBound(sourceLen)
  end

  # Put a short in the pending buffer. The 16-bit value is put in MSB order.
  # IN assertion: the stream state is correct and there is enough room in
  # pending_buf.
  def putShortMSB(s,b)
    s.pending_buf[s.pending] = (b >> 8)
    s.pending+=1
    s.pending_buf[s.pending] = (b & 0xff)
    s.pending+=1
  end

  # Flush as much pending output as possible. All deflate() output goes
  # through this function so some applications may wish to modify it
  # to avoid allocating a large strm->next_out buffer and copying into it.
  # (See also read_buf()).
  def flush_pending(strm)
    s = strm.state
    len = s.pending
    if (len > strm.avail_out)
      len = strm.avail_out
    end
    return if len == 0
    strm.next_out.buffer[strm.next_out.offset,len] = s.pending_out.current[0,len]
    strm.next_out += len
    s.pending_out += len
    strm.total_out += len
    strm.avail_out -= len
    s.pending -= len
    if s.pending == 0
      s.pending_out = Bytef.new(strm.state.pending_buf)
    end
  end

  #
  def deflate(strm,flush)
    if strm.state.nil? || (flush > Z_FINISH) || (flush < 0)
      return Z_STREAM_ERROR
    end
    s = strm.state

    if strm.next_out.nil? ||
       (strm.next_in.nil? && strm.avail_in != 0) ||
       ((s.status == FINISH_STATE) && (flush != Z_FINISH))
      strm.msg = @@z_errmsg[Z_errbase - Z_STREAM_ERROR]
      return Z_STREAM_ERROR
    end

    if strm.avail_out == 0
      strm.msg = @@z_errmsg[Z_errbase - Z_BUF_ERROR]
      return Z_BUF_ERROR
    end

    s.strm = strm
    old_flush = s.last_flush
    s.last_flush = flush

    if (s.status == INIT_STATE)
      if (s.wrap == 2)
        strm.adler = crc32(0,nil)
        s.pending_buf[s.pending] = 31
        s.pending+=1
        s.pending_buf[s.pending] = 139
        s.pending+=1
        s.pending_buf[s.pending] = 8
        s.pending+=1
        if s.gzhead.nil?
          s.pending_buf[s.pending] = 0
          s.pending+=1
          s.pending_buf[s.pending] = 0
          s.pending+=1
          s.pending_buf[s.pending] = 0
          s.pending+=1
          s.pending_buf[s.pending] = 0
          s.pending+=1
          s.pending_buf[s.pending] = 0
          s.pending+=1
          s.pending_buf[s.pending] = (s.level==9) ? 2 :
            (s.strategy >= Z_HUFFMAN_ONLY || s.level < 2 ?
                             4 : 0)
          s.pending+=1
        else
          c = (s.gzhead.text ? 1 : 0) +
                            (s.gzhead.hcrc ? 2 : 0) +
                            (s.gzhead.extra.nil? ? 0 : 4) +
                            (s.gzhead.name.nil? ? 0 : 8) +
                            (s.gzhead.comment.nil? ? 0 : 16)
          s.pending_buf[s.pending] = c
          s.pending+=1
          c = s.gzhead.time & 0xff
          s.pending_buf[s.pending] = c
          s.pending+=1
          c = (s.gzhead.time >> 8) & 0xff
          s.pending_buf[s.pending] = c
          s.pending+=1
          c = (s.gzhead.time >> 16) & 0xff
          s.pending_buf[s.pending] = c
          s.pending+=1
          c = (s.gzhead.time >> 24) & 0xff
          s.pending_buf[s.pending] = c
          s.pending+=1
          c = s.level == 9 ? 2 :
                            (s.strategy >= Z_HUFFMAN_ONLY || s.level < 2 ?
                             4 : 0)
          s.pending_buf[s.pending] = c
          s.pending+=1
          c = s.gzhead.os & 0xff
          s.pending_buf[s.pending] = c
          s.pending+=1
          if s.gzhead.extra
            c = s.gzhead.extra_len & 0xff
            s.pending_buf[s.pending] = c
            s.pending+=1
            c = (s.gzhead.extra_len >> 8) & 0xff
            s.pending_buf[s.pending] = c
            s.pending+=1
          end
          if (s.gzhead.hcrc)
            strm.adler = crc32(strm.adler, s.pending_buf.buffer, s.pending)
            s.gzindex = 0
            s.status = EXTRA_STATE
          end
        end
      else
        header = (Z_DEFLATED + ((s.w_bits-8) << 4)) << 8

        if (s.strategy >= Z_HUFFMAN_ONLY || s.level < 2)
          level_flags = 0
        elsif (s.level < 6)
          level_flags = 1
        elsif (s.level == 6)
          level_flags = 2
        else
          level_flags = 3
        end
        header |= (level_flags << 6)
        if s.strstart != 0
          header |= PRESET_DICT
        end
        header += 31 - (header % 31)

        s.status = BUSY_STATE
        putShortMSB(s, header)

        if s.strstart != 0
          putShortMSB(s, (strm.adler >> 16))
          putShortMSB(s, (strm.adler & 0xffff))
        end
        strm.adler = adler32(0,nil)
      end
    end

    if (s.status == EXTRA_STATE)
      if s.gzhead.extra
        beg = s.pending

        while (s.gzindex < (s.gzhead.extra_len & 0xffff))
          if (s.pending == s.pending_buf_size)
            if (s.gzhead.hcrc && s.pending > beg)
              strm.adler = crc32(strm.adler, s.pending_buf.buffer[beg,s.pending - beg])
            end
            flush_pending(strm)
            beg = s.pending
            break if (s.pending == s.pending_buf_size)
          end
          s.pending_buf[s.pending] = s.gzhead.extra[s.gzindex]
          s.pending+=1
          s.gzindex+=1
        end
        if (s.gzhead.hcrc && s.pending > beg)
          strm.adler = crc32(strm.adler, s.pending_buf[beg,s.pending - beg])
        end
        if (s.gzindex == s.gzhead.extra_len)
          s.gzindex = 0
          s.status = NAME_STATE
        end
      else
        s.status = NAME_STATE
      end
    end
    if (s.status == NAME_STATE)
      if s.gzhead.name
        beg = s.pending
        loop do
          if (s.pending == s.pending_buf_size)
            if (s.gzhead.hcrc && s.pending > beg)
              strm.adler = crc32(strm.adler, s.pending_buf.buffer[beg,s.pending - beg])
            end
            flush_pending(strm)
            beg = s.pending
            if (s.pending == s.pending_buf_size)
              val = 1
              break
            end
          end
          val = s.gzhead.name[s.gzindex]
          s.gzindex+=1
          s.pending_buf[s.pending] = val
          s.pending+=1
          break if val == 0
        end
        if (s.gzhead.hcrc && s.pending > beg)
          strm.adler = crc32(strm.adler, s.pending_buf.buffer[beg,s.pending - beg])
        end
        if val == 0
          s.gzindex = 0
          s.status = COMMENT_STATE
        end
      else
        s.status = COMMENT_STATE
      end
    end
    if (s.status == COMMENT_STATE)
      if s.gzhead.comment
        beg = s.pending
        loop do
          if (s.pending == s.pending_buf_size)
            if (s.gzhead.hcrc && s.pending > beg)
              strm.adler = crc32(strm.adler, s.pending_buf.buffer[beg,s.pending - beg])
            end
            flush_pending(strm)
            beg = s.pending
            if (s.pending == s.pending_buf_size)
              val = 1
              break
            end
          end
          val = s.gzhead.comment[s.gzindex]
          s.gzindex+=1
          s.pending_buf[s.pending] = val
          s.pending+=1
          break if val == 0
        end
        if (s.gzhead.hcrc && s.pending > beg)
          strm.adler = crc32(strm.adler, s.pending_buf.buffer[beg,s.pending - beg])
        end
        if val == 0
          s.status = HCRC_STATE
        end
      else
        s.status = HCRC_STATE
      end
    end
    if (s.status == HCRC_STATE)
      if (s.gzhead.hcrc)
        if (s.pending + 2 > s.pending_buf_size)
          flush_pending(strm)
        end
        if (s.pending + 2 <= s.pending_buf_size)
          s.pending_buf[s.pending] = strm.adler & 0xff
          s.pending+=1
          s.pending_buf[s.pending] = (strm.adler >> 8) & 0xff
          s.pending+=1
          strm.adler = crc32(0, nil)
          s.status = BUSY_STATE
        end
      else
        s.status = BUSY_STATE
      end
    end
    if s.pending != 0
      flush_pending(strm)
      if strm.avail_out == 0
        s.last_flush = -1
        return Z_OK
      end
    elsif strm.avail_in == 0 && (flush <= old_flush) && (flush != Z_FINISH)
      strm.msg = @@z_errmsg[Z_errbase - Z_BUF_ERROR]
      return Z_BUF_ERROR
    end
    if (s.status == FINISH_STATE) && strm.avail_in != 0
      strm.msg = @@z_errmsg[Z_errbase - Z_BUF_ERROR]
      return Z_BUF_ERROR
    end

    if strm.avail_in != 0 || s.lookahead != 0 || ((flush != Z_NO_FLUSH) && (s.status != FINISH_STATE))
      bstate = send(@@configuration_table[s.level].func, s, flush)
      if (bstate == :finish_started) || (bstate == :finish_done)
        s.status = FINISH_STATE
      end
      if (bstate == :need_more) || (bstate == :finish_started)
        if strm.avail_out == 0
          s.last_flush = -1
        end
        return Z_OK
      end
      if (bstate == :block_done)
        if (flush == Z_PARTIAL_FLUSH)
          _tr_align(s)
        else
          _tr_stored_block(s, nil, (0), false)

          if (flush == Z_FULL_FLUSH)
            s.head = Array.new(s.hash_size,0)
            s.head[s.hash_size-1] = ZNIL
          end
        end
        flush_pending(strm)
        if strm.avail_out == 0
          s.last_flush = -1
          return Z_OK
        end

      end
    end
    if (flush != Z_FINISH)
      return Z_OK
    end

    if (s.wrap <= 0)
      return Z_STREAM_END
    end

    if (s.wrap == 2)
      s.pending_buf[s.pending] = strm.adler & 0xff
      s.pending+=1
      s.pending_buf[s.pending] = (strm.adler >> 8) & 0xff
      s.pending+=1
      s.pending_buf[s.pending] = (strm.adler >> 16) & 0xff
      s.pending+=1
      s.pending_buf[s.pending] = (strm.adler >> 24) & 0xff
      s.pending+=1
      s.pending_buf[s.pending] = strm.total_in & 0xff
      s.pending+=1
      s.pending_buf[s.pending] = (strm.total_in >> 8) & 0xff
      s.pending+=1
      s.pending_buf[s.pending] = (strm.total_in >> 16) & 0xff
      s.pending+=1
      s.pending_buf[s.pending] = (strm.total_in >> 24) & 0xff
      s.pending+=1
    else
      putShortMSB(s, (strm.adler >> 16))
      putShortMSB(s, (strm.adler & 0xffff))
    end
    flush_pending(strm)

    s.wrap = -s.wrap if (s.wrap > 0)
    return s.pending != 0 ? Z_OK : Z_STREAM_END
  end

  #
  def deflateEnd(strm)
    if (strm.nil? || strm.state.nil?)
      return Z_STREAM_ERROR
    end

    s = strm.state
    status = s.status
    if (status != INIT_STATE &&
        status != EXTRA_STATE &&
        status != NAME_STATE &&
        status != COMMENT_STATE &&
        status != HCRC_STATE &&
        status != BUSY_STATE &&
        status != FINISH_STATE)
      return Z_STREAM_ERROR
    end
    strm.state = nil

    if status == BUSY_STATE
      return Z_DATA_ERROR
    else
      return Z_OK
    end
  end

  # Copy the source state to the destination state.
  # To simplify the source, this is not supported for 16-bit MSDOS (which
  # doesn't have enough memory anyway to duplicate compression states).
  def deflateCopy(dest,source)
    if source.nil? || dest.nil? || source.state.nil?
      return Z_STREAM_ERROR
    end
    ss = (source.state)
    dest = source.dup

    ds = Deflate_state.new
    dest.state = ds
    ds = ss.dup
    ds.strm = dest

    ds.window = ss.window.dup
    ds.prev = ss.prev.dup
    ds.head = ss.head.dup
    ds.pending_buf = ss.pending_buf.dup
    ds.pending_buf.buffer = ss.pending_buf.buffer.dup

    ds.pending_out = Bytef.new(ds.pending_buf,ss.pending_out.offset)
    ds.d_buf = Posf.new(ds.pending_buf,ds.lit_bufsize)
    ds.l_buf = Bytef.new(ds.pending_buf,(1+2) * ds.lit_bufsize)

    ds.l_desc.dyn_tree = ds.dyn_ltree
    ds.d_desc.dyn_tree = ds.dyn_dtree
    ds.bl_desc.dyn_tree = ds.bl_tree

    return Z_OK
  end

  # Read a new buffer from the current input stream, update the adler32
  # and total number of bytes read.  All deflate() input goes through
  # this function so some applications may wish to modify it to avoid
  # allocating a large strm->next_in buffer and copying from it.
  # (See also flush_pending()).
  def read_buf(strm,buf,offset,size)
    len = strm.avail_in

    if (len > size)
      len = size
    end
    if len == 0
      return 0
    end

    strm.avail_in -= len

    if strm.state.wrap == 1
      strm.adler = adler32(strm.adler, strm.next_in.current,len)
    elsif (strm.state.wrap == 2)
      strm.adler = crc32(strm.adler,strm.next_in.current,len)
    end

    buf[offset,len] = strm.next_in.current[0,len]
    strm.next_in += len
    strm.total_in += len

    return len
  end

  # Initialize the "longest match" routines for a new zlib stream
  def lm_init(s)
    s.window_size = (2*s.w_size)

    s.head = Array.new(s.hash_size,0)
    s.head[s.hash_size-1] = ZNIL

    s.max_lazy_match   = @@configuration_table[s.level].max_lazy
    s.good_match       = @@configuration_table[s.level].good_length
    s.nice_match       = @@configuration_table[s.level].nice_length
    s.max_chain_length = @@configuration_table[s.level].max_chain

    s.strstart = 0
    s.block_start = (0)
    s.lookahead = 0
    s.prev_length = MIN_MATCH-1
    s.match_length = MIN_MATCH-1
    s.match_available = false
    s.ins_h = 0
  end

  # Set match_start to the longest match starting at the given string and
  # return its length. Matches shorter or equal to prev_length are discarded,
  # in which case the result is equal to prev_length and match_start is
  # garbage.
  # IN assertions: cur_match is the head of the hash chain for the current
  #   string (strstart) and its distance is <= MAX_DIST, and prev_length >= 1
  # OUT assertion: the match length is not greater than s->lookahead.
  def longest_match(s,cur_match)
    chain_length = s.max_chain_length
    scan = s.strstart
    best_len = s.prev_length
    nice_match = s.nice_match

    _MAX_DIST = s.w_size - MIN_LOOKAHEAD

    if s.strstart > (_MAX_DIST)
      limit = s.strstart - (_MAX_DIST)
    else
      limit = ZNIL
    end

    prev = s.prev
    wmask = s.w_mask

    strend = s.strstart + MAX_MATCH
    scan_end1  = s.window[scan + best_len-1]
    scan_end   = s.window[scan + best_len]

    if s.prev_length >= s.good_match
      chain_length >>= 2
    end

    if nice_match > s.lookahead
      nice_match = s.lookahead
    end

    begin
        match = cur_match

        if (s.window[match + best_len] != scan_end) ||
           (s.window[match + best_len-1] != scan_end1) ||
           (s.window[match] != s.window[scan])
        then
           cur_match = prev[cur_match & wmask]
           chain_length -= 1
           next
        end

        match += 1

        if s.window[match] != s.window[scan+1]
           cur_match = prev[cur_match & wmask]
           chain_length -= 1
           next
        end

        scan += 2
        match += 1

        # Seems redundant, but mimics the C code
        loop do
          break if s.window[scan+=1] != s.window[match+=1]
          break if s.window[scan+=1] != s.window[match+=1]
          break if s.window[scan+=1] != s.window[match+=1]
          break if s.window[scan+=1] != s.window[match+=1]
          break if s.window[scan+=1] != s.window[match+=1]
          break if s.window[scan+=1] != s.window[match+=1]
          break if s.window[scan+=1] != s.window[match+=1]
          break if s.window[scan+=1] != s.window[match+=1]
          break if scan >= strend
        end

        len = MAX_MATCH - (strend - scan)
        scan = strend
        scan -= MAX_MATCH

        if len > best_len
          s.match_start = cur_match
          best_len = len
          break if (len >= nice_match)
          scan_end1 = s.window[scan+best_len-1]
          scan_end  = s.window[scan+best_len]
        end

        cur_match = prev[cur_match & wmask]
        chain_length -= 1
    end until (cur_match <= limit) || chain_length == 0

    if best_len <= s.lookahead
      return best_len
    else
      return s.lookahead
    end
  end

   # Optimized version for level == 1 or strategy == Z_RLE only
  def longest_match_fast(s, cur_match)
    scan = Bytef.new(s.window,s.strstart)
    strend = Bytef.new(s.window,s.strstart + MAX_MATCH)

    match = Bytef.new(s.window,cur_match)

    return (MIN_MATCH-1) if (match[0] != scan[0] || match[1] != scan[1])

    scan += 2
    match += 2
    loop do
      scan += 1
      match += 1
      break if scan.get == match.get
      scan += 1
      match += 1
      break if scan.get == match.get
      scan += 1
      match += 1
      break if scan.get == match.get
      scan += 1
      match += 1
      break if scan.get == match.get
      scan += 1
      match += 1
      break if scan.get == match.get
      scan += 1
      match += 1
      break if scan.get == match.get
      scan += 1
      match += 1
      break if scan.get == match.get
      scan += 1
      match += 1
      break if scan.get == match.get
      break if (scan.offset >= strend.offset)
    end

    len = MAX_MATCH - (strend.offset - scan.offset)

    return (MIN_MATCH - 1) if (len < MIN_MATCH)

    s.match_start = cur_match
    return len <= s.lookahead ? len : s.lookahead
  end

  # Check that the match at match_start is indeed a match.
  def check_match(s,start, match,length)
  end

  # Fill the window when the lookahead becomes insufficient.
  # Updates strstart and lookahead.
  #
  # IN assertion: lookahead < MIN_LOOKAHEAD
  # OUT assertions: strstart <= window_size-MIN_LOOKAHEAD
  #    At least one byte has been read, or avail_in == 0; reads are
  #    performed for at least two bytes (required for the zip translate_eol
  #    option -- not supported here).
  def fill_window(s)
    wsize = s.w_size
    loop do
      more = s.window_size - s.lookahead - s.strstart

      if more == 0 && s.strstart == 0 && s.lookahead == 0
        more = wsize
      elsif (more == (-1))
          more -= 1
      end
      if (s.strstart >= wsize+ (wsize-MIN_LOOKAHEAD))
        s.window[0,wsize] = s.window[wsize,wsize]
        s.match_start -= wsize
        s.strstart -= wsize
        s.block_start -= wsize


        n = s.hash_size
        ap = n
        loop do
          ap -= 1
          m = s.head[ap]
          if (m >= wsize)
            s.head[ap] = m-wsize
          else
            s.head[ap] = ZNIL
          end
          n -= 1
          break if n == 0
        end

        n = wsize
        ap = n
        loop do
          ap-=1
          m = s.prev[ap]
          if (m >= wsize)
            s.prev[ap] = (m-wsize)
          else
            s.prev[ap] = ZNIL
          end
          n-=1
          break if n == 0
        end
        more += wsize
      end
      return if s.strm.avail_in == 0

      n = read_buf(s.strm, s.window,(s.strstart + s.lookahead),more)
      s.lookahead += n

      if (s.lookahead >= MIN_MATCH)
        s.ins_h = s.window[s.strstart].ord
        s.ins_h = ((s.ins_h << s.hash_shift) ^ s.window[s.strstart+1].ord) & s.hash_mask
      end
      break if (s.lookahead >= MIN_LOOKAHEAD) || s.strm.avail_in == 0
    end
  end

  # Flush the current block, with given end-of-file flag.
  # IN assertion: strstart is set to the end of the current match.
  def FLUSH_BLOCK_ONLY(s,eof)
    if (s.block_start >= (0))
      _tr_flush_block(s, s.window[(s.block_start)..-1],
                    ((s.strstart) - s.block_start), eof)
    else
      _tr_flush_block(s, nil,
                    ((s.strstart) - s.block_start), eof)
    end

    s.block_start = s.strstart
    flush_pending(s.strm)
  end

  # Copy without compression as much as possible from the input stream, return
  # the current block state.
  # This function does not insert new strings in the dictionary since
  # uncompressible data is probably not useful. This function is used
  # only for the level=0 compression option.
  # NOTE: this function should be optimized to avoid extra copying from
  # window to pending_buf.
  def deflate_stored(s,flush)
    max_block_size = 0xffff
    if (max_block_size > s.pending_buf_size - 5)
      max_block_size = s.pending_buf_size - 5
    end

    loop do
      if (s.lookahead <= 1)
        fill_window(s)
        if s.lookahead == 0 && (flush == Z_NO_FLUSH)
          return :need_more
        end

        break if s.lookahead == 0
      end
      s.strstart += s.lookahead
      s.lookahead = 0

      max_start = s.block_start + max_block_size
      if s.strstart == 0 || ((s.strstart) >= max_start)
        s.lookahead = (s.strstart - max_start)
        s.strstart = (max_start)
        FLUSH_BLOCK_ONLY(s, false)
        if s.strm.avail_out == 0
          return :need_more
        end
      end

      if (s.strstart - (s.block_start) >=
          s.w_size-MIN_LOOKAHEAD)
        FLUSH_BLOCK_ONLY(s, false)
        if s.strm.avail_out == 0
          return :need_more
        end
      end
    end

    FLUSH_BLOCK_ONLY(s, flush == Z_FINISH)
    if s.strm.avail_out == 0
      if flush == Z_FINISH
        return :finish_started
      else
        return :need_more
      end
    end

    if flush == Z_FINISH
      return :finish_done
    else
      return :block_done
    end
  end

  # Compress as much as possible from the input stream, return the current
  # block state.
  # This function does not perform lazy evaluation of matches and inserts
  # new strings in the dictionary only for unmatched strings or for short
  # matches. It is used only for the fast compression options.
  def deflate_fast(s,flush)
    hash_head = ZNIL
    loop do

      if (s.lookahead < MIN_LOOKAHEAD)
        fill_window(s)
        if (s.lookahead < MIN_LOOKAHEAD) && (flush == Z_NO_FLUSH)
          return :need_more
        end

        break if s.lookahead == 0
      end

      if (s.lookahead >= MIN_MATCH)
        hash_head = INSERT_STRING(s, s.strstart, hash_head)
      end

      if (hash_head != ZNIL) &&
       (s.strstart - hash_head <= (s.w_size-MIN_LOOKAHEAD))
        if (s.strategy != Z_HUFFMAN_ONLY && s.strategy != Z_RLE)
          s.match_length = longest_match(s, hash_head)
        elsif(s.strategy == Z_RLE && s.srstart - hash_end == 1)
          s.match_length = longest_match_fast(s, hash_head)
        end
      end
      if (s.match_length >= MIN_MATCH)
        bflush = _tr_tally(s, s.strstart - s.match_start,
                        s.match_length - MIN_MATCH)

        s.lookahead -= s.match_length
        if (s.match_length <= s.max_lazy_match) && (s.lookahead >= MIN_MATCH)
          s.match_length -= 1
          loop do
            s.strstart += 1
            hash_head = INSERT_STRING(s, s.strstart, hash_head)
            s.match_length -= 1
            break if s.match_length == 0
          end
          s.strstart += 1
        else
          s.strstart += s.match_length
          s.match_length = 0
          s.ins_h = s.window[s.strstart].ord
          s.ins_h = (( s.ins_h << s.hash_shift) ^
                     s.window[s.strstart+1].ord) & s.hash_mask
        end
      else
        bflush = _tr_tally(s, 0, s.window[s.strstart].ord)

        s.lookahead-=1
        s.strstart+=1
      end
      if bflush
        FLUSH_BLOCK_ONLY(s, false)
        if s.strm.avail_out == 0
          return :need_more
        end
      end
    end
    FLUSH_BLOCK_ONLY(s, flush == Z_FINISH)
    if s.strm.avail_out == 0
      if flush == Z_FINISH
        return :finish_started
      else
        return :need_more
      end
    end

    if flush == Z_FINISH
      return :finish_done
    else
      return :block_done
    end
  end

  # Same as above, but achieves better compression. We use a lazy
  # evaluation for matches: a match is finally adopted only if there is
  # no better match at the next window position.
  def deflate_slow(s,flush)
    hash_head = ZNIL

    loop do
      if (s.lookahead < MIN_LOOKAHEAD)
        fill_window(s)
        if (s.lookahead < MIN_LOOKAHEAD) && (flush == Z_NO_FLUSH)
          return :need_more
        end

        break if s.lookahead == 0
      end

      if (s.lookahead >= MIN_MATCH)
        hash_head = INSERT_STRING(s, s.strstart, hash_head)
      end

      s.prev_length = s.match_length
      s.prev_match = s.match_start
      s.match_length = MIN_MATCH-1

      if (hash_head != ZNIL) && (s.prev_length < s.max_lazy_match) &&
       (s.strstart - hash_head <= (s.w_size-MIN_LOOKAHEAD))

        if (s.strategy != Z_HUFFMAN_ONLY && s.strategy != Z_RLE)
          s.match_length = longest_match(s, hash_head)
        elsif(s.strategy == Z_RLE && s.strstart - hash_head == 1)
          s.match_length = longest_match_fast(s, hash_head)
        end

        if (s.match_length <= 5) && ((s.strategy == Z_FILTERED) ||
             ((s.match_length == MIN_MATCH) &&
              (s.strstart - s.match_start > TOO_FAR)))
            s.match_length = MIN_MATCH-1
        end
      end
      if (s.prev_length >= MIN_MATCH) && (s.match_length <= s.prev_length)
        max_insert = s.strstart + s.lookahead - MIN_MATCH

        bflush = _tr_tally(s, s.strstart - 1 - s.prev_match,
                          s.prev_length - MIN_MATCH)
        s.lookahead -= s.prev_length-1
        s.prev_length -= 2
        loop do
          s.strstart+=1
          if (s.strstart <= max_insert)
            hash_head = INSERT_STRING(s, s.strstart, hash_head)
          end
          s.prev_length-=1
          break if s.prev_length == 0
        end
        s.match_available = false
        s.match_length = MIN_MATCH-1
        s.strstart+=1
        if (bflush)
          FLUSH_BLOCK_ONLY(s, false)
          if s.strm.avail_out == 0
            return :need_more
          end
        end
      elsif (s.match_available)
          bflush = _tr_tally(s, 0, s.window[s.strstart-1].ord)
          if bflush
            FLUSH_BLOCK_ONLY(s, false)
          end
          s.strstart+=1
          s.lookahead-=1
          if s.strm.avail_out == 0
            return :need_more
          end
      else
          s.match_available = true
          s.strstart+=1
          s.lookahead-=1
      end
    end
    if (s.match_available)
      _tr_tally(s, 0, s.window[s.strstart-1].ord)
      s.match_available = false
    end
    FLUSH_BLOCK_ONLY(s, flush == Z_FINISH)
    if s.strm.avail_out == 0
      if flush == Z_FINISH
        return :finish_started
      else
        return :need_more
      end
    end
    if flush == Z_FINISH
      return :finish_done
    else
      return :block_done
    end
  end

  LENGTH_CODES = 29
  LITERALS = 256
  L_CODES = (LITERALS+1+LENGTH_CODES)
  D_CODES = 30
  BL_CODES = 19
  HEAP_SIZE = (2*L_CODES+1)
  MAX_BITS = 15

  INIT_STATE =  42
  EXTRA_STATE =  69
  NAME_STATE =  73
  COMMENT_STATE = 91
  HCRC_STATE =  103
  BUSY_STATE =  113
  FINISH_STATE = 666

  Ct_data = Struct.new(:fc,:dl)

  Static_tree_desc = Struct.new(:static_tree,:extra_bits,:extra_base,:elems,:max_length)

  Tree_desc = Struct.new(:dyn_tree,:max_code,:stat_desc)

  Deflate_state = Struct.new(:strm,:status,:pending_buf,:pending_buf_size,:pending_out,
    :pending,:wrap,:gzhead,:gzindex,:method,:last_flush,:w_size,:w_bits,:w_mask,:window,
    :window_size,:prev,:head,:ins_h,:hash_size,:hash_bits,:hash_mask,:hash_shift,:block_start,
    :match_length,:prev_match,:match_available,:strstart,:match_start,:lookahead,:prev_length,
    :max_chain_length,:level,:strategy,:good_match,:nice_match,:dyn_ltree,
    :dyn_dtree,:bl_tree,:l_desc,:d_desc,:bl_desc,:bl_count,:heap,:heap_len,:heap_max,:depth,
    :l_buf,:lit_bufsize,:last_lit,:d_buf,:opt_len,:static_len,:matches,:last_eob_len,
    :bi_buf,:bi_valid,:max_lazy_match,:max_insert_length)

  DIST_CODE_LEN = 512

  @@static_ltree = [
    [( 12),( 8)], [(140),( 8)], [( 76),( 8)],
    [(204),( 8)], [( 44),( 8)], [(172),( 8)],
    [(108),( 8)], [(236),( 8)], [( 28),( 8)],
    [(156),( 8)], [( 92),( 8)], [(220),( 8)],
    [( 60),( 8)], [(188),( 8)], [(124),( 8)],
    [(252),( 8)], [(  2),( 8)], [(130),( 8)],
    [( 66),( 8)], [(194),( 8)], [( 34),( 8)],
    [(162),( 8)], [( 98),( 8)], [(226),( 8)],
    [( 18),( 8)], [(146),( 8)], [( 82),( 8)],
    [(210),( 8)], [( 50),( 8)], [(178),( 8)],
    [(114),( 8)], [(242),( 8)], [( 10),( 8)],
    [(138),( 8)], [( 74),( 8)], [(202),( 8)],
    [( 42),( 8)], [(170),( 8)], [(106),( 8)],
    [(234),( 8)], [( 26),( 8)], [(154),( 8)],
    [( 90),( 8)], [(218),( 8)], [( 58),( 8)],
    [(186),( 8)], [(122),( 8)], [(250),( 8)],
    [(  6),( 8)], [(134),( 8)], [( 70),( 8)],
    [(198),( 8)], [( 38),( 8)], [(166),( 8)],
    [(102),( 8)], [(230),( 8)], [( 22),( 8)],
    [(150),( 8)], [( 86),( 8)], [(214),( 8)],
    [( 54),( 8)], [(182),( 8)], [(118),( 8)],
    [(246),( 8)], [( 14),( 8)], [(142),( 8)],
    [( 78),( 8)], [(206),( 8)], [( 46),( 8)],
    [(174),( 8)], [(110),( 8)], [(238),( 8)],
    [( 30),( 8)], [(158),( 8)], [( 94),( 8)],
    [(222),( 8)], [( 62),( 8)], [(190),( 8)],
    [(126),( 8)], [(254),( 8)], [(  1),( 8)],
    [(129),( 8)], [( 65),( 8)], [(193),( 8)],
    [( 33),( 8)], [(161),( 8)], [( 97),( 8)],
    [(225),( 8)], [( 17),( 8)], [(145),( 8)],
    [( 81),( 8)], [(209),( 8)], [( 49),( 8)],
    [(177),( 8)], [(113),( 8)], [(241),( 8)],
    [(  9),( 8)], [(137),( 8)], [( 73),( 8)],
    [(201),( 8)], [( 41),( 8)], [(169),( 8)],
    [(105),( 8)], [(233),( 8)], [( 25),( 8)],
    [(153),( 8)], [( 89),( 8)], [(217),( 8)],
    [( 57),( 8)], [(185),( 8)], [(121),( 8)],
    [(249),( 8)], [(  5),( 8)], [(133),( 8)],
    [( 69),( 8)], [(197),( 8)], [( 37),( 8)],
    [(165),( 8)], [(101),( 8)], [(229),( 8)],
    [( 21),( 8)], [(149),( 8)], [( 85),( 8)],
    [(213),( 8)], [( 53),( 8)], [(181),( 8)],
    [(117),( 8)], [(245),( 8)], [( 13),( 8)],
    [(141),( 8)], [( 77),( 8)], [(205),( 8)],
    [( 45),( 8)], [(173),( 8)], [(109),( 8)],
    [(237),( 8)], [( 29),( 8)], [(157),( 8)],
    [( 93),( 8)], [(221),( 8)], [( 61),( 8)],
    [(189),( 8)], [(125),( 8)], [(253),( 8)],
    [( 19),( 9)], [(275),( 9)], [(147),( 9)],
    [(403),( 9)], [( 83),( 9)], [(339),( 9)],
    [(211),( 9)], [(467),( 9)], [( 51),( 9)],
    [(307),( 9)], [(179),( 9)], [(435),( 9)],
    [(115),( 9)], [(371),( 9)], [(243),( 9)],
    [(499),( 9)], [( 11),( 9)], [(267),( 9)],
    [(139),( 9)], [(395),( 9)], [( 75),( 9)],
    [(331),( 9)], [(203),( 9)], [(459),( 9)],
    [( 43),( 9)], [(299),( 9)], [(171),( 9)],
    [(427),( 9)], [(107),( 9)], [(363),( 9)],
    [(235),( 9)], [(491),( 9)], [( 27),( 9)],
    [(283),( 9)], [(155),( 9)], [(411),( 9)],
    [( 91),( 9)], [(347),( 9)], [(219),( 9)],
    [(475),( 9)], [( 59),( 9)], [(315),( 9)],
    [(187),( 9)], [(443),( 9)], [(123),( 9)],
    [(379),( 9)], [(251),( 9)], [(507),( 9)],
    [(  7),( 9)], [(263),( 9)], [(135),( 9)],
    [(391),( 9)], [( 71),( 9)], [(327),( 9)],
    [(199),( 9)], [(455),( 9)], [( 39),( 9)],
    [(295),( 9)], [(167),( 9)], [(423),( 9)],
    [(103),( 9)], [(359),( 9)], [(231),( 9)],
    [(487),( 9)], [( 23),( 9)], [(279),( 9)],
    [(151),( 9)], [(407),( 9)], [( 87),( 9)],
    [(343),( 9)], [(215),( 9)], [(471),( 9)],
    [( 55),( 9)], [(311),( 9)], [(183),( 9)],
    [(439),( 9)], [(119),( 9)], [(375),( 9)],
    [(247),( 9)], [(503),( 9)], [( 15),( 9)],
    [(271),( 9)], [(143),( 9)], [(399),( 9)],
    [( 79),( 9)], [(335),( 9)], [(207),( 9)],
    [(463),( 9)], [( 47),( 9)], [(303),( 9)],
    [(175),( 9)], [(431),( 9)], [(111),( 9)],
    [(367),( 9)], [(239),( 9)], [(495),( 9)],
    [( 31),( 9)], [(287),( 9)], [(159),( 9)],
    [(415),( 9)], [( 95),( 9)], [(351),( 9)],
    [(223),( 9)], [(479),( 9)], [( 63),( 9)],
    [(319),( 9)], [(191),( 9)], [(447),( 9)],
    [(127),( 9)], [(383),( 9)], [(255),( 9)],
    [(511),( 9)], [(  0),( 7)], [( 64),( 7)],
    [( 32),( 7)], [( 96),( 7)], [( 16),( 7)],
    [( 80),( 7)], [( 48),( 7)], [(112),( 7)],
    [(  8),( 7)], [( 72),( 7)], [( 40),( 7)],
    [(104),( 7)], [( 24),( 7)], [( 88),( 7)],
    [( 56),( 7)], [(120),( 7)], [(  4),( 7)],
    [( 68),( 7)], [( 36),( 7)], [(100),( 7)],
    [( 20),( 7)], [( 84),( 7)], [( 52),( 7)],
    [(116),( 7)], [(  3),( 8)], [(131),( 8)],
    [( 67),( 8)], [(195),( 8)], [( 35),( 8)],
    [(163),( 8)], [( 99),( 8)], [(227),( 8)]
    ].map{|i| Ct_data.new(*i)}

  @@static_dtree = [
    [( 0),(5)], [(16),(5)], [( 8),(5)],
    [(24),(5)], [( 4),(5)], [(20),(5)],
    [(12),(5)], [(28),(5)], [( 2),(5)],
    [(18),(5)], [(10),(5)], [(26),(5)],
    [( 6),(5)], [(22),(5)], [(14),(5)],
    [(30),(5)], [( 1),(5)], [(17),(5)],
    [( 9),(5)], [(25),(5)], [( 5),(5)],
    [(21),(5)], [(13),(5)], [(29),(5)],
    [( 3),(5)], [(19),(5)], [(11),(5)],
    [(27),(5)], [( 7),(5)], [(23),(5)]
    ].map{|i| Ct_data.new(*i)}

  @@_dist_code = [
     0,  1,  2,  3,  4,  4,  5,  5,  6,  6,  6,  6,  7,  7,  7,  7,  8,  8,  8,  8,
     8,  8,  8,  8,  9,  9,  9,  9,  9,  9,  9,  9, 10, 10, 10, 10, 10, 10, 10, 10,
    10, 10, 10, 10, 10, 10, 10, 10, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11, 11,
    11, 11, 11, 11, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12,
    12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 12, 13, 13, 13, 13,
    13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13, 13,
    13, 13, 13, 13, 13, 13, 13, 13, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,
    14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,
    14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14,
    14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 14, 15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,
    15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15,  0,  0, 16, 17,
    18, 18, 19, 19, 20, 20, 20, 20, 21, 21, 21, 21, 22, 22, 22, 22, 22, 22, 22, 22,
    23, 23, 23, 23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
    24, 24, 24, 24, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
    26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
    26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27,
    27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27,
    27, 27, 27, 27, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
    28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
    28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28, 28,
    28, 28, 28, 28, 28, 28, 28, 28, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29,
    29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29, 29]
  @@_length_code = [
     0,  1,  2,  3,  4,  5,  6,  7,  8,  8,  9,  9, 10, 10, 11, 11, 12, 12, 12, 12,
    13, 13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15, 16, 16, 16, 16, 16, 16, 16, 16,
    17, 17, 17, 17, 17, 17, 17, 17, 18, 18, 18, 18, 18, 18, 18, 18, 19, 19, 19, 19,
    19, 19, 19, 19, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20, 20,
    21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 21, 22, 22, 22, 22,
    22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 23, 23, 23, 23, 23, 23, 23, 23,
    23, 23, 23, 23, 23, 23, 23, 23, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
    24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
    25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25,
    25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 25, 26, 26, 26, 26, 26, 26, 26, 26,
    26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
    26, 26, 26, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27,
    27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 28]
  @@base_length = [
    0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56,
    64, 80, 96, 112, 128, 160, 192, 224, 0]
  @@base_dist = [
        0,     1,     2,     3,     4,     6,     8,    12,    16,    24,
       32,    48,    64,    96,   128,   192,   256,   384,   512,   768,
     1024,  1536,  2048,  3072,  4096,  6144,  8192, 12288, 16384, 24576]

  MAX_BL_BITS = 7

  END_BLOCK = 256

  REP_3_6 = 16

  REPZ_3_10 = 17

  REPZ_11_138 = 18

  @@extra_lbits = [0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,0]

  @@extra_dbits = [0,0,0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13]

  @@extra_blbits = [0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,3,7]

  @@bl_order = [16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15]

  Buf_size = (8 * 2*1)

  @@static_l_desc = Static_tree_desc.new(@@static_ltree,@@extra_lbits,LITERALS+1,L_CODES,MAX_BITS)

  @@static_d_desc = Static_tree_desc.new(@@static_dtree,@@extra_dbits,0,D_CODES,MAX_BITS)

  @@static_bl_desc = Static_tree_desc.new(nil,@@extra_blbits,0,BL_CODES,MAX_BL_BITS)

  # Send a value on a given number of bits.
  # IN assertion: length <= 16 and value fits in length bits.
  def send_bits(s,value,length)
    if (s.bi_valid > Buf_size - length)
      s.bi_buf |= (value << s.bi_valid)
      s.bi_buf &= 0xffff
      s.pending_buf[s.pending] = s.bi_buf & 0xff
      s.pending += 1
      s.pending_buf[s.pending] = s.bi_buf >> 8
      s.pending += 1
      s.bi_buf = value >> (Buf_size - s.bi_valid)
      s.bi_valid += length - Buf_size
    else
      s.bi_buf |= (value << s.bi_valid)
      s.bi_valid += length
    end
  end

  # Reverse the first len bits of a code, using straightforward code (a faster
  # method would use a table)
  # IN assertion: 1 <= len <= 15
  def bi_reverse(code,len)
    res = 0
    loop do
      res |= (code & 1)
      code >>= 1
      res <<= 1
      len -= 1
      break if len<=0
    end
    res >> 1
  end

  # Generate the codes for a given tree and bit counts (which need not be
  # optimal).
  # IN assertion: the array bl_count contains the bit length statistics for
  # the given tree and the field len is set for all tree elements.
  # OUT assertion: the field code is set for all tree elements of non
  #     zero code length.
  def gen_codes(tree,max_code,bl_count)
    next_code = Array.new(MAX_BITS+1,0)
    code = 0
    for bits in 1 .. MAX_BITS
      code = ((code + bl_count[bits-1]) << 1)
      next_code[bits] = code
    end

    for n in 0..max_code
      len = tree[n].dl
      next if len == 0
      tree[n].fc = bi_reverse(next_code[len], len)
      next_code[len] += 1
    end
  end

  # Initialize a new block.
  def init_block(s)
    for n in 0 ... L_CODES
      s.dyn_ltree[n].fc = 0
    end
    for n in 0 ... D_CODES
      s.dyn_dtree[n].fc = 0
    end
    for n in 0 ... BL_CODES
      s.bl_tree[n].fc = 0
    end
    s.dyn_ltree[END_BLOCK].fc = 1
    s.static_len = 0
    s.opt_len = 0
    s.matches = 0
    s.last_lit = 0
  end

  SMALLEST = 1

  # Initialize the tree data structures for a new zlib stream.
  def _tr_init(s)
    s.l_desc = Tree_desc.new
    s.l_desc.dyn_tree = s.dyn_ltree
    s.l_desc.stat_desc = @@static_l_desc
    s.d_desc = Tree_desc.new
    s.d_desc.dyn_tree = s.dyn_dtree
    s.d_desc.stat_desc = @@static_d_desc
    s.bl_desc = Tree_desc.new
    s.bl_desc.dyn_tree = s.bl_tree
    s.bl_desc.stat_desc = @@static_bl_desc

    s.bi_buf = 0
    s.bi_valid = 0
    s.last_eob_len = 8

    init_block(s)
  end

  # Restore the heap property by moving down the tree starting at node k,
  # exchanging a node with the smallest of its two sons if necessary, stopping
  # when the heap property is re-established (each father smaller than its
  # two sons).
  def pqdownheap(s,tree,k)
    v = s.heap[k]
    j = k << 1
    while (j <= s.heap_len)
      if (j < s.heap_len) &&
        ( (tree[s.heap[j+1]].fc < tree[s.heap[j]].fc) ||
        ((tree[s.heap[j+1]].fc == tree[s.heap[j]].fc) &&
         (s.depth[s.heap[j+1]] <= s.depth[s.heap[j]])) )
        j+=1
      end
      break if ( (tree[v].fc < tree[s.heap[j]].fc) ||
         ((tree[v].fc == tree[s.heap[j]].fc) &&
          (s.depth[v] <= s.depth[s.heap[j]])) )

      s.heap[k] = s.heap[j]
      k = j

      j <<= 1
    end
    s.heap[k] = v
  end

  # Compute the optimal bit lengths for a tree and update the total bit length
  # for the current block.
  # IN assertion: the fields freq and dad are set, heap[heap_max] and
  #    above are the tree nodes sorted by increasing frequency.
  # OUT assertions: the field len is set to the optimal bit length, the
  #     array bl_count contains the frequencies for each bit length.
  #     The length opt_len is updated; static_len is also updated if stree is
  #     not null.
  def gen_bitlen(s,desc)
    tree = desc.dyn_tree
    max_code = desc.max_code
    stree = desc.stat_desc.static_tree
    extra = desc.stat_desc.extra_bits
    base = desc.stat_desc.extra_base
    max_length = desc.stat_desc.max_length
    overflow = 0

    for bits in 0 .. MAX_BITS
      s.bl_count[bits] = 0
    end

    tree[s.heap[s.heap_max]].dl = 0

    for h in s.heap_max+1 ... HEAP_SIZE
      n = s.heap[h]
      bits = tree[tree[n].dl].dl + 1
      if (bits > max_length)
        bits = max_length
        overflow+=1
      end
      tree[n].dl = bits

      next if (n > max_code)

      s.bl_count[bits]+=1
      xbits = 0
      xbits = extra[n-base] if (n >= base)
      f = tree[n].fc
      s.opt_len += (f) * (bits + xbits)
      if stree
        s.static_len += (f) * (stree[n].dl + xbits)
      end
    end

    return if overflow == 0

    loop do
      bits = max_length-1
      bits -= 1 while s.bl_count[bits] == 0
      s.bl_count[bits] -= 1
      s.bl_count[bits+1] += 2
      s.bl_count[max_length] -= 1

      overflow -= 2
      break if (overflow <= 0)
    end

    h = HEAP_SIZE
    max_length.downto(1) do |nbits|
      n = s.bl_count[nbits]
      while n != 0
        h -= 1
        m = s.heap[h]
        next if (m > max_code)
        if (tree[m].dl != nbits)
          s.opt_len += (nbits - tree[m].dl) * tree[m].fc
          tree[m].dl = nbits
        end
        n-=1
      end
    end
  end

  # Construct one Huffman tree and assigns the code bit strings and lengths.
  # Update the total bit length for the current block.
  # IN assertion: the field freq is set for all tree elements.
  # OUT assertions: the fields len and code are set to the optimal bit length
  #     and corresponding code. The length opt_len is updated; static_len is
  #     also updated if stree is not null. The field max_code is set.
  def build_tree(s,desc)
    tree = desc.dyn_tree
    stree = desc.stat_desc.static_tree
    elems = desc.stat_desc.elems
    max_code = -1

    s.heap_len = 0
    s.heap_max = HEAP_SIZE

    for n in 0 ... elems
      if tree[n].fc != 0
        max_code = n
        s.heap_len+=1
        s.heap[s.heap_len] = n
        s.depth[n] = 0
      else
        tree[n].dl = 0
      end
    end

    while (s.heap_len < 2)
      s.heap_len+=1
      if (max_code < 2)
        max_code+=1
        s.heap[s.heap_len] = max_code
        node = max_code
      else
        s.heap[s.heap_len] = 0
        node = 0
      end
      tree[node].fc = 1
      s.depth[node] = 0
      s.opt_len-=1
      if stree
        s.static_len -= stree[node].dl
      end
    end
    desc.max_code = max_code

    (s.heap_len / 2).downto(1) do |nheap|
      pqdownheap(s, tree, nheap)
    end

    node = elems
    loop do
      n = s.heap[SMALLEST]
      s.heap[SMALLEST] = s.heap[s.heap_len]
      s.heap_len-=1
      pqdownheap(s, tree, SMALLEST)

      m = s.heap[SMALLEST]

      s.heap_max-=1
      s.heap[s.heap_max] = n
      s.heap_max-=1
      s.heap[s.heap_max] = m
      tree[node].fc = tree[n].fc + tree[m].fc
      if (s.depth[n] >= s.depth[m])
        s.depth[node] = s.depth[n] + 1
      else
        s.depth[node] = s.depth[m] + 1
      end
      tree[m].dl = node
      tree[n].dl = node

      s.heap[SMALLEST] = node
      node+=1
      pqdownheap(s, tree, SMALLEST)
      break if (s.heap_len < 2)
    end

    s.heap_max-=1
    s.heap[s.heap_max] = s.heap[SMALLEST]

    gen_bitlen(s, desc)

    gen_codes(tree, max_code, s.bl_count)
  end

  # Scan a literal or distance tree to determine the frequencies of the codes
  # in the bit length tree.
  def scan_tree(s,tree,max_code)
    prevlen = -1
    nextlen = tree[0].dl
    count = 0
    max_count = 7
    min_count = 4

    if nextlen == 0
      max_count = 138
      min_count = 3
    end
    tree[max_code+1].dl = 0xffff

    for n in 0 .. max_code
      curlen = nextlen
      nextlen = tree[n+1].dl
      count+=1
      if (count < max_count) && (curlen == nextlen)
        next
      elsif (count < min_count)
        s.bl_tree[curlen].fc += count
      elsif curlen != 0
        if (curlen != prevlen)
          s.bl_tree[curlen].fc+=1
        end
        s.bl_tree[REP_3_6].fc+=1
      elsif (count <= 10)
        s.bl_tree[REPZ_3_10].fc+=1
      else
        s.bl_tree[REPZ_11_138].fc+=1
      end
      count = 0
      prevlen = curlen
      if nextlen == 0
        max_count = 138
        min_count = 3
      elsif (curlen == nextlen)
        max_count = 6
        min_count = 3
      else
        max_count = 7
        min_count = 4
      end
    end
  end

  # Send a literal or distance tree in compressed form, using the codes in
  # bl_tree.
  def send_tree(s,tree,max_code)
    prevlen = -1
    nextlen = tree[0].dl
    count = 0
    max_count = 7
    min_count = 4

    if nextlen == 0
      max_count = 138
      min_count = 3
    end
    for n in 0 .. max_code
      curlen = nextlen
      nextlen = tree[n+1].dl
      count+=1
      if (count < max_count) && (curlen == nextlen)
        next
      elsif (count < min_count)
        loop do
          send_bits(s, s.bl_tree[curlen].fc, s.bl_tree[curlen].dl)
          count-=1
          break if count == 0
        end
      elsif curlen != 0
        if (curlen != prevlen)
          send_bits(s, s.bl_tree[curlen].fc, s.bl_tree[curlen].dl)
          count-=1
        end
        send_bits(s, s.bl_tree[REP_3_6].fc, s.bl_tree[REP_3_6].dl)
        send_bits(s, count-3, 2)
      elsif (count <= 10)
        send_bits(s, s.bl_tree[REPZ_3_10].fc, s.bl_tree[REPZ_3_10].dl)
        send_bits(s, count-3, 3)
      else
        send_bits(s, s.bl_tree[REPZ_11_138].fc, s.bl_tree[REPZ_11_138].dl)
        send_bits(s, count-11, 7)
      end
      count = 0
      prevlen = curlen
      if nextlen == 0
        max_count = 138
        min_count = 3
      elsif (curlen == nextlen)
        max_count = 6
        min_count = 3
      else
        max_count = 7
        min_count = 4
      end
    end
  end

  # Construct the Huffman tree for the bit lengths and return the index in
  # bl_order of the last bit length code to send.
  def build_bl_tree(s)
    scan_tree(s, s.dyn_ltree, s.l_desc.max_code)
    scan_tree(s, s.dyn_dtree, s.d_desc.max_code)

    build_tree(s, s.bl_desc)

    max_blindex = 0
    (BL_CODES-1).downto(3) do |i|
      max_blindex = i
      break if s.bl_tree[@@bl_order[i]].dl != 0
    end
    s.opt_len += 3*(max_blindex+1) + 5+5+4

    max_blindex
  end

  # Send the header for a block using dynamic Huffman trees: the counts, the
  # lengths of the bit length codes, the literal tree and the distance tree.
  # IN assertion: lcodes >= 257, dcodes >= 1, blcodes >= 4.
  def send_all_trees(s,lcodes,dcodes,blcodes)
    send_bits(s, lcodes-257, 5)
    send_bits(s, dcodes-1,   5)
    send_bits(s, blcodes-4,  4)
    for rank in 0 ... blcodes
      send_bits(s, s.bl_tree[@@bl_order[rank]].dl, 3)
    end

    send_tree(s, s.dyn_ltree, lcodes-1)

    send_tree(s, s.dyn_dtree, dcodes-1)
  end

  # Flush the bit buffer and align the output on a byte boundary
  def bi_windup(s)
    if (s.bi_valid > 8)
      s.pending_buf[s.pending] = s.bi_buf & 0xff
      s.pending+=1
      s.pending_buf[s.pending] = s.bi_buf >> 8
      s.pending+=1
    elsif (s.bi_valid > 0)
      s.pending_buf[s.pending] = s.bi_buf
      s.pending+=1
    end
    s.bi_buf = 0
    s.bi_valid = 0
  end

  # Copy a stored block, storing first the length and its
  # one's complement if requested.
  def copy_block(s,buf,len,header)
    bi_windup(s)
    s.last_eob_len = 8

    if header
      s.pending_buf[s.pending] = len & 0xff
      s.pending+=1
      s.pending_buf[s.pending] = len >> 8
      s.pending+=1
      s.pending_buf[s.pending] = ((~len) & 0xff)
      s.pending+=1
      s.pending_buf[s.pending] = ((~len) >> 8) & 0xff
      s.pending+=1
    end
    i = 0
    while len != 0
      len-=1
      s.pending_buf[s.pending] = buf[i].ord
      i+=1
      s.pending+=1
    end
  end

  # Send a stored block
  def _tr_stored_block(s,buf,stored_len,eof)
    send_bits(s, (STORED_BLOCK << 1) + (eof ? 1 : 0), 3)

    copy_block(s, buf, stored_len, true)
  end

  # Flush the bit buffer, keeping at most 7 bits in it.
  def bi_flush(s)
    if (s.bi_valid == 16)
      s.pending_buf[s.pending] = (s.bi_buf & 0xff)
      s.pending += 1
      s.pending_buf[s.pending] = (s.bi_buf >> 8)
      s.pending += 1

      s.bi_buf = 0
      s.bi_valid = 0
    elsif (s.bi_valid >= 8)
      s.pending_buf[s.pending] = s.bi_buf
      s.pending += 1

      s.bi_buf >>= 8
      s.bi_valid -= 8
    end
  end

  # Send one empty static block to give enough lookahead for inflate.
  # This takes 10 bits, of which 7 may remain in the bit buffer.
  # The current inflate code requires 9 bits of lookahead. If the
  # last two codes for the previous block (real code plus EOB) were coded
  # on 5 bits or less, inflate may have only 5+3 bits of lookahead to decode
  # the last real code. In this case we send two empty static blocks instead
  # of one. (There are no problems if the previous block is stored or fixed.)
  # To simplify the code, we assume the worst case of last real code encoded
  # on one bit only.
  def _tr_align(s)
    send_bits(s, STATIC_TREES << 1, 3)
    send_bits(s, @@static_ltree[END_BLOCK].fc, @@static_ltree[END_BLOCK].dl)

    bi_flush(s)
    if (1 + s.last_eob_len + 10 - s.bi_valid < 9)
      send_bits(s, STATIC_TREES << 1, 3)
      send_bits(s, @@static_ltree[END_BLOCK].fc, @@static_ltree[END_BLOCK].dl)
      bi_flush(s)
    end
    s.last_eob_len = 7
  end

  # Set the data type to BINARY or TEXT, using a crude approximation:
  # set it to Z_TEXT if all symbols are either printable characters (33 to 255)
  # or white spaces (9 to 13, or 32); or set it to Z_BINARY otherwise.
  # IN assertion: the fields Freq of dyn_ltree are set.
  def set_data_type(s)
    for n in 0 ... 9
      break if s.dyn_ltree[n].fc != 0
    end
    if n == 9
        for n in 14 ... 32
            break if s.dyn_ltree[n].fc != 0
        end
    end
    s.strm.data_type = (n == 32) ? Z_TEXT : Z_BINARY
  end

  # Send the block data compressed using the given Huffman trees
  def compress_block(s,ltree,dtree)
    lx = 0
    if s.last_lit != 0
      loop do
        dist = s.d_buf[lx]
        lc = s.l_buf[lx]
        lx+=1
        if dist == 0
          send_bits(s, ltree[lc].fc, ltree[lc].dl)
        else
          code = @@_length_code[lc]
          send_bits(s, ltree[code+LITERALS+1].fc, ltree[code+LITERALS+1].dl)
          extra = @@extra_lbits[code]
          if extra != 0
            lc -= @@base_length[code]
            send_bits(s, lc, extra)
          end
          dist-=1
          code = (dist < 256) ? @@_dist_code[dist] : @@_dist_code[256+(dist >> 7)]

          send_bits(s, dtree[code].fc, dtree[code].dl)
          extra = @@extra_dbits[code]
          if extra != 0
            dist-= @@base_dist[code]
            send_bits(s, dist, extra)
          end
        end
        break if (lx >= s.last_lit)
      end
    end
    send_bits(s, ltree[END_BLOCK].fc, ltree[END_BLOCK].dl)
    s.last_eob_len = ltree[END_BLOCK].dl
  end

  # Determine the best encoding for the current block: dynamic trees, static
  #  trees or store, and output the encoded block to the zip file.
  def _tr_flush_block(s,buf,stored_len,eof)
    max_blindex = 0
    if (s.level > 0)
      if (stored_len>0 && s.strm.data_type == Z_UNKNOWN)
        set_data_type(s)
      end

      build_tree(s, s.l_desc)

      build_tree(s, s.d_desc)
      max_blindex = build_bl_tree(s)

      opt_lenb = (s.opt_len+3+7) >> 3
      static_lenb = (s.static_len+3+7) >> 3
      if (static_lenb <= opt_lenb)
        opt_lenb = static_lenb
      end
    else
      static_lenb = stored_len + 5
      opt_lenb = static_lenb
    end
    if (stored_len+4 <= opt_lenb) && buf
      _tr_stored_block(s, buf, stored_len, eof)
    elsif (s.strategy == Z_FIXED || static_lenb == opt_lenb)
      send_bits(s, (STATIC_TREES << 1)+(eof ? 1 : 0), 3)
      compress_block(s, @@static_ltree, @@static_dtree)
    else
      send_bits(s, (DYN_TREES << 1)+(eof ? 1 : 0), 3)
      send_all_trees(s, s.l_desc.max_code+1, s.d_desc.max_code+1,
                     max_blindex+1)
      compress_block(s, s.dyn_ltree, s.dyn_dtree)
    end

    init_block(s)

    bi_windup(s) if eof
  end

  # Save the match info and tally the frequency counts. Return true if
  #  the current block must be flushed.
  def _tr_tally(s,dist,lc)
    s.d_buf[s.last_lit] = dist
    s.l_buf[s.last_lit] = lc
    s.last_lit+=1
    if dist == 0
      s.dyn_ltree[lc].fc+=1
    else
      s.matches+=1
      dist-=1

      code =  (dist < 256) ? @@_dist_code[dist] : @@_dist_code[256+(dist >> 7)]
      s.dyn_ltree[@@_length_code[lc]+LITERALS+1].fc += 1
      s.dyn_dtree[code].fc += 1
    end

    (s.last_lit == s.lit_bufsize-1)
  end

  #   Compresses the source buffer into the destination buffer. The level
  # parameter has the same meaning as in deflateInit.  sourceLen is the byte
  # length of the source buffer. Upon entry, destLen is the total size of the
  # destination buffer, which must be at least 0.1% larger than sourceLen plus
  # 12 bytes. Upon exit, destLen is the actual size of the compressed buffer.
  #
  #   compress2 returns Z_OK if success, Z_MEM_ERROR if there was not enough
  # memory, Z_BUF_ERROR if there was not enough room in the output buffer,
  # Z_STREAM_ERROR if the level parameter is invalid.
  def compress2(dest,destLen,source,sourceLen,level)
    stream = Z_stream.new
    stream.next_in = Bytef.new(source)
    stream.avail_in = sourceLen
    stream.next_out = Bytef.new(dest)
    stream.avail_out = destLen
    if (stream.avail_out != destLen)
      return [Z_BUF_ERROR,destLen]
    end

    err = deflateInit(stream, level)
    if (err != Z_OK)
      return [err,destLen]
    end

    err = deflate(stream, Z_FINISH)
    if (err != Z_STREAM_END)
      deflateEnd(stream)
      if err == Z_OK
        return [Z_BUF_ERROR,destLen]
      else
        return [err,destLen]
      end
    end
    destLen = stream.total_out

    err = deflateEnd(stream)
    return [err,destLen]
  end

  #
  def compress(dest,destLen,source,sourceLen)
    return compress2(dest, destLen, source, sourceLen, Z_DEFAULT_COMPRESSION)
  end

  Code = Struct.new(:op,:bits,:val)
  ENOUGH = 2048
  MAXD = 592

  CODES,
  LENS,
  DISTS = *0..2

  MAXBITS = 15

  Inflate_copyright = " inflate 1.2.3 Copyright 1995-2005 Mark Adler "

  # Build a set of tables to decode the provided canonical Huffman code.
  # The code lengths are lens[0..codes-1].  The result starts at *table,
  # whose indices are 0..2^bits-1.  work is a writable array of at least
  # lens shorts, which is used as a work area.  type is the type of code
  # to be generated, CODES, LENS, or DISTS.  On return, zero is success,
  # -1 is an invalid code, and +1 means that ENOUGH isn't enough.  table
  # on return points to the next available entry's address.  bits is the
  # requested root table index bits, and on return it is the actual root
  # table index bits.  It will differ if the request is greater than the
  # longest code or if it is less than the shortest code.
  def inflate_table(type, lens, codes, table, offset, bits, work)
    lbase =  [
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
        35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0]
    lext = [
        16, 16, 16, 16, 16, 16, 16, 16, 17, 17, 17, 17, 18, 18, 18, 18,
        19, 19, 19, 19, 20, 20, 20, 20, 21, 21, 21, 21, 16, 201, 196]
    dbase = [
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
        8193, 12289, 16385, 24577, 0, 0]
    dext = [
        16, 16, 16, 16, 17, 17, 18, 18, 19, 19, 20, 20, 21, 21, 22, 22,
        23, 23, 24, 24, 25, 25, 26, 26, 27, 27,
        28, 28, 29, 29, 64, 64]

    this = Code.new

    count = Array.new(MAXBITS+1,0)
    offs = Array.new(MAXBITS+1,0)
    for sym in 0 ... codes
        count[lens[sym]]+=1
    end

    root = bits

    max = 0
    MAXBITS.downto(1) do |i|
        max = i
        break if count[max] != 0
    end

    root = max if (root > max)
    if max == 0
        this.op = 64
        this.bits = 1
        this.val = 0
        table[offset] = this.dup
        offset += 1
        table[offset] = this.dup
        offset += 1
        bits = 1
        return [0,bits,offset]
    end
    for min in 1 .. MAXBITS
        break if count[min] != 0
    end
    root = min if (root < min)

    left = 1
    for len in 1 .. MAXBITS
        left <<= 1
        left -= count[len]
        return [-1,bits,offset]  if (left < 0)
    end
    if (left > 0 && (type == CODES || max != 1))
        return [-1,bits,offset]
    end

    offs[1] = 0
    for len in 1 ... MAXBITS
        offs[len + 1] = offs[len] + count[len]
    end

    for sym in 0 ... codes
        if lens[sym] != 0
          work[offs[lens[sym]]] = sym
          offs[lens[sym]]+=1
        end
    end

    case (type)
    when CODES
        extra = Bytef.new(work)
        base = Bytef.new(work)
        _end = 19
    when LENS
        base = Bytef.new(lbase)
        base -= 257
        extra = Bytef.new(lext)
        extra -= 257
        _end = 256
    else
        base = Bytef.new(dbase)
        extra = Bytef.new(dext)
        _end = -1
    end

    huff = 0
    sym = 0
    len = min
    _next = Bytef.new(table,offset)
    curr = root
    drop = 0
    low = (-1)
    used = 1 << root
    mask = used - 1

    if (type == LENS && used >= ENOUGH - MAXD)
        return [1,bits,offset]
    end
    loop do
        this.bits = (len - drop)
        if ((work[sym]) < _end)
            this.op = 0
            this.val = work[sym]
        elsif ((work[sym]) > _end)
            this.op = (extra[work[sym]])
            this.val = base[work[sym]]
        else
            this.op = (32 + 64)
            this.val = 0
        end
        incr = 1 << (len - drop)
        fill = 1 << curr
        min = fill
        loop do
            fill -= incr
            _next[((huff >> drop) + fill)] = this.dup
            break if fill == 0
        end
        incr = 1 << (len - 1)
        while (huff & incr) != 0
            incr >>= 1
        end
        if incr != 0
            huff &= incr - 1
            huff += incr
        else
            huff = 0
        end
        sym+=1
        count[len]-=1
        if count[len] == 0
            break if (len == max)
            len = lens[work[sym]]
        end

        if (len > root && (huff & mask) != low)
            if drop == 0
                drop = root
            end

            _next += min

            curr = len - drop
            left = (1 << curr)
            while (curr + drop < max)
                left -= count[curr + drop]
                break if (left <= 0)
                curr+=1
                left <<= 1
            end

            used += 1 << curr
            if (type == LENS && used >= ENOUGH - MAXD)
                return [1,bits,offset]
            end
            low = huff & mask
            table[offset+low] = Code.new
            table[offset+low].op = curr
            table[offset+low].bits = root
            table[offset+low].val = (_next.offset - offset)
        end
    end
    this.op = 64
    this.bits = (len - drop)
    this.val = 0
    while huff != 0
        if (drop != 0 && (huff & mask) != low)
            drop = 0
            len = root
            _next.offset = offset
            this.bits = len
        end

        _next[(huff >> drop)] = this.dup
        incr = 1 << (len - 1)
        while (huff & incr) != 0
            incr >>= 1
        end
        if incr != 0
            huff &= incr - 1
            huff += incr
        else
            huff = 0
        end
    end

    offset += used
    bits = root
    return [0,bits,offset]
  end

  # Decode literal, length, and distance codes and write out the resulting
  # literal and match bytes until either not enough input or output is
  # available, an end-of-block is encountered, or a data error is encountered.
  # When large enough input and output buffers are supplied to inflate(), for
  # example, a 16K input buffer and a 64K output buffer, more than 95% of the
  # inflate execution time is spent in this routine.
  #
  # Entry assumptions:
  #
  #      state->mode == LEN
  #      strm->avail_in >= 6
  #      strm->avail_out >= 258
  #      start >= strm->avail_out
  #      state->bits < 8
  #
  # On return, state->mode is one of:
  #
  #      LEN -- ran out of enough output space or enough available input
  #      TYPE -- reached end of block code, inflate() to interpret next block
  #      BAD -- error in block data
  #
  # Notes:
  #
  #  - The maximum input bits used by a length/distance pair is 15 bits for the
  #    length code, 5 bits for the length extra, 15 bits for the distance code,
  #    and 13 bits for the distance extra.  This totals 48 bits, or six bytes.
  #    Therefore if strm->avail_in >= 6, then there is enough input to avoid
  #    checking for available input while decoding.
  #
  #  - The maximum bytes that a single length/distance pair can output is 258
  #    bytes, which is the maximum length that can be coded.  inflate_fast()
  #    requires strm->avail_out >= 258 for each loop to avoid checking for
  #    output space.
  #
  def inflate_fast(strm, start)
    state = strm.state
    _in = Bytef.new(strm.next_in,strm.next_in.offset - 1)
    last = Bytef.new(_in,_in.offset + (strm.avail_in - 5))
    out = Bytef.new(strm.next_out, strm.next_out.offset - 1)
    beg = Bytef.new(out, out.offset - (start - strm.avail_out))
    _end = Bytef.new(out, out.offset + (strm.avail_out - 257))
    wsize = state.wsize
    whave = state.whave
    write = state.write
    window = state.window
    hold = state.hold
    bits = state.bits
    lcode = state.lencode
    dcode = state.distcode
    lmask = (1 << state.lenbits) - 1
    dmask = (1 << state.distbits) - 1
    status = nil
    this = nil

    op = -1
    len = -1

    loop do
      if status.nil?
        if (bits < 15)
            _in += 1
            hold += _in.get << bits
            bits += 8
            _in += 1
            hold += _in.get << bits
            bits += 8
        end
        this = lcode[hold & lmask]
        status = :dolen if status.nil?
      end
      if(status==:dolen)
          op = (this.bits)
          hold >>= op
          bits -= op
          op = (this.op)
      end
      if op == 0
            out += 1
            out.set(this.val)
      elsif (op & 16) != 0 || status == :dodist
          if(status != :dodist && (op & 16) != 0)
            len = (this.val)
            op &= 15
            if op != 0
                if (bits < op)
                    _in += 1
                    hold += _in.get << bits
                    bits += 8
                end
                len += hold & ((1 << op) - 1)
                hold >>= op
                bits -= op
            end
            if (bits < 15)
                _in += 1
                hold += _in.get << bits
                bits += 8
                _in += 1
                hold += _in.get << bits
                bits += 8
            end
            this = dcode[hold & dmask]
          end
            op = (this.bits)
            hold >>= op
            bits -= op
            op = this.op
            if (op & 16) != 0
                dist = this.val
                op &= 15
                if (bits < op)
                    _in += 1
                    hold += _in.get << bits
                    bits += 8
                    if (bits < op)
                        _in += 1
                        hold += _in.get << bits
                        bits += 8
                    end
                end
                dist += hold & ((1 << op) - 1)
                hold >>= op
                bits -= op
                op = (out.offset - beg.offset)
                if (dist > op)
                    op = dist - op
                    if (op > whave)
                        strm.msg = "invalid distance too far back"
                        state.mode = BAD
                        break
                    end
                    from = Bytef.new(window,-1)
                    if write == 0
                        from += wsize - op
                        if (op < len)
                            len -= op
                            loop do
                                out += 1
                                from += 1
                                out.set(from.get)
                                op -= 1
                                break if op == 0
                            end
                            from = Bytef.new(out,out.offset - dist)
                        end
                    elsif (write < op)
                        from += wsize + write - op
                        op -= write
                        if (op < len)
                            len -= op
                            loop do
                                out += 1
                                from += 1
                                out.set(from.get)
                                op -= 1
                                break if op == 0
                            end
                            from = Bytef.new(window,-1)
                            if (write < len)
                                op = write
                                len -= op
                                loop do
                                    out += 1
                                    from += 1
                                    out.set(from.get)
                                    op -= 1
                                    break if op == 0
                                end
                                from = Bytef.new(out,out.offset - dist)
                            end
                        end
                    else
                        from += write - op
                        if (op < len)
                            len -= op
                            loop do
                                out += 1
                                from += 1
                                out.set(from.get)
                                op -= 1
                                break if op == 0
                            end
                            from = Bytef.new(out,out.offset - dist)
                        end
                    end
                    while (len > 2)
                        out += 1
                        from += 1
                        out.set(from.get)
                        out += 1
                        from += 1
                        out.set(from.get)
                        out += 1
                        from += 1
                        out.set(from.get)
                        len -= 3
                    end
                    if len != 0
                        out += 1
                        from += 1
                        out.set(from.get)
                        if (len > 1)
                          out += 1
                          from += 1
                          out.set(from.get)
                        end
                    end
                else
                    from = Bytef.new(out,out.offset - dist)
                    loop do
                        out += 1
                        from += 1
                        out.set(from.get)
                        out += 1
                        from += 1
                        out.set(from.get)
                        out += 1
                        from += 1
                        out.set(from.get)
                        len -= 3
                        break if (len <= 2)
                    end
                    if len != 0
                        out += 1
                        from += 1
                        out.set(from.get)
                        if (len > 1)
                          out += 1
                          from += 1
                          out.set(from.get)
                        end
                    end
                end
            elsif (op & 64) == 0
                this = dcode[this.val + (hold & ((1 << op) - 1))]
                status = :dodist
                redo
            else
                strm.msg = "invalid distance code"
                state.mode = BAD
                break
            end
      elsif (op & 64) == 0
          this = lcode[this.val + (hold & ((1 << op) - 1))]
          status = :dolen
          redo
      elsif (op & 32) != 0
          state.mode = TYPE
          break
      else
          strm.msg = "invalid literal/length code"
          state.mode = BAD
          break
      end
      status = nil
      break unless (_in.offset < last.offset && out.offset < _end.offset)
    end
    len = bits >> 3
    _in -= len
    bits -= len << 3
    hold &= (1 << bits) - 1
    strm.next_in.offset = _in.offset + 1
    strm.next_out.offset = out.offset + 1
    strm.avail_in = (_in.offset < last.offset ? 5 + (last.offset - _in.offset) : 5 - (_in.offset - last.offset))
    strm.avail_out = (out.offset < _end.offset ?
                                 257 + (_end.offset - out.offset) : 257 - (out.offset - _end.offset))
    state.hold = hold
    state.bits = bits
    return
  end

  HEAD,
  FLAGS,
  TIME,
  OS,
  EXLEN,
  EXTRA,
  NAME,
  COMMENT,
  HCRC,
  DICTID,
  DICT,
  TYPE,
  TYPEDO,
  STORED,
  COPY,
  TABLE,
  LENLENS,
  CODELENS,
  LEN,
  LENEXT,
  DIST,
  DISTEXT,
  MATCH,
  LIT,
  CHECK,
  LENGTH,
  DONE,
  BAD,
  MEM,
  SYNC = *0..29

  Inflate_state = Struct.new(:mode,:last,:wrap,:havedict,:flags,:dmax,:check,:total,
    :head,:wbits,:wsize,:whave,:write,:window,:hold,:bits,:length,:offset,
    :extra,:lencode,:distcode,:lenbits,:distbits,:ncode,:nlen,:ndist,:have,:next,:lens,
    :work,:codes)

  #
  def inflateInit(strm)
    return inflateInit_(strm,ZLIB_VERSION, strm.size)
  end

  #
  def inflateInit2(strm, windowBits)
    return inflateInit2_(strm,windowBits, ZLIB_VERSION, strm.size)
  end

  #
  def inflateReset(strm)
    return Z_STREAM_ERROR if (strm.nil? || strm.state.nil?)
    state = strm.state
    strm.total_in = strm.total_out = state.total = 0
    strm.msg = ''
    strm.adler = 1
    state.mode = HEAD
    state.last = 0
    state.havedict = 0
    state.dmax = 32768
    state.head = nil
    state.wsize = 0
    state.whave = 0
    state.write = 0
    state.hold = 0
    state.bits = 0
    state.lens = Bytef.new(Array.new(320,0))
    state.work = Array.new(288,0)
    state.codes = Array.new(ENOUGH,0)
    state.next = Bytef.new(state.codes)
    state.distcode = Bytef.new(state.codes)
    state.lencode = Bytef.new(state.codes)
    return Z_OK
  end

  #
  def inflatePrime(strm, bits, value)
    return Z_STREAM_ERROR if (strm.nil? || strm.state.nil?)
    state = strm.state
    return Z_STREAM_ERROR if (bits > 16 || state.bits + bits > 32)
    value &= (1 << bits) - 1
    state.hold += value << state.bits
    state.bits += bits
    return Z_OK
  end

  #
  def inflateInit2_(strm, windowBits, version, stream_size)
    if (version.nil? || version[0] != ZLIB_VERSION[0] ||
        stream_size != strm.size)
        return Z_VERSION_ERROR
    end
    return Z_STREAM_ERROR if strm.nil?
    strm.msg = ''
    state = Inflate_state.new
    state.lens = Array.new(320,0)
    state.work = Array.new(288,0)
    state.codes = Array.new(ENOUGH,0)
    return Z_MEM_ERROR if state.nil?
    strm.state = state
    if (windowBits < 0)
        state.wrap = 0
        windowBits = -windowBits
    else
        state.wrap = (windowBits >> 4) + 1
        windowBits &= 15 if (windowBits < 48)
    end
    if (windowBits < 8 || windowBits > 15)
        ZFREE(strm, state)
        strm.state = nil
        return Z_STREAM_ERROR
    end
    state.wbits = windowBits
    state.window = nil
    return inflateReset(strm)
  end

  #
  def inflateInit_(strm, version, stream_size)
    return inflateInit2_(strm, DEF_WBITS, version, stream_size)
  end

  # Return state with length and distance decoding tables and index sizes set to
  # fixed code decoding.  Normally this returns fixed tables from inffixed.h.
  # If BUILDFIXED is defined, then instead this routine builds the tables the
  # first time it's called, and returns those tables the first time and
  # thereafter.  This reduces the size of the code by about 2K bytes, in
  # exchange for a little execution time.  However, BUILDFIXED should not be
  # used for threaded applications, since the rewriting of the tables and virgin
  # may not be thread-safe.
  def fixedtables(state)

    lenfix = [
        [96,7,0],[0,8,80],[0,8,16],[20,8,115],[18,7,31],[0,8,112],[0,8,48],
        [0,9,192],[16,7,10],[0,8,96],[0,8,32],[0,9,160],[0,8,0],[0,8,128],
        [0,8,64],[0,9,224],[16,7,6],[0,8,88],[0,8,24],[0,9,144],[19,7,59],
        [0,8,120],[0,8,56],[0,9,208],[17,7,17],[0,8,104],[0,8,40],[0,9,176],
        [0,8,8],[0,8,136],[0,8,72],[0,9,240],[16,7,4],[0,8,84],[0,8,20],
        [21,8,227],[19,7,43],[0,8,116],[0,8,52],[0,9,200],[17,7,13],[0,8,100],
        [0,8,36],[0,9,168],[0,8,4],[0,8,132],[0,8,68],[0,9,232],[16,7,8],
        [0,8,92],[0,8,28],[0,9,152],[20,7,83],[0,8,124],[0,8,60],[0,9,216],
        [18,7,23],[0,8,108],[0,8,44],[0,9,184],[0,8,12],[0,8,140],[0,8,76],
        [0,9,248],[16,7,3],[0,8,82],[0,8,18],[21,8,163],[19,7,35],[0,8,114],
        [0,8,50],[0,9,196],[17,7,11],[0,8,98],[0,8,34],[0,9,164],[0,8,2],
        [0,8,130],[0,8,66],[0,9,228],[16,7,7],[0,8,90],[0,8,26],[0,9,148],
        [20,7,67],[0,8,122],[0,8,58],[0,9,212],[18,7,19],[0,8,106],[0,8,42],
        [0,9,180],[0,8,10],[0,8,138],[0,8,74],[0,9,244],[16,7,5],[0,8,86],
        [0,8,22],[64,8,0],[19,7,51],[0,8,118],[0,8,54],[0,9,204],[17,7,15],
        [0,8,102],[0,8,38],[0,9,172],[0,8,6],[0,8,134],[0,8,70],[0,9,236],
        [16,7,9],[0,8,94],[0,8,30],[0,9,156],[20,7,99],[0,8,126],[0,8,62],
        [0,9,220],[18,7,27],[0,8,110],[0,8,46],[0,9,188],[0,8,14],[0,8,142],
        [0,8,78],[0,9,252],[96,7,0],[0,8,81],[0,8,17],[21,8,131],[18,7,31],
        [0,8,113],[0,8,49],[0,9,194],[16,7,10],[0,8,97],[0,8,33],[0,9,162],
        [0,8,1],[0,8,129],[0,8,65],[0,9,226],[16,7,6],[0,8,89],[0,8,25],
        [0,9,146],[19,7,59],[0,8,121],[0,8,57],[0,9,210],[17,7,17],[0,8,105],
        [0,8,41],[0,9,178],[0,8,9],[0,8,137],[0,8,73],[0,9,242],[16,7,4],
        [0,8,85],[0,8,21],[16,8,258],[19,7,43],[0,8,117],[0,8,53],[0,9,202],
        [17,7,13],[0,8,101],[0,8,37],[0,9,170],[0,8,5],[0,8,133],[0,8,69],
        [0,9,234],[16,7,8],[0,8,93],[0,8,29],[0,9,154],[20,7,83],[0,8,125],
        [0,8,61],[0,9,218],[18,7,23],[0,8,109],[0,8,45],[0,9,186],[0,8,13],
        [0,8,141],[0,8,77],[0,9,250],[16,7,3],[0,8,83],[0,8,19],[21,8,195],
        [19,7,35],[0,8,115],[0,8,51],[0,9,198],[17,7,11],[0,8,99],[0,8,35],
        [0,9,166],[0,8,3],[0,8,131],[0,8,67],[0,9,230],[16,7,7],[0,8,91],
        [0,8,27],[0,9,150],[20,7,67],[0,8,123],[0,8,59],[0,9,214],[18,7,19],
        [0,8,107],[0,8,43],[0,9,182],[0,8,11],[0,8,139],[0,8,75],[0,9,246],
        [16,7,5],[0,8,87],[0,8,23],[64,8,0],[19,7,51],[0,8,119],[0,8,55],
        [0,9,206],[17,7,15],[0,8,103],[0,8,39],[0,9,174],[0,8,7],[0,8,135],
        [0,8,71],[0,9,238],[16,7,9],[0,8,95],[0,8,31],[0,9,158],[20,7,99],
        [0,8,127],[0,8,63],[0,9,222],[18,7,27],[0,8,111],[0,8,47],[0,9,190],
        [0,8,15],[0,8,143],[0,8,79],[0,9,254],[96,7,0],[0,8,80],[0,8,16],
        [20,8,115],[18,7,31],[0,8,112],[0,8,48],[0,9,193],[16,7,10],[0,8,96],
        [0,8,32],[0,9,161],[0,8,0],[0,8,128],[0,8,64],[0,9,225],[16,7,6],
        [0,8,88],[0,8,24],[0,9,145],[19,7,59],[0,8,120],[0,8,56],[0,9,209],
        [17,7,17],[0,8,104],[0,8,40],[0,9,177],[0,8,8],[0,8,136],[0,8,72],
        [0,9,241],[16,7,4],[0,8,84],[0,8,20],[21,8,227],[19,7,43],[0,8,116],
        [0,8,52],[0,9,201],[17,7,13],[0,8,100],[0,8,36],[0,9,169],[0,8,4],
        [0,8,132],[0,8,68],[0,9,233],[16,7,8],[0,8,92],[0,8,28],[0,9,153],
        [20,7,83],[0,8,124],[0,8,60],[0,9,217],[18,7,23],[0,8,108],[0,8,44],
        [0,9,185],[0,8,12],[0,8,140],[0,8,76],[0,9,249],[16,7,3],[0,8,82],
        [0,8,18],[21,8,163],[19,7,35],[0,8,114],[0,8,50],[0,9,197],[17,7,11],
        [0,8,98],[0,8,34],[0,9,165],[0,8,2],[0,8,130],[0,8,66],[0,9,229],
        [16,7,7],[0,8,90],[0,8,26],[0,9,149],[20,7,67],[0,8,122],[0,8,58],
        [0,9,213],[18,7,19],[0,8,106],[0,8,42],[0,9,181],[0,8,10],[0,8,138],
        [0,8,74],[0,9,245],[16,7,5],[0,8,86],[0,8,22],[64,8,0],[19,7,51],
        [0,8,118],[0,8,54],[0,9,205],[17,7,15],[0,8,102],[0,8,38],[0,9,173],
        [0,8,6],[0,8,134],[0,8,70],[0,9,237],[16,7,9],[0,8,94],[0,8,30],
        [0,9,157],[20,7,99],[0,8,126],[0,8,62],[0,9,221],[18,7,27],[0,8,110],
        [0,8,46],[0,9,189],[0,8,14],[0,8,142],[0,8,78],[0,9,253],[96,7,0],
        [0,8,81],[0,8,17],[21,8,131],[18,7,31],[0,8,113],[0,8,49],[0,9,195],
        [16,7,10],[0,8,97],[0,8,33],[0,9,163],[0,8,1],[0,8,129],[0,8,65],
        [0,9,227],[16,7,6],[0,8,89],[0,8,25],[0,9,147],[19,7,59],[0,8,121],
        [0,8,57],[0,9,211],[17,7,17],[0,8,105],[0,8,41],[0,9,179],[0,8,9],
        [0,8,137],[0,8,73],[0,9,243],[16,7,4],[0,8,85],[0,8,21],[16,8,258],
        [19,7,43],[0,8,117],[0,8,53],[0,9,203],[17,7,13],[0,8,101],[0,8,37],
        [0,9,171],[0,8,5],[0,8,133],[0,8,69],[0,9,235],[16,7,8],[0,8,93],
        [0,8,29],[0,9,155],[20,7,83],[0,8,125],[0,8,61],[0,9,219],[18,7,23],
        [0,8,109],[0,8,45],[0,9,187],[0,8,13],[0,8,141],[0,8,77],[0,9,251],
        [16,7,3],[0,8,83],[0,8,19],[21,8,195],[19,7,35],[0,8,115],[0,8,51],
        [0,9,199],[17,7,11],[0,8,99],[0,8,35],[0,9,167],[0,8,3],[0,8,131],
        [0,8,67],[0,9,231],[16,7,7],[0,8,91],[0,8,27],[0,9,151],[20,7,67],
        [0,8,123],[0,8,59],[0,9,215],[18,7,19],[0,8,107],[0,8,43],[0,9,183],
        [0,8,11],[0,8,139],[0,8,75],[0,9,247],[16,7,5],[0,8,87],[0,8,23],
        [64,8,0],[19,7,51],[0,8,119],[0,8,55],[0,9,207],[17,7,15],[0,8,103],
        [0,8,39],[0,9,175],[0,8,7],[0,8,135],[0,8,71],[0,9,239],[16,7,9],
        [0,8,95],[0,8,31],[0,9,159],[20,7,99],[0,8,127],[0,8,63],[0,9,223],
        [18,7,27],[0,8,111],[0,8,47],[0,9,191],[0,8,15],[0,8,143],[0,8,79],
        [0,9,255]].map{|i|Code.new(*i)}

    distfix = [
        [16,5,1],[23,5,257],[19,5,17],[27,5,4097],[17,5,5],[25,5,1025],
        [21,5,65],[29,5,16385],[16,5,3],[24,5,513],[20,5,33],[28,5,8193],
        [18,5,9],[26,5,2049],[22,5,129],[64,5,0],[16,5,2],[23,5,385],
        [19,5,25],[27,5,6145],[17,5,7],[25,5,1537],[21,5,97],[29,5,24577],
        [16,5,4],[24,5,769],[20,5,49],[28,5,12289],[18,5,13],[26,5,3073],
        [22,5,193],[64,5,0]].map{|i|Code.new(*i)}
    state.lencode = Bytef.new(lenfix)
    state.lenbits = 9
    state.distcode = Bytef.new(distfix)
    state.distbits = 5
  end

  # Update the window with the last wsize (normally 32K) bytes written before
  # returning.  If window does not exist yet, create it.  This is only called
  # when a window is already in use, or when output has been written during this
  # inflate call, but the end of the deflate stream has not been reached yet.
  # It is also called to create a window for dictionary data when a dictionary
  # is loaded.
  #
  # Providing output buffers larger than 32K to inflate() should provide a speed
  # advantage, since only the last 32K of output is copied to the sliding window
  # upon return from inflate(), and since all distances after the first 32K of
  # output will fall in the output data, making match copies simpler and faster.
  # The advantage may be dependent on the size of the processor's data caches.
  def updatewindow(strm, out)
    state = strm.state

    if state.window.nil?
        state.window = 0.chr * (1 << state.wbits)
        return true if state.window.nil?
    end

    if state.wsize == 0
        state.wsize = 1 << state.wbits
        state.write = 0
        state.whave = 0
    end

    copy = out - strm.avail_out
    if (copy >= state.wsize)
        state.window[0,state.wsize] =
          strm.next_out.buffer[strm.next_out.offset-state.wsize,state.wsize]
        state.write = 0
        state.whave = state.wsize
    else
        dist = state.wsize - state.write
        dist = copy if (dist > copy)
        state.window[state.write,dist] =
          strm.next_out.buffer[strm.next_out.offset-copy,dist]
        copy -= dist
        if copy != 0
            state.window[0,copy] =
              strm.next_out.buffer[strm.next_out.offset-copy,copy]
            state.write = copy
            state.whave = state.wsize
        else
            state.write += dist
            state.write = 0 if (state.write == state.wsize)
            state.whave += dist if (state.whave < state.wsize)
        end
    end
    return false
  end

  # check function to use adler32() for zlib or crc32() for gzip
  def UPDATE(state, check, buf)
    state.flags != 0 ? crc32(check, buf) : adler32(check, buf)
  end

  # compute crc
  def CRC2(check, word)
      hbuf = 0.chr * 2
      hbuf[0] = (word & 0xff).chr
      hbuf[1] = ((word >> 8) & 0xff).chr
      check = crc32(check, hbuf)
  end

  # compute crc
  def CRC4(check, word)
        hbuf = 0.chr * 4
        hbuf[0] = (word & 0xff).chr
        hbuf[1] = ((word >> 8) & 0xff).chr
        hbuf[2] = ((word >> 16) & 0xff).chr
        hbuf[3] = ((word >> 24) & 0xff).chr
        check = crc32(check, hbuf)
  end

  # Load registers with state in inflate() for speed
  def LOAD(strm,state)
        @@put = strm.next_out
        @@left = strm.avail_out
        @@next = strm.next_in
        @@have = strm.avail_in
        @@hold = state.hold
        @@bits = state.bits
  end

  # Restore state from registers in inflate()
  def RESTORE(strm,state)
        strm.next_out = @@put
        strm.avail_out = @@left
        strm.next_in = @@next
        strm.avail_in = @@have
        state.hold = @@hold
        state.bits = @@bits
  end

  # Clear the input bit accumulator
  def INITBITS()
        @@hold = 0
        @@bits = 0
  end

  # Get a byte of input into the bit accumulator, or return from inflate()
  # if there is no input available.
  def PULLBYTE()
    throw :inf_leave if @@have == 0
    @@have -= 1
    @@next.get
    @@hold += (@@next.get) << @@bits
    @@next += 1
    @@bits += 8
  end

  # Assure that there are at least n bits in the bit accumulator.  If there is
  # not enough available input to do that, then return from inflate().
  def NEEDBITS(n)
        while (@@bits < (n))
            PULLBYTE()
        end
  end

  # Return the low n bits of the bit accumulator (n < 16)
  def BITS(n)
    (@@hold & ((1 << (n)) - 1))
  end

  # Remove n bits from the bit accumulator
  def DROPBITS(n)
        @@hold >>= (n)
        @@bits -= (n)
  end

  # Remove zero to seven bits as needed to go to a byte boundary
  def BYTEBITS()
        @@hold >>= @@bits & 7
        @@bits -= @@bits & 7
  end

  # Reverse the bytes in a 32-bit value
  def REVERSE(q)
    ((((q) >> 24) & 0xff) + (((q) >> 8) & 0xff00) +
     (((q) & 0xff00) << 8) + (((q) & 0xff) << 24))
  end

  # inflate() uses a state machine to process as much input data and generate as
  # much output data as possible before returning.  The state machine is
  # structured roughly as follows:
  #
  #  for (;;) switch (state) {
  #  ...
  #  case STATEn:
  #      if (not enough input data or output space to make progress)
  #          return;
  #      ... make progress ...
  #      state = STATEm;
  #      break;
  #  ...
  #  }
  #
  # so when inflate() is called again, the same case is attempted again, and
  # if the appropriate resources are provided, the machine proceeds to the
  # next state.  The NEEDBITS() macro is usually the way the state evaluates
  # whether it can proceed or should return.  NEEDBITS() does the return if
  # the requested bits are not available.  The typical use of the BITS macros
  # is:
  #
  #      NEEDBITS(n);
  #      ... do something with BITS(n) ...
  #      DROPBITS(n);
  #
  # where NEEDBITS(n) either returns from inflate() if there isn't enough
  # input left to load n bits into the accumulator, or it continues.  BITS(n)
  # gives the low n bits in the accumulator.  When done, DROPBITS(n) drops
  # the low n bits off the accumulator.  INITBITS() clears the accumulator
  # and sets the number of available bits to zero.  BYTEBITS() discards just
  # enough bits to put the accumulator on a byte boundary.  After BYTEBITS()
  # and a NEEDBITS(8), then BITS(8) would return the next byte in the stream.
  #
  # NEEDBITS(n) uses PULLBYTE() to get an available byte of input, or to return
  # if there is no input available.  The decoding of variable length codes uses
  # PULLBYTE() directly in order to pull just enough bytes to decode the next
  # code, and no more.
  #
  # Some states loop until they get enough input, making sure that enough
  # state information is maintained to continue the loop where it left off
  # if NEEDBITS() returns in the loop.  For example, want, need, and keep
  # would all have to actually be part of the saved state in case NEEDBITS()
  # returns:
  #
  #  case STATEw:
  #      while (want < need) {
  #          NEEDBITS(n);
  #          keep[want++] = BITS(n);
  #          DROPBITS(n);
  #      }
  #      state = STATEx;
  #  case STATEx:
  #
  # As shown above, if the next state is also the next case, then the break
  # is omitted.
  #
  # A state may also return if there is not enough output space available to
  # complete that state.  Those states are copying stored data, writing a
  # literal byte, and copying a matching string.
  #
  # When returning, a "goto inf_leave" is used to update the total counters,
  # update the check value, and determine whether any progress has been made
  # during that inflate() call in order to return the proper return code.
  # Progress is defined as a change in either strm->avail_in or strm->avail_out.
  # When there is a window, goto inf_leave will update the window with the last
  # output written.  If a goto inf_leave occurs in the middle of decompression
  # and there is no window currently, goto inf_leave will create one and copy
  # output to the window for the next call of inflate().
  #
  # In this implementation, the flush parameter of inflate() only affects the
  # return code (per zlib.h).  inflate() always writes as much as possible to
  # strm->next_out, given the space available and the provided input--the effect
  # documented in zlib.h of Z_SYNC_FLUSH.  Furthermore, inflate() always defers
  # the allocation of and copying into a sliding window until necessary, which
  # provides the effect documented in zlib.h for Z_FINISH when the entire input
  # stream available.  So the only thing the flush parameter actually does is:
  # when flush is set to Z_FINISH, inflate() cannot return Z_OK.  Instead it
  # will return Z_BUF_ERROR if it has not reached the end of the stream.
  def inflate(strm, flush)
    order = [
        16, 17, 18, 0, 8, 7, 9, 6, 10, 5, 11, 4, 12, 3, 13, 2, 14, 1, 15]

    if strm.nil? || strm.state.nil? || strm.next_out.nil? ||
        (strm.next_in.nil? && strm.avail_in != 0)
        return Z_STREAM_ERROR
    end

    state = strm.state
    state.mode = TYPEDO  if (state.mode == TYPE)
    LOAD(strm,state)
    _in = @@have
    out = @@left
    ret = Z_OK

    catch :inf_leave do
      loop do
        if(state.mode==HEAD)
            if state.wrap == 0
                state.mode = TYPEDO
                next
            end
            NEEDBITS(16)
            if ((state.wrap & 2) != 0 && @@hold == 0x8b1f)
                state.check = crc32(0, nil)
                state.check = CRC2(state.check, @@hold)
                INITBITS()
                state.mode = FLAGS
                next
            end
            state.flags = 0
            if state.head
                state.head.done = -1
            end
            if ((state.wrap & 1) == 0 ||
                ((BITS(8) << 8) + (@@hold >> 8)) % 31) != 0
                strm.msg = "incorrect header check"
                state.mode = BAD
                next
            end
            if (BITS(4) != Z_DEFLATED)
                strm.msg = "unknown compression method"
                state.mode = BAD
                next
            end
            DROPBITS(4)
            len = BITS(4) + 8
            if (len > state.wbits)
                strm.msg = "invalid window size"
                state.mode = BAD
                next
            end
            state.dmax = 1 << len
            strm.adler = state.check = adler32(0, nil)
            state.mode = (@@hold & 0x200) != 0 ? DICTID : TYPE
            INITBITS()
        end
        if(state.mode==FLAGS)
            NEEDBITS(16)
            state.flags = (@@hold)
            if ((state.flags & 0xff) != Z_DEFLATED)
                strm.msg = "unknown compression method"
                state.mode = BAD
                next
            end
            if (state.flags & 0xe000) != 0
                strm.msg = "unknown header flags set"
                state.mode = BAD
                next
            end
            if state.head
                state.head.text = ((@@hold >> 8) & 1)
            end
            state.check = CRC2(state.check, @@hold) if (state.flags & 0x0200) != 0
            INITBITS()
            state.mode = TIME
        end
        if(state.mode==TIME)
            NEEDBITS(32)
            if state.head
                state.head.time = @@hold
            end
            state.check = CRC4(state.check, @@hold) if (state.flags & 0x0200) != 0
            INITBITS()
            state.mode = OS
        end
        if(state.mode==OS)
            NEEDBITS(16)
            if state.head
                state.head.xflags = (@@hold & 0xff)
                state.head.os = (@@hold >> 8)
            end
            state.check = CRC2(state.check, @@hold) if (state.flags & 0x0200) != 0
            INITBITS()
            state.mode = EXLEN
        end
        if(state.mode==EXLEN)
            if (state.flags & 0x0400) != 0
                NEEDBITS(16)
                state.length = @@hold
                if state.head
                    state.head.extra_len = @@hold
                end
                state.check = CRC2(state.check, @@hold) if (state.flags & 0x0200) != 0
                INITBITS()
            elsif state.head
                state.head.extra = nil
            end
            state.mode = EXTRA
        end
        if(state.mode==EXTRA)
            if (state.flags & 0x0400) != 0
                copy = state.length
                copy = @@have if (copy > @@have)
                if copy != 0
                    if state.head && state.head.extra
                        len = state.head.extra_len - state.length
                        l = len + copy > state.head.extra_max ?
                                state.head.extra_max - len : copy
                        state.head.extra[len,l] = @@next.current[0,l]
                    end
                    if (state.flags & 0x0200) != 0
                        state.check = crc32(state.check, @@next.current,copy)
                    end
                    @@have -= copy
                    @@next += copy
                    state.length -= copy
                end
                throw :inf_leave if state.length != 0
            end
            state.length = 0
            state.mode = NAME
        end
        if(state.mode==NAME)
            if (state.flags & 0x0800) != 0
                throw :inf_leave if @@have == 0
                copy = 0
                loop do
                    len = (@@next[copy])
                    copy+=1
                    if (state.head && state.head.name &&
                            state.length < state.head.name_max)
                        state.head.name[state.length] = len
                        state.length+=1
                    end
                    break unless (len != 0 && copy < @@have)
                end
                if (state.flags & 0x0200) != 0
                    state.check = crc32(state.check, @@next.current,copy)
                end
                @@have -= copy
                @@next += copy
                throw :inf_leave if len != 0
            elsif state.head
                state.head.name = nil
            end
            state.length = 0
            state.mode = COMMENT
        end
        if(state.mode==COMMENT)
            if (state.flags & 0x1000) != 0
                throw :inf_leave if @@have == 0
                copy = 0
                loop do
                    len = (@@next[copy])
                    copy += 1
                    if (state.head  && state.head.comment  &&
                            state.length < state.head.comm_max)
                        state.head.comment[state.length] = len
                        state.length+=1
                    end
                    break unless (len != 0 && copy < have)
                end
                if (state.flags & 0x0200) != 0
                    state.check = crc32(state.check, @@next.current, copy)
                end
                @@have -= copy
                @@next += copy
                throw :inf_leave if len != 0
            elsif state.head
                state.head.comment = nil
            end
            state.mode = HCRC
        end
        if(state.mode==HCRC)
            if (state.flags & 0x0200) != 0
                NEEDBITS(16)
                if (@@hold != (state.check & 0xffff))
                    strm.msg = "header crc mismatch"
                    state.mode = BAD
                    next
                end
                INITBITS()
            end
            if state.head
                state.head.hcrc = ((state.flags >> 9) & 1)
                state.head.done = 1
            end
            strm.adler = state.check = crc32(0, nil)
            state.mode = TYPE
        end
        if(state.mode==DICTID)
            NEEDBITS(32)
            strm.adler = state.check = REVERSE(@@hold)
            INITBITS()
            state.mode = DICT
        end
        if(state.mode==DICT)
            if state.havedict == 0
                RESTORE(strm,state)
                return Z_NEED_DICT
            end
            strm.adler = state.check = adler32(0, nil)
            state.mode = TYPE
        end
        if(state.mode==TYPE)
            throw :inf_leave if (flush == Z_BLOCK)
        end
        if([TYPE,TYPEDO].include?(state.mode))
            if state.last != 0
                BYTEBITS()
                state.mode = CHECK
                next
            end
            NEEDBITS(3)
            state.last = BITS(1)
            DROPBITS(1)
            case (BITS(2))
            when 0
                state.mode = STORED
            when 1
                fixedtables(state)
                state.mode = LEN
            when 2
                state.mode = TABLE
            when 3
                strm.msg = "invalid block type"
                state.mode = BAD
            end
            DROPBITS(2)
        end
        if(state.mode==STORED)
            BYTEBITS()
            NEEDBITS(32)
            if ((@@hold & 0xffff) != ((@@hold >> 16) ^ 0xffff))
                strm.msg = "invalid stored block lengths"
                state.mode = BAD
                next
            end
            state.length = @@hold & 0xffff
            INITBITS()
            state.mode = COPY
        end
        if(state.mode==COPY)
            copy = state.length
            if copy != 0
                copy = @@have if (copy > @@have)
                copy = @@left if (copy > @@left)
                throw :inf_leave if copy == 0
                @@put.buffer[@@put.offset,copy] = @@next.current[0,copy]
                @@have -= copy
                @@next += copy
                @@left -= copy
                @@put += copy
                state.length -= copy
                next
            end
            state.mode = TYPE
        end
        if(state.mode==TABLE)
            NEEDBITS(14)
            state.nlen = BITS(5) + 257
            DROPBITS(5)
            state.ndist = BITS(5) + 1
            DROPBITS(5)
            state.ncode = BITS(4) + 4
            DROPBITS(4)
            if (state.nlen > 286 || state.ndist > 30)
                strm.msg = "too many length or distance symbols"
                state.mode = BAD
                next
            end
            state.have = 0
            state.mode = LENLENS
        end
        if(state.mode==LENLENS)
            while (state.have < state.ncode)
                NEEDBITS(3)
                state.lens[order[state.have]] = BITS(3)
                state.have += 1
                DROPBITS(3)
            end
            while (state.have < 19)
                state.lens[order[state.have]] = 0
                state.have += 1
            end
            state.next = Bytef.new(state.codes)
            state.lencode = Bytef.new(state.codes)
            state.lenbits = 7
            ret,state.lenbits,state.next.offset = inflate_table(CODES, state.lens, 19, state.codes,
              state.next.offset,state.lenbits, state.work)

            if ret != 0
                strm.msg = "invalid code lengths set"
                state.mode = BAD
                next
            end
            state.have = 0
            state.mode = CODELENS
        end
        if(state.mode==CODELENS)
            while (state.have < state.nlen + state.ndist)
                this = nil
                loop do
                    this = state.lencode[BITS(state.lenbits)]
                    break if ((this.bits) <= @@bits)
                    PULLBYTE()
                end
                if (this.val < 16)
                    NEEDBITS(this.bits)
                    DROPBITS(this.bits)
                    state.lens[state.have] = this.val
                    state.have += 1
                else
                    if (this.val == 16)
                        NEEDBITS(this.bits + 2)
                        DROPBITS(this.bits)
                        if state.have == 0
                            strm.msg = "invalid bit length repeat"
                            state.mode = BAD
                            break
                        end
                        len = state.lens[state.have - 1]
                        copy = 3 + BITS(2)
                        DROPBITS(2)
                    elsif (this.val == 17)
                        NEEDBITS(this.bits + 3)
                        DROPBITS(this.bits)
                        len = 0
                        copy = 3 + BITS(3)
                        DROPBITS(3)
                    else
                        NEEDBITS(this.bits + 7)
                        DROPBITS(this.bits)
                        len = 0
                        copy = 11 + BITS(7)
                        DROPBITS(7)
                    end
                    if (state.have + copy > state.nlen + state.ndist)
                        strm.msg = "invalid bit length repeat"
                        state.mode = BAD
                        break
                    end
                    while copy != 0
                        copy -= 1
                        state.lens[state.have] = len
                        state.have+=1
                    end
                end
            end

            next if (state.mode == BAD)

            state.next = Bytef.new(state.codes)
            state.lencode = Bytef.new(state.codes)
            state.lenbits = 9
            ret,state.lenbits,state.next.offset = inflate_table(LENS, state.lens, state.nlen,
              state.codes,state.next.offset,state.lenbits, state.work)
            if ret != 0
                strm.msg = "invalid literal/lengths set"
                state.mode = BAD
                next
            end
            state.distcode = Bytef.new(state.codes,state.next.offset)
            state.distbits = 6
            ret,state.distbits,state.next.offset = inflate_table(DISTS, state.lens+state.nlen, state.ndist,
                            state.codes,state.next.offset, state.distbits, state.work)
            if ret != 0
                strm.msg = "invalid distances set"
                state.mode = BAD
                next
            end
            state.mode = LEN
        end
        if(state.mode==LEN)
            if (@@have >= 6 && @@left >= 258)
                RESTORE(strm,state)
                inflate_fast(strm, out)
                LOAD(strm,state)
                next
            end
            this = nil
            loop do
                this = state.lencode[BITS(state.lenbits)]
                break if ((this.bits) <= @@bits)
                PULLBYTE()
            end
            if (this.op != 0 && (this.op & 0xf0) == 0)
                last = this
                loop do
                    this = state.lencode[last.val +
                            (BITS(last.bits + last.op) >> last.bits)]
                    break if ((last.bits + this.bits) <= @@bits)
                    PULLBYTE()
                end
                DROPBITS(last.bits)
            end
            DROPBITS(this.bits)
            state.length = this.val
            if this.op == 0
                state.mode = LIT
                next
            end
            if (this.op & 32) != 0
                state.mode = TYPE
                next
            end
            if (this.op & 64) != 0
                strm.msg = "invalid literal/length code"
                state.mode = BAD
                next
            end
            state.extra = (this.op) & 15
            state.mode = LENEXT
        end
        if(state.mode==LENEXT)
            if state.extra != 0
                NEEDBITS(state.extra)
                state.length += BITS(state.extra)
                DROPBITS(state.extra)
            end
            state.mode = DIST
        end
        if(state.mode==DIST)
            loop do
                this = state.distcode[BITS(state.distbits)]
                break if ((this.bits) <= @@bits)
                PULLBYTE()
            end
            if (this.op & 0xf0) == 0
                last = this
                loop do
                    this = state.distcode[last.val +
                            (BITS(last.bits + last.op) >> last.bits)]
                    break if ((last.bits + this.bits) <= @@bits)
                    PULLBYTE()
                end
                DROPBITS(last.bits)
            end
            DROPBITS(this.bits)
            if (this.op & 64) != 0
                strm.msg = "invalid distance code"
                state.mode = BAD
                next
            end
            state.offset = this.val
            state.extra = (this.op) & 15
            state.mode = DISTEXT
        end
        if(state.mode==DISTEXT)
            if state.extra != 0
                NEEDBITS(state.extra)
                state.offset += BITS(state.extra)
                DROPBITS(state.extra)
            end
            if (state.offset > state.whave + out - @@left)
                strm.msg = "invalid distance too far back"
                state.mode = BAD
                next
            end
            state.mode = MATCH
        end
        if(state.mode==MATCH)
            throw :inf_leave if @@left == 0
            copy = out - @@left
            if (state.offset > copy)
                copy = state.offset - copy
                if (copy > state.write)
                    copy -= state.write
                    from = Bytef.new(state.window,state.wsize - copy)
                else
                    from = Bytef.new(state.window,state.write - copy)
                end
                copy = state.length if (copy > state.length)
            else
                from = Bytef.new(@@put,@@put.offset - state.offset)
                copy = state.length
            end
            copy = @@left if (copy > @@left)
            @@left -= copy
            state.length -= copy
            loop do
                @@put.set(from.get)
                @@put += 1
                from += 1
                copy-=1
                break if copy == 0
            end
            state.mode = LEN if state.length == 0
        end
        if(state.mode==LIT)
            throw :inf_leave if @@left == 0
            @@put.set(state.length)
            @@put += 1
            @@left-=1
            state.mode = LEN
        end
        if(state.mode==CHECK)
            if state.wrap != 0
                NEEDBITS(32)
                out -= @@left
                strm.total_out += out
                state.total += out
                if out != 0
                    strm.adler = state.check =
                        UPDATE(state, state.check, @@put.buffer[@@put.offset - out, out])
                end
                out = @@left
                if ((state.flags != 0 ? @@hold : REVERSE(@@hold)) != state.check)
                    strm.msg = "incorrect data check"
                    state.mode = BAD
                    next
                end
                INITBITS()
            end
            state.mode = LENGTH
        end
        if(state.mode==LENGTH)
            if (state.wrap != 0 && state.flags != 0)
                NEEDBITS(32)
                if (@@hold != (state.total & 0xffffffff))
                    strm.msg = "incorrect length check"
                    state.mode = BAD
                    next
                end
                INITBITS()
            end
            state.mode = DONE
        end
        if(state.mode==DONE)
            ret = Z_STREAM_END
            throw :inf_leave
        end
        if(state.mode==BAD)
            ret = Z_DATA_ERROR
            throw :inf_leave
        elsif(state.mode==MEM)
            return Z_MEM_ERROR
        elsif(state.mode==SYNC)
            return Z_STREAM_ERROR
        end
      end
    end

    RESTORE(strm,state)

    if (state.wsize != 0 || (state.mode < CHECK && out != strm.avail_out))
        if (updatewindow(strm, out))
            state.mode = MEM
            return Z_MEM_ERROR
        end
    end

    _in -= strm.avail_in
    out -= strm.avail_out
    strm.total_in += _in
    strm.total_out += out
    state.total += out

    if (state.wrap != 0 && out != 0)
        strm.adler = state.check =
            UPDATE(state, state.check, strm.next_out.buffer[strm.next_out.offset - out, out])
    end
    strm.data_type = state.bits + (state.last != 0 ? 64 : 0) +
                      (state.mode == TYPE ? 128 : 0)
    if (((_in == 0 && out == 0) || flush == Z_FINISH) && ret == Z_OK)
        ret = Z_BUF_ERROR
    end
    return ret
  end

  #
  def inflateEnd(strm)
    if (strm.nil? || strm.state.nil?)
        return Z_STREAM_ERROR
    end
    state = strm.state
    state.window = nil
    strm.state = nil
    return Z_OK
  end

  #
  def inflateSetDictionary(strm, dictionary, dictLength)
    return Z_STREAM_ERROR if (strm.nil? || strm.state.nil?)
    state = strm.state
    if (state.wrap != 0 && state.mode != DICT)
        return Z_STREAM_ERROR
    end

    if (state.mode == DICT)
        id = adler32(0, nil)
        id = adler32(id, dictionary, dictLength)
        if (id != state.check)
            return Z_DATA_ERROR
        end
    end

    if (updatewindow(strm, strm.avail_out))
        state.mode = MEM
        return Z_MEM_ERROR
    end
    if (dictLength > state.wsize)
        state.window[0,state.wsize] =
          dictionary[dictLength - state.wsize,state.wsize]
        state.whave = state.wsize
    else
        state.window[state.wsize - dictLength,dictLength] =
          dictionary[0,dictLength]
        state.whave = dictLength
    end
    state.havedict = 1
    return Z_OK
  end

  #
  def inflateGetHeader(strm, head)
    return Z_STREAM_ERROR if (strm.nil? || strm.state.nil?)
    state = strm.state
    return Z_STREAM_ERROR if (state.wrap & 2) == 0

    state.head = head
    head.done = 0
    return Z_OK
  end

  # Search buf[0..len-1] for the pattern: 0, 0, 0xff, 0xff.  Return when found
  # or when out of input.  When called, *have is the number of pattern bytes
  # found in order so far, in 0..3.  On return *have is updated to the new
  # state.  If on return *have equals four, then the pattern was found and the
  # return value is how many bytes were read including the last byte of the
  # pattern.  If *have is less than four, then the pattern has not been found
  # yet and the return value is len.  In the latter case, syncsearch() can be
  # called again with more data and the *have state.  *have is initialized to
  # zero for the first call.
  def syncsearch(have, buf, len)
    got = have
    _next = 0
    while (_next < len && got < 4)
        if ((buf[_next]) == (got < 2 ? 0 : 0xff))
            got+=1
        elsif buf[_next] != 0
            got = 0
        else
            got = 4 - got
        end
        _next+=1
    end
    have = got
    return [_next,have]
  end

  #
  def inflateSync(strm)
    return Z_STREAM_ERROR if (strm.nil? || strm.state.nil?)
    state = strm.state
    return Z_BUF_ERROR if (strm.avail_in == 0 && state.bits < 8)

    buf = 0.chr * 4
    if (state.mode != SYNC)
        state.mode = SYNC
        state.hold <<= state.bits & 7
        state.bits -= state.bits & 7
        len = 0
        while (state.bits >= 8)
            buf[len] = (state.hold).chr
            len+=1
            state.hold >>= 8
            state.bits -= 8
        end
        state.have = 0
        _,state.have = syncsearch((state.have), buf, len)
    end

    len,state.have = syncsearch((state.have), strm.next_in, strm.avail_in)
    strm.avail_in -= len
    strm.next_in += len
    strm.total_in += len
    return Z_DATA_ERROR if (state.have != 4)
    _in = strm.total_in
    out = strm.total_out
    inflateReset(strm)
    strm.total_in = _in
    strm.total_out = out
    state.mode = TYPE
    return Z_OK
  end

  # Returns true if inflate is currently at the end of a block generated by
  # Z_SYNC_FLUSH or Z_FULL_FLUSH. This function is used by one PPP
  # implementation to provide an additional safety check. PPP uses
  # Z_SYNC_FLUSH but removes the length bytes of the resulting empty stored
  # block. When decompressing, PPP checks that at the end of input packet,
  # inflate is waiting for these length bytes.
  def inflateSyncPoint(strm)
    return Z_STREAM_ERROR if (strm.nil? || strm.state.nil?)
    state = strm.state
    return state.mode == STORED && state.bits == 0
  end

  #
  def inflateCopy(dest, source)
    if (dest.nil? || source.nil? || source.state.nil?)
        return Z_STREAM_ERROR
    end
    state = source.state

    copy = Inflate_state.new()
    return Z_MEM_ERROR if copy.nil?
    window = nil
    if state.window
        window = 0.chr * (1 << state.wbits)
        if window.nil?
            copy = nil
            return Z_MEM_ERROR
        end
    end

    dest = source.dup
    copy = state.dup
    if (state.lencode.offset >= 0 &&
        state.lencode.offset <= 0 + ENOUGH - 1)
        copy.lencode.offset = 0 + (state.lencode.offset - 0)
        copy.distcode.offset = 0 + (state.distcode.offset - 0)
    end
    copy.next.offset = 0 + (state.next.offset - 0)
    if window
        wsize = 1 << state.wbits
        window[0,wsize] = state.window[0,wsize]
    end
    copy.window = window
    dest.state = copy
    return Z_OK
  end

  #   Decompresses the source buffer into the destination buffer.  sourceLen is
  # the byte length of the source buffer. Upon entry, destLen is the total
  # size of the destination buffer, which must be large enough to hold the
  # entire uncompressed data. (The size of the uncompressed data must have
  # been saved previously by the compressor and transmitted to the decompressor
  # by some mechanism outside the scope of this compression library.)
  # Upon exit, destLen is the actual size of the compressed buffer.
  #   This function can be used to decompress a whole file at once if the
  # input file is mmap'ed.
  #
  #   uncompress returns Z_OK if success, Z_MEM_ERROR if there was not
  # enough memory, Z_BUF_ERROR if there was not enough room in the output
  # buffer, or Z_DATA_ERROR if the input data was corrupted.
  def uncompress(dest,destLen,source,sourceLen)
    stream = Z_stream.new
    stream.next_in = Bytef.new(source)
    stream.avail_in = sourceLen
    return [Z_BUF_ERROR,destLen] if (stream.avail_in != sourceLen)

    stream.next_out = Bytef.new(dest)
    stream.avail_out = destLen
    return [Z_BUF_ERROR,destLen] if (stream.avail_out != destLen)

    err = inflateInit(stream)
    return [err,destLen] if (err != Z_OK)

    err = inflate(stream, Z_FINISH)
    if (err != Z_STREAM_END)
        inflateEnd(stream)
        if (err == Z_NEED_DICT || (err == Z_BUF_ERROR && stream.avail_in == 0))
            return [Z_DATA_ERROR,destLen]
        end
        return [err,destLen]
    end
    destLen = stream.total_out

    err = inflateEnd(stream)
    return [err,destLen]
  end


end
