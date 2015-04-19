# Copyright (c) 2007-2014, Evan Phoenix and contributors
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# * Redistributions of source code must retain the above copyright notice, this
#   list of conditions and the following disclaimer.
# * Redistributions in binary form must reproduce the above copyright notice
#   this list of conditions and the following disclaimer in the documentation
#   and/or other materials provided with the distribution.
# * Neither the name of Rubinius nor the names of its contributors
#   may be used to endorse or promote products derived from this software
#   without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# IO#setup modified to not refer to STDOUT and STDERR if they aren't defined,
# as we create those as files using normal File.new.

class IO
  FFI = Rubinius::FFI

  include Enumerable

  module WaitReadable; end
  module WaitWritable; end

  class EAGAINWaitReadable < Errno::EAGAIN
    include ::IO::WaitReadable
  end

  class EAGAINWaitWritable < Errno::EAGAIN
    include ::IO::WaitWritable
  end

  # Import platform constants

  SEEK_SET = Rubinius::Config['rbx.platform.io.SEEK_SET']
  SEEK_CUR = Rubinius::Config['rbx.platform.io.SEEK_CUR']
  SEEK_END = Rubinius::Config['rbx.platform.io.SEEK_END']

  # InternalBuffer provides a sliding window into a region of bytes.
  # The buffer is filled to the +used+ indicator, which is
  # always less than or equal to +total+. As bytes are taken
  # from the buffer, the +start+ indicator is incremented by
  # the number of bytes taken. Once +start+ == +used+, the
  # buffer is +empty?+ and needs to be refilled.
  #
  # This description should be independent of the "direction"
  # in which the buffer is used. As a read buffer, +fill_from+
  # appends at +used+, but not exceeding +total+. When +used+
  # equals total, no additional bytes will be filled until the
  # buffer is emptied.
  #
  # As a write buffer, +empty_to+ removes bytes from +start+ up
  # to +used+. When +start+ equals +used+, no additional bytes
  # will be emptied until the buffer is filled.
  #
  # IO presents a stream of input. Buffer presents buckets of
  # input. IO's task is to chain the buckets so the user sees
  # a stream. IO explicitly requests that the buffer be filled
  # (on input) and then determines how much of the input to take
  # (e.g. by looking for a separator or collecting a certain
  # number of bytes). Buffer decides whether or not to go to the
  # source for more data or just present what is already in the
  # buffer.
  class InternalBuffer

    attr_reader :total
    attr_reader :start
    attr_reader :used

    ##
    # Returns +true+ if the buffer can be filled.
    def empty?
      @start == @used
    end

    ##
    # Returns +true+ if the buffer is empty and cannot be filled further.
    def exhausted?
      @eof and empty?
    end

    ##
    # A request to the buffer to have data. The buffer decides whether
    # existing data is sufficient, or whether to read more data from the
    # +IO+ instance. Any new data causes this method to return.
    #
    # Returns the number of bytes in the buffer.
    def fill_from(io, skip = nil)
      Rubinius.synchronize(self) do
        empty_to io
        discard skip if skip

        return size unless empty?

        reset!

        if fill(io) < 0
          raise IOError, "error occurred while filling buffer (#{obj})"
        end

        if @used == 0
          io.eof!
          @eof = true
        end

        return size
      end
    end

    def empty_to(io)
      return 0 if @write_synced or empty?
      @write_synced = true

      io.prim_write(String.from_bytearray(@storage, @start, size))
      reset!

      return size
    end

    ##
    # Advances the beginning-of-buffer marker past any number
    # of contiguous characters == +skip+. For example, if +skip+
    # is ?\n and the buffer contents are "\n\n\nAbc...", the
    # start marker will be positioned on 'A'.
    def discard(skip)
      while @start < @used
        break unless @storage[@start] == skip
        @start += 1
      end
    end

    ##
    # Returns the number of bytes to fetch from the buffer up-to-
    # and-including +pattern+. Returns +nil+ if pattern is not found.
    def find(pattern, discard = nil)
      if count = @storage.locate(pattern, @start, @used)
        count - @start
      end
    end

    ##
    # Returns +true+ if the buffer is filled to capacity.
    def full?
      @total == @used
    end

    def inspect # :nodoc:
      "#<IO::InternalBuffer:0x%x total=%p start=%p used=%p data=%p>" % [
        object_id, @total, @start, @used, @storage
      ]
    end

    ##
    # Resets the buffer state so the buffer can be filled again.
    def reset!
      @start = @used = 0
      @eof = false
      @write_synced = true
    end

    def write_synced?
      @write_synced
    end

    def unseek!(io)
      Rubinius.synchronize(self) do
        # Unseek the still buffered amount
        return unless write_synced?
        io.prim_seek @start - @used, IO::SEEK_CUR unless empty?
        reset!
      end
    end

    ##
    # Returns +count+ bytes from the +start+ of the buffer as a new String.
    # If +count+ is +nil+, returns all available bytes in the buffer.
    def shift(count=nil)
      Rubinius.synchronize(self) do
        total = size
        total = count if count and count < total

        str = String.from_bytearray @storage, @start, total
        @start += total

        str
      end
    end

    PEEK_AHEAD_LIMIT = 16

    def read_to_char_boundary(io, str)
      str.force_encoding(io.external_encoding || Encoding.default_external)
      return IO.read_encode(io, str) if str.valid_encoding?

      peek_ahead = 0
      while size > 0 and peek_ahead < PEEK_AHEAD_LIMIT
        str.force_encoding Encoding::ASCII_8BIT
        str << @storage[@start]
        @start += 1
        peek_ahead += 1

        str.force_encoding(io.external_encoding || Encoding.default_external)
        if str.valid_encoding?
          return IO.read_encode io, str
        end
      end

      IO.read_encode io, str
    end

    ##
    # Returns one Fixnum as the start byte.
    def getbyte(io)
      return if size == 0 and fill_from(io) == 0

      Rubinius.synchronize(self) do
        byte = @storage[@start]
        @start += 1
        byte
      end
    end

    # TODO: fix this when IO buffering is re-written.
    def getchar(io)
      return if size == 0 and fill_from(io) == 0

      Rubinius.synchronize(self) do
        char = ""
        while size > 0
          char.force_encoding Encoding::ASCII_8BIT
          char << @storage[@start]
          @start += 1

          char.force_encoding(io.external_encoding || Encoding.default_external)
          if char.chr_at(0)
            return IO.read_encode io, char
          end
        end
      end
    end

    ##
    # Prepends the byte +chr+ to the internal buffer, so that future
    # reads will return it.
    def put_back(chr)
      # A simple case, which is common and can be done efficiently
      if @start > 0
        @start -= 1
        @storage[@start] = chr
      else
        @storage = @storage.prepend(chr.chr)
        @start = 0
        @total = @storage.size
        @used += 1
      end
    end

    ##
    # Returns the number of bytes available in the buffer.
    def size
      @used - @start
    end

    ##
    # Returns the number of bytes of capacity remaining in the buffer.
    # This is the number of additional bytes that can be added to the
    # buffer before it is full.
    def unused
      @total - @used
    end
  end

  attr_accessor :descriptor
  attr_accessor :external
  attr_accessor :internal
  attr_accessor :mode

  def self.binread(file, length=nil, offset=0)
    raise ArgumentError, "Negative length #{length} given" if !length.nil? && length < 0

    File.open(file, "r", :encoding => "ascii-8bit:-") do |f|
      f.seek(offset)
      f.read(length)
    end
  end

  def self.binwrite(file, string, *args)
    offset, opts = args
    opts ||= {}
    if offset.is_a?(Hash)
      offset, opts = nil, offset
    end

    mode, binary, external, internal, autoclose = IO.normalize_options(nil, opts)
    unless mode
      mode = File::CREAT | File::RDWR | File::BINARY
      mode |= File::TRUNC unless offset
    end
    File.open(file, mode, :encoding => (external || "ASCII-8BIT")) do |f|
      f.seek(offset || 0)
      f.write(string)
    end
  end

  class StreamCopier
    def initialize(from, to, length, offset)
      @length = length
      @offset = offset

      @from_io, @from = to_io(from, "rb")
      @to_io, @to = to_io(to, "wb")

      @method = read_method @from
    end

    def to_io(obj, mode)
      if obj.kind_of? IO
        flag = true
        io = obj
      else
        flag = false

        if obj.kind_of? String
          io = File.open obj, mode
        elsif obj.respond_to? :to_path
          path = Rubinius::Type.coerce_to obj, String, :to_path
          io = File.open path, mode
        else
          io = obj
        end
      end

      return flag, io
    end

    def read_method(obj)
      if obj.respond_to? :readpartial
        :readpartial
      else
        :read
      end
    end

    def run
      @from.ensure_open_and_readable if @from.kind_of? IO
      @to.ensure_open_and_writable if @to.kind_of? IO

      if @offset
        if @from_io && !@from.pipe?
          saved_pos = @from.pos
        else
          saved_pos = 0
        end

        @from.seek @offset, IO::SEEK_CUR
      end

      size = @length ? @length : 16384
      bytes = 0

      begin
        while data = @from.__send__(@method, size, "")
          @to.write data
          bytes += data.bytesize

          break if @length && bytes >= @length
        end
      rescue EOFError
        # done reading
      end

      @to.flush if @to.kind_of? IO
      return bytes
    ensure
      if @from_io
        @from.pos = saved_pos if @offset
      else
        @from.close if @from.kind_of? IO
      end

      @to.close if @to.kind_of? IO unless @to_io
    end
  end

  def self.copy_stream(from, to, max_length=nil, offset=nil)
    StreamCopier.new(from, to, max_length, offset).run
  end

  def self.foreach(name, separator=undefined, limit=undefined, options=undefined)
    return to_enum(:foreach, name, separator, limit, options) unless block_given?

    name = Rubinius::Type.coerce_to_path name

    case separator
    when Fixnum
      options = limit
      limit = separator
      separator = $/
    when undefined
      separator = $/
    when Hash
      options = separator
      separator = $/
    when nil
      # do nothing
    else
      separator = StringValue(separator)
    end

    case limit
    when Fixnum, nil
      # do nothing
    when undefined
      limit = nil
    when Hash
      if undefined.equal? options
        options = limit
        limit = nil
      else
        raise TypeError, "can't convert Hash into Integer"
      end
    else
      value = limit
      limit = Rubinius::Type.try_convert limit, Fixnum, :to_int

      unless limit
        options = Rubinius::Type.coerce_to value, Hash, :to_hash
      end
    end

    case options
    when Hash
      # do nothing
    when undefined, nil
      options = { }
    else
      options = Rubinius::Type.coerce_to options, Hash, :to_hash
    end

    saved_line = $_

    if name[0] == ?|
      io = IO.popen(name[1..-1], "r")
      return nil unless io
    else
      options[:mode] = "r" unless options.key? :mode
      io = File.open(name, options)
    end

    begin
      while line = io.gets(separator, limit)
        yield line
      end
    ensure
      $_ = saved_line
      io.close
    end

    return nil
  end

  def self.readlines(name, separator=undefined, limit=undefined, options=undefined)
    lines = []
    foreach(name, separator, limit, options) { |l| lines << l }

    lines
  end

  def self.read_encode(io, str)
    internal = io.internal_encoding
    external = io.external_encoding || Encoding.default_external

    if external.equal? Encoding::ASCII_8BIT
      str.force_encoding external
    elsif internal and external
      ec = Encoding::Converter.new external, internal
      ec.convert str
    else
      str.force_encoding external
    end
  end

  def self.write(file, string, *args)
    if args.size > 2
      raise ArgumentError, "wrong number of arguments (#{args.size + 2} for 2..3)"
    end

    offset, opts = args
    opts ||= {}
    if offset.is_a?(Hash)
      offset, opts = nil, offset
    end

    mode, binary, external, internal, autoclose = IO.normalize_options(nil, opts)
    unless mode
      mode = File::CREAT | File::WRONLY
      mode |= File::TRUNC unless offset
    end

    open_args = opts[:open_args] || [mode, :encoding => (external || "ASCII-8BIT")]
    File.open(file, *open_args) do |f|
      f.seek(offset) if offset
      f.write(string)
    end
  end

  def self.for_fd(fd, mode=undefined, options=undefined)
    new fd, mode, options
  end

  def self.read(name, length_or_options=undefined, offset=0, options=nil)
    offset = 0 if offset.nil?
    name = Rubinius::Type.coerce_to_path name
    mode = "r"

    if undefined.equal? length_or_options
      length = undefined
    elsif Rubinius::Type.object_kind_of? length_or_options, Hash
      length = undefined
      offset = 0
      options = length_or_options
    elsif length_or_options
      offset = Rubinius::Type.coerce_to(offset || 0, Fixnum, :to_int)
      raise Errno::EINVAL, "offset must not be negative" if offset < 0

      length = Rubinius::Type.coerce_to(length_or_options, Fixnum, :to_int)
      raise ArgumentError, "length must not be negative" if length < 0
    else
      length = undefined
    end

    if options
      mode = options.delete(:mode) || "r"
    end

    # Detect pipe mode
    if name[0] == ?|
      io = IO.popen(name[1..-1], "r")
      return nil unless io # child process
    else
      io = File.new(name, mode, options)
    end

    str = nil
    begin
      io.seek(offset) unless offset == 0

      if undefined.equal?(length)
        str = io.read
      else
        str = io.read length
      end
    ensure
      io.close
    end

    return str
  end

  def self.try_convert(obj)
    Rubinius::Type.try_convert obj, IO, :to_io
  end

  def self.normalize_options(mode, options)
    mode = nil if undefined.equal?(mode)
    autoclose = true

    if undefined.equal?(options)
      options = Rubinius::Type.try_convert(mode, Hash, :to_hash)
      mode = nil if options
    elsif !options.nil?
      options = Rubinius::Type.try_convert(options, Hash, :to_hash)
      raise ArgumentError, "wrong number of arguments (3 for 1..2)" unless options
    end

    if mode
      mode = (Rubinius::Type.try_convert(mode, Integer, :to_int) or
              Rubinius::Type.coerce_to(mode, String, :to_str))
    end

    if options
      if optmode = options[:mode]
        optmode = (Rubinius::Type.try_convert(optmode, Integer, :to_int) or
                   Rubinius::Type.coerce_to(optmode, String, :to_str))
      end

      if mode
        raise ArgumentError, "mode specified twice" if optmode
      else
        mode = optmode
      end

      autoclose = !!options[:autoclose] if options.key?(:autoclose)
    end

    if mode.kind_of?(String)
      mode, external, internal = mode.split(":")
      raise ArgumentError, "invalid access mode" unless mode

      binary = true  if mode[1] === ?b
      binary = false if mode[1] === ?t
    elsif mode
      binary = true  if (mode & BINARY) != 0
    end

    if options
      if options[:textmode] and options[:binmode]
        raise ArgumentError, "both textmode and binmode specified"
      end

      if binary.nil?
        binary = options[:binmode]
      elsif options.key?(:textmode) or options.key?(:binmode)
        raise ArgumentError, "text/binary mode specified twice"
      end

      if !external and !internal
        external = options[:external_encoding]
        internal = options[:internal_encoding]
      elsif options[:external_encoding] or options[:internal_encoding] or options[:encoding]
        raise ArgumentError, "encoding specified twice"
      end

      if !external and !internal
        encoding = options[:encoding]

        if encoding.kind_of? Encoding
          external = encoding
        elsif !encoding.nil?
          encoding = StringValue(encoding)
          external, internal = encoding.split(':')
        end
      end
    end

    [mode, binary, external, internal, autoclose]
  end

  def self.open(*args)
    io = new(*args)

    return io unless block_given?

    begin
      yield io
    ensure
      begin
        io.close unless io.closed?
      rescue StandardError
        # nothing, just swallow them.
      end
    end
  end

  def self.parse_mode(mode)
    return mode if Rubinius::Type.object_kind_of? mode, Integer

    mode = StringValue(mode)

    ret = 0

    case mode[0]
    when ?r
      ret |= RDONLY
    when ?w
      ret |= WRONLY | CREAT | TRUNC
    when ?a
      ret |= WRONLY | CREAT | APPEND
    else
      raise ArgumentError, "invalid mode -- #{mode}"
    end

    return ret if mode.length == 1

    case mode[1]
    when ?+
      ret &= ~(RDONLY | WRONLY)
      ret |= RDWR
    when ?b
      ret |= BINARY
    when ?t
      ret &= ~BINARY
    when ?:
      warn("encoding options not supported in 1.8")
      return ret
    else
      raise ArgumentError, "invalid mode -- #{mode}"
    end

    return ret if mode.length == 2

    case mode[2]
    when ?+
      ret &= ~(RDONLY | WRONLY)
      ret |= RDWR
    when ?b
      ret |= BINARY
    when ?t
      ret &= ~BINARY
    when ?:
      warn("encoding options not supported in 1.8")
      return ret
    else
      raise ArgumentError, "invalid mode -- #{mode}"
    end

    ret
  end

  def self.pipe(external=nil, internal=nil, options=nil)
    lhs = allocate
    rhs = allocate

    connect_pipe(lhs, rhs)

    lhs.set_encoding external || Encoding.default_external,
                     internal || Encoding.default_internal, options

    lhs.sync = true
    rhs.sync = true

    lhs.pipe = true
    rhs.pipe = true

    if block_given?
      begin
        yield lhs, rhs
      ensure
        lhs.close unless lhs.closed?
        rhs.close unless rhs.closed?
      end
    else
      [lhs, rhs]
    end
  end

  def self.popen(*args)
    if env = Rubinius::Type.try_convert(args.first, Hash, :to_hash)
      args.shift
    end

    if io_options = Rubinius::Type.try_convert(args.last, Hash, :to_hash)
      args.pop
    end

    if args.size > 2
      raise ArgumentError, "#{__method__}: given #{args.size}, expected 1..2"
    end

    cmd, mode = args
    mode ||= "r"

    if cmd.kind_of? Array
      if sub_env = Rubinius::Type.try_convert(cmd.first, Hash, :to_hash)
        env = sub_env unless env
        cmd.shift
      end

      if exec_options = Rubinius::Type.try_convert(cmd.last, Hash, :to_hash)
        cmd.pop
      end
    end

    mode, binary, external, internal, autoclose =
      IO.normalize_options(mode, io_options || {})
    mode_int = parse_mode mode

    readable = false
    writable = false

    if mode_int & IO::RDWR != 0
      readable = true
      writable = true
    elsif mode_int & IO::WRONLY != 0
      writable = true
    else # IO::RDONLY
      readable = true
    end

    pa_read, ch_write = pipe if readable
    ch_read, pa_write = pipe if writable

    # We only need the Bidirectional pipe if we're reading and writing.
    # If we're only doing one, we can just return the IO object for
    # the proper half.
    if readable and writable
      # Transmogrify pa_read into a BidirectionalPipe object,
      # and then tell it abou it's pid and pa_write

      Rubinius::Unsafe.set_class pa_read, IO::BidirectionalPipe

      pipe = pa_read
      pipe.set_pipe_info(pa_write)
    elsif readable
      pipe = pa_read
    elsif writable
      pipe = pa_write
    else
      raise ArgumentError, "IO is neither readable nor writable"
    end

    pipe.binmode if binary
    pipe.set_encoding(external || Encoding.default_external, internal)

    if cmd == "-"
      pid = Rubinius::Mirror::Process.fork

      if !pid
        # Child
        begin
          if readable
            pa_read.close
            STDOUT.reopen ch_write
          end

          if writable
            pa_write.close
            STDIN.reopen ch_read
          end

          if block_given?
            yield nil
            exit! 0
          else
            return nil
          end
        rescue
          exit! 0
        end
      end
    else
      options = {}
      options[:in] = ch_read.fileno if ch_read
      options[:out] = ch_write.fileno if ch_write

      if io_options
        io_options.delete_if do |key, _|
          [:mode, :external_encoding, :internal_encoding,
            :encoding, :textmode, :binmode, :autoclose
          ].include? key
        end

        options.merge! io_options
      end

      if exec_options
        options.merge! exec_options
      end

      pid = Rubinius::Mirror::Process.spawn(env || {}, *cmd, options)
    end

    pipe.pid = pid

    ch_write.close if readable
    ch_read.close  if writable

    return pipe unless block_given?

    begin
      yield pipe
    ensure
      pipe.close unless pipe.closed?
    end
  end

  #
  # +select+ examines the IO object Arrays that are passed in
  # as +readables+, +writables+, and +errorables+ to see if any
  # of their descriptors are ready for reading, are ready for
  # writing, or have an exceptions pending respectively. An IO
  # may appear in more than one of the sets. Any of the three
  # sets may be +nil+ if you are not interested in those events.
  #
  # If +timeout+ is not nil, it specifies the number of seconds
  # to wait for events (maximum.) The number may be fractional,
  # conceptually up to a microsecond resolution.
  #
  # A +timeout+ of 0 indicates that each descriptor should be
  # checked once only, effectively polling the sets.
  #
  # Leaving the +timeout+ to +nil+ causes +select+ to block
  # infinitely until an event transpires.
  #
  # If the timeout expires without events, +nil+ is returned.
  # Otherwise, an [readable, writable, errors] Array of Arrays
  # is returned, only, with the IO objects that have events.
  #
  # @compatibility  MRI 1.8 and 1.9 require the +readables+ Array,
  #                 Rubinius does not.
  #
  def self.select(readables=nil, writables=nil, errorables=nil, timeout=nil)
    if timeout
      unless Rubinius::Type.object_kind_of? timeout, Numeric
        raise TypeError, "Timeout must be numeric"
      end

      raise ArgumentError, 'timeout must be positive' if timeout < 0

      # Microseconds, rounded down
      timeout = Integer(timeout * 1_000_000)
    end

    if readables
      readables =
        Rubinius::Type.coerce_to(readables, Array, :to_ary).map do |obj|
          if obj.kind_of? IO
            raise IOError, "closed stream" if obj.closed?
            return [[obj],[],[]] unless obj.buffer_empty?
            obj
          else
            io = Rubinius::Type.coerce_to(obj, IO, :to_io)
            raise IOError, "closed stream" if io.closed?
            [obj, io]
          end
        end
    end

    if writables
      writables =
        Rubinius::Type.coerce_to(writables, Array, :to_ary).map do |obj|
          if obj.kind_of? IO
            raise IOError, "closed stream" if obj.closed?
            obj
          else
            io = Rubinius::Type.coerce_to(obj, IO, :to_io)
            raise IOError, "closed stream" if io.closed?
            [obj, io]
          end
        end
    end

    if errorables
      errorables =
        Rubinius::Type.coerce_to(errorables, Array, :to_ary).map do |obj|
          if obj.kind_of? IO
            raise IOError, "closed stream" if obj.closed?
            obj
          else
            io = Rubinius::Type.coerce_to(obj, IO, :to_io)
            raise IOError, "closed stream" if io.closed?
            [obj, io]
          end
        end
    end

    IO.select_primitive(readables, writables, errorables, timeout)
  end

  ##
  # Opens the given path, returning the underlying file descriptor as a Fixnum.
  #  IO.sysopen("testfile")   #=> 3
  def self.sysopen(path, mode = nil, perm = nil)
    path = Rubinius::Type.coerce_to_path path
    mode = parse_mode(mode || "r")
    perm ||= 0666

    open_with_mode path, mode, perm
  end

  #
  # Internally associate +io+ with the given descriptor.
  #
  # The +mode+ will be checked and set as the current mode if
  # the underlying descriptor allows it.
  #
  # The +sync+ attribute will also be set.
  #
  def self.setup(io, fd, mode=nil, sync=false)
    cur_mode = FFI::Platform::POSIX.fcntl(fd, F_GETFL, 0)
    Errno.handle if cur_mode < 0

    cur_mode &= ACCMODE

    if mode
      mode = parse_mode(mode)
      mode &= ACCMODE

      if (cur_mode == RDONLY or cur_mode == WRONLY) and mode != cur_mode
        raise Errno::EINVAL, "Invalid new mode for existing descriptor #{fd}"
      end
    end

    io.descriptor = fd
    io.mode       = mode || cur_mode
    io.sync       = !!sync

    if defined? STDOUT and STDOUT.respond_to?(:fileno) and not STDOUT.closed?
      io.sync ||= STDOUT.fileno == fd
    end

    if defined? STDERR and STDERR.respond_to?(:fileno) and not STDERR.closed?
      io.sync ||= STDERR.fileno == fd
    end
  end

  def self.max_open_fd
    @max_open_fd.get
  end

  #
  # Create a new IO associated with the given fd.
  #
  def initialize(fd, mode=undefined, options=undefined)
    if block_given?
      warn 'IO::new() does not take block; use IO::open() instead'
    end

    mode, binary, external, internal, @autoclose = IO.normalize_options(mode, options)

    IO.setup self, Rubinius::Type.coerce_to(fd, Integer, :to_int), mode

    binmode if binary
    set_encoding external, internal

    if @external && !external
      @external = nil
    end

    if @internal
      if Encoding.default_external == Encoding.default_internal or
         (@external || Encoding.default_external) == Encoding::ASCII_8BIT
        @internal = nil
      end
    elsif @mode != RDONLY
      if Encoding.default_external != Encoding.default_internal
        @internal = Encoding.default_internal
      end
    end

    unless @external
      if @binmode
        @external = Encoding::ASCII_8BIT
      elsif @internal or Encoding.default_internal
        @external = Encoding.default_external
      end
    end

    @pipe = false
  end

  private :initialize

  ##
  # Obtains a new duplicate descriptor for the current one.
  def initialize_copy(original) # :nodoc:
    @descriptor = FFI::Platform::POSIX.dup(@descriptor)
  end

  private :initialize_copy

  alias_method :prim_write, :write
  alias_method :prim_close, :close

  def advise(advice, offset = 0, len = 0)
    raise IOError, "stream is closed" if closed?
    raise TypeError, "advice must be a Symbol" unless advice.kind_of?(Symbol)

    if offset.kind_of?(Bignum) || len.kind_of?(Bignum)
      raise RangeError, "bignum too big to convert into `long'"
    end

    unless [:normal, :sequential, :random, :noreuse, :dontneed, :willneed].include? advice
      raise NotImplementedError, "Unsupported advice: #{advice}"
    end

    offset = Rubinius::Type.coerce_to offset, Integer, :to_int
    len = Rubinius::Type.coerce_to len, Integer, :to_int

    Rubinius.primitive :io_advise
    nil
  end

  def autoclose?
    @autoclose
  end

  def autoclose=(autoclose)
    @autoclose = !!autoclose
  end

  def binmode
    ensure_open

    @binmode = true
    @external = Encoding::BINARY
    @internal = nil

    # HACK what to do?
    self
  end

  def binmode?
    !@binmode.nil?
  end
  # Used to find out if there is buffered data available.
  def buffer_empty?
    @ibuffer.empty?
  end

  def close_on_exec=(value)
    if value
      fcntl(F_SETFD, fcntl(F_GETFD) | FD_CLOEXEC)
    else
      fcntl(F_SETFD, fcntl(F_GETFD) & ~FD_CLOEXEC)
    end
    nil
  end

  def close_on_exec?
    (fcntl(F_GETFD) & FD_CLOEXEC) != 0
  end

  def <<(obj)
    write(obj.to_s)
    return self
  end

  ##
  # Closes the read end of a duplex I/O stream (i.e., one
  # that contains both a read and a write stream, such as
  # a pipe). Will raise an IOError if the stream is not duplexed.
  #
  #  f = IO.popen("/bin/sh","r+")
  #  f.close_read
  #  f.readlines
  # produces:
  #
  #  prog.rb:3:in `readlines': not opened for reading (IOError)
  #   from prog.rb:3
  def close_read
    if @mode == WRONLY || @mode == RDWR
      raise IOError, 'closing non-duplex IO for reading'
    end
    close
  end

  ##
  # Closes the write end of a duplex I/O stream (i.e., one
  # that contains both a read and a write stream, such as
  # a pipe). Will raise an IOError if the stream is not duplexed.
  #
  #  f = IO.popen("/bin/sh","r+")
  #  f.close_write
  #  f.print "nowhere"
  # produces:
  #
  #  prog.rb:3:in `write': not opened for writing (IOError)
  #   from prog.rb:3:in `print'
  #   from prog.rb:3
  def close_write
    if @mode == RDONLY || @mode == RDWR
      raise IOError, 'closing non-duplex IO for writing'
    end
    close
  end

  ##
  # Returns true if ios is completely closed (for duplex
  # streams, both reader and writer), false otherwise.
  #
  #  f = File.new("testfile")
  #  f.close         #=> nil
  #  f.closed?       #=> true
  #  f = IO.popen("/bin/sh","r+")
  #  f.close_write   #=> nil
  #  f.closed?       #=> false
  #  f.close_read    #=> nil
  #  f.closed?       #=> true
  def closed?
    @descriptor == -1
  end

  def dup
    ensure_open
    super
  end

  # Argument matrix for IO#gets and IO#each:
  #
  #  separator / limit | nil | >= 0 | < 0
  # ===================+=====+======+=====
  #  String (nonempty) |  A  |  B   |  C
  #                    +-----+------+-----
  #  ""                |  D  |  E   |  F
  #                    +-----+------+-----
  #  nil               |  G  |  H   |  I
  #

  class EachReader
    def initialize(io, buffer, separator, limit)
      @io = io
      @buffer = buffer
      @separator = separator
      @limit = limit
      @skip = nil
    end

    def each(&block)
      if @separator
        if @separator.empty?
          @separator = "\n\n"
          @skip = 10
        end

        if @limit
          read_to_separator_with_limit(&block)
        else
          read_to_separator(&block)
        end
      else
        if @limit
          read_to_limit(&block)
        else
          read_all(&block)
        end
      end
    end

    # method A, D
    def read_to_separator
      str = ""

      until @buffer.exhausted?
        available = @buffer.fill_from @io, @skip
        break unless available > 0

        if count = @buffer.find(@separator)
          str << @buffer.shift(count)

          str = IO.read_encode(@io, str)
          str.taint

          $. = @io.increment_lineno
          @buffer.discard @skip if @skip

          yield str

          str = ""
        else
          str << @buffer.shift
        end
      end

      str << @buffer.shift
      unless str.empty?
        str = IO.read_encode(@io, str)
        str.taint
        $. = @io.increment_lineno
        yield str
      end
    end

    # method B, E
    def read_to_separator_with_limit
      str = ""

      #TODO: implement ignoring encoding with negative limit
      wanted = limit = @limit.abs

      until @buffer.exhausted?
        available = @buffer.fill_from @io, @skip
        break unless available > 0

        if count = @buffer.find(@separator)
          bytes = count < wanted ? count : wanted
          str << @buffer.shift(bytes)

          str = IO.read_encode(@io, str)
          str.taint

          $. = @io.increment_lineno
          @buffer.discard @skip if @skip

          yield str

          str = ""
          wanted = limit
        else
          if wanted < available
            str << @buffer.shift(wanted)

            str = @buffer.read_to_char_boundary(@io, str)
            str.taint

            $. = @io.increment_lineno
            @buffer.discard @skip if @skip

            yield str

            str = ""
            wanted = limit
          else
            str << @buffer.shift
            wanted -= available
          end
        end
      end

      unless str.empty?
        str = IO.read_encode(@io, str)
        str.taint
        $. = @io.increment_lineno
        yield str
      end
    end

    # Method G
    def read_all
      str = ""
      until @buffer.exhausted?
        @buffer.fill_from @io
        str << @buffer.shift
      end

      unless str.empty?
        str = IO.read_encode(@io, str)
        str.taint
        $. = @io.increment_lineno
        yield str
      end
    end

    # Method H
    def read_to_limit
      str = ""
      wanted = limit = @limit.abs

      until @buffer.exhausted?
        available = @buffer.fill_from @io
        if wanted < available
          str << @buffer.shift(wanted)

          str = @buffer.read_to_char_boundary(@io, str)
          str.taint

          $. = @io.increment_lineno
          yield str

          str = ""
          wanted = limit
        else
          str << @buffer.shift
          wanted -= available
        end
      end

      unless str.empty?
        str = IO.read_encode(@io, str)
        str.taint
        $. = @io.increment_lineno
        yield str
      end
    end
  end

  def increment_lineno
    @lineno += 1
  end

  ##
  # Return a string describing this IO object.
  def inspect
    if @descriptor != -1
      "#<#{self.class}:fd #{@descriptor}>"
    else
      "#<#{self.class}:(closed)"
    end
  end

  def lines(*args, &block)
    if block_given?
      each_line(*args, &block)
    else
      to_enum :each_line, *args
    end
  end

  def each(sep_or_limit=$/, limit=nil, &block)
    return to_enum(:each, sep_or_limit, limit) unless block_given?

    ensure_open_and_readable

    if limit
      limit = Rubinius::Type.coerce_to limit, Integer, :to_int
      sep = sep_or_limit ? StringValue(sep_or_limit) : nil
    else
      case sep_or_limit
      when String
        sep = sep_or_limit
      when nil
        sep = nil
      else
        unless sep = Rubinius::Type.check_convert_type(sep_or_limit, String, :to_str)
          sep = $/
          limit = Rubinius::Type.coerce_to sep_or_limit, Integer, :to_int
        end
      end
    end

    return if @ibuffer.exhausted?

    EachReader.new(self, @ibuffer, sep, limit).each(&block)

    self
  end

  alias_method :each_line, :each

  def each_byte
    return to_enum(:each_byte) unless block_given?

    yield getbyte until eof?

    self
  end

  alias_method :bytes, :each_byte

  def each_char
    return to_enum :each_char unless block_given?
    ensure_open_and_readable

    while char = getc
      yield char
    end

    self
  end

  alias_method :chars, :each_char

  def each_codepoint
    return to_enum :each_codepoint unless block_given?
    ensure_open_and_readable

    while char = getc
      yield char.ord
    end

    self
  end

  alias_method :codepoints, :each_codepoint


  ##
  # Set the pipe so it is at the end of the file
  def eof!
    @eof = true
  end

  ##
  # Returns true if ios is at end of file that means
  # there are no more data to read. The stream must be
  # opened for reading or an IOError will be raised.
  #
  #  f = File.new("testfile")
  #  dummy = f.readlines
  #  f.eof   #=> true
  # If ios is a stream such as pipe or socket, IO#eof?
  # blocks until the other end sends some data or closes it.
  #
  #  r, w = IO.pipe
  #  Thread.new { sleep 1; w.close }
  #  r.eof?  #=> true after 1 second blocking
  #
  #  r, w = IO.pipe
  #  Thread.new { sleep 1; w.puts "a" }
  #  r.eof?  #=> false after 1 second blocking
  #
  #  r, w = IO.pipe
  #  r.eof?  # blocks forever
  #
  # Note that IO#eof? reads data to a input buffer.
  # So IO#sysread doesn't work with IO#eof?.
  def eof?
    ensure_open_and_readable
    @ibuffer.fill_from self unless @ibuffer.exhausted?
    @eof and @ibuffer.exhausted?
  end

  alias_method :eof, :eof?

  def ensure_open_and_readable
    ensure_open
    write_only = @mode & ACCMODE == WRONLY
    raise IOError, "not opened for reading" if write_only
  end

  def ensure_open_and_writable
    ensure_open
    read_only = @mode & ACCMODE == RDONLY
    raise IOError, "not opened for writing" if read_only
  end

  def external_encoding
    return @external if @external
    return Encoding.default_external if @mode == RDONLY
  end

  ##
  # Provides a mechanism for issuing low-level commands to
  # control or query file-oriented I/O streams. Arguments
  # and results are platform dependent. If arg is a number,
  # its value is passed directly. If it is a string, it is
  # interpreted as a binary sequence of bytes (Array#pack
  # might be a useful way to build this string). On Unix
  # platforms, see fcntl(2) for details. Not implemented on all platforms.
  def fcntl(command, arg=0)
    ensure_open

    if !arg
      arg = 0
    elsif arg == true
      arg = 1
    elsif arg.kind_of? String
      raise NotImplementedError, "cannot handle String"
    else
      arg = Rubinius::Type.coerce_to arg, Fixnum, :to_int
    end

    command = Rubinius::Type.coerce_to command, Fixnum, :to_int
    FFI::Platform::POSIX.fcntl descriptor, command, arg
  end

  def internal_encoding
    @internal
  end

  ##
  # Provides a mechanism for issuing low-level commands to
  # control or query file-oriented I/O streams. Arguments
  # and results are platform dependent. If arg is a number,
  # its value is passed directly. If it is a string, it is
  # interpreted as a binary sequence of bytes (Array#pack
  # might be a useful way to build this string). On Unix
  # platforms, see fcntl(2) for details. Not implemented on all platforms.
  def ioctl(command, arg=0)
    ensure_open

    if !arg
      real_arg = 0
    elsif arg == true
      real_arg = 1
    elsif arg.kind_of? String
      # This could be faster.
      buffer_size = arg.bytesize
      # On BSD and Linux, we could read the buffer size out of the ioctl value.
      # Most Linux ioctl codes predate the convention, so a fallback like this
      # is still necessary.
      buffer_size = 4096 if buffer_size < 4096
      buffer = FFI::MemoryPointer.new buffer_size
      buffer.write_string arg, arg.bytesize
      real_arg = buffer.address
    else
      real_arg = Rubinius::Type.coerce_to arg, Fixnum, :to_int
    end

    command = Rubinius::Type.coerce_to command, Fixnum, :to_int
    ret = FFI::Platform::POSIX.ioctl descriptor, command, real_arg
    Errno.handle if ret < 0
    if arg.kind_of?(String)
      arg.replace buffer.read_string(buffer_size)
      buffer.free
    end
    ret
  end

  ##
  # Returns an integer representing the numeric file descriptor for ios.
  #
  #  $stdin.fileno    #=> 0
  #  $stdout.fileno   #=> 1
  def fileno
    ensure_open
    @descriptor
  end

  alias_method :to_i, :fileno

  ##
  # Flushes any buffered data within ios to the underlying
  # operating system (note that this is Ruby internal
  # buffering only; the OS may buffer the data as well).
  #
  #  $stdout.print "no newline"
  #  $stdout.flush
  # produces:
  #
  #  no newline
  def flush
    ensure_open
    @ibuffer.empty_to self
    self
  end

  ##
  # Immediately writes all buffered data in ios to disk. Returns
  # nil if the underlying operating system does not support fsync(2).
  # Note that fsync differs from using IO#sync=. The latter ensures
  # that data is flushed from Ruby's buffers, but does not guarantee
  # that the underlying operating system actually writes it to disk.
  def fsync
    flush

    err = FFI::Platform::POSIX.fsync @descriptor

    Errno.handle 'fsync(2)' if err < 0

    err
  end

  def getbyte
    ensure_open

    return @ibuffer.getbyte(self)
  end

  ##
  # Gets the next 8-bit byte (0..255) from ios.
  # Returns nil if called at end of file.
  #
  #  f = File.new("testfile")
  #  f.getc   #=> 84
  #  f.getc   #=> 104
  def getc
    ensure_open

    return @ibuffer.getchar(self)
  end

  def gets(sep_or_limit=$/, limit=nil)
    each sep_or_limit, limit do |line|
      $_ = line if line
      return line
    end

    nil
  end

  ##
  # Returns the current line number in ios. The
  # stream must be opened for reading. lineno
  # counts the number of times gets is called,
  # rather than the number of newlines encountered.
  # The two values will differ if gets is called with
  # a separator other than newline. See also the $. variable.
  #
  #  f = File.new("testfile")
  #  f.lineno   #=> 0
  #  f.gets     #=> "This is line one\n"
  #  f.lineno   #=> 1
  #  f.gets     #=> "This is line two\n"
  #  f.lineno   #=> 2
  def lineno
    ensure_open

    @lineno
  end

  ##
  # Manually sets the current line number to the
  # given value. $. is updated only on the next read.
  #
  #  f = File.new("testfile")
  #  f.gets                     #=> "This is line one\n"
  #  $.                         #=> 1
  #  f.lineno = 1000
  #  f.lineno                   #=> 1000
  #  $. # lineno of last read   #=> 1
  #  f.gets                     #=> "This is line two\n"
  #  $. # lineno of last read   #=> 1001
  def lineno=(line_number)
    ensure_open

    raise TypeError if line_number.nil?

    @lineno = Integer(line_number)
  end

  ##
  # FIXME
  # Returns the process ID of a child process
  # associated with ios. This will be set by IO::popen.
  #
  #  pipe = IO.popen("-")
  #  if pipe
  #    $stderr.puts "In parent, child pid is #{pipe.pid}"
  #  else
  #    $stderr.puts "In child, pid is #{$$}"
  #  end
  # produces:
  #
  #  In child, pid is 26209
  #  In parent, child pid is 26209
  def pid
    raise IOError, 'closed stream' if closed?
    @pid
  end

  attr_writer :pid

  def pipe=(v)
    @pipe = !!v
  end

  def pipe?
    @pipe
  end

  ##
  #
  def pos
    flush
    @ibuffer.unseek! self

    prim_seek 0, SEEK_CUR
  end

  alias_method :tell, :pos

  ##
  # Seeks to the given position (in bytes) in ios.
  #
  #  f = File.new("testfile")
  #  f.pos = 17
  #  f.gets   #=> "This is line two\n"
  def pos=(offset)
    seek offset, SEEK_SET
  end

  ##
  # Writes each given argument.to_s to the stream or $_ (the result of last
  # IO#gets) if called without arguments. Appends $\.to_s to output. Returns
  # nil.
  def print(*args)
    if args.empty?
      write $_.to_s
    else
      args.each { |o| write o.to_s }
    end

    write $\.to_s
    nil
  end

  ##
  # Formats and writes to ios, converting parameters under
  # control of the format string. See Kernel#sprintf for details.
  def printf(fmt, *args)
    fmt = StringValue(fmt)
    write ::Rubinius::Sprinter.get(fmt).call(*args)
  end

  ##
  # If obj is Numeric, write the character whose code is obj,
  # otherwise write the first character of the string
  # representation of obj to ios.
  #
  #  $stdout.putc "A"
  #  $stdout.putc 65
  # produces:
  #
  #  AA
  def putc(obj)
    if Rubinius::Type.object_kind_of? obj, String
      write obj.substring(0, 1)
    else
      byte = Rubinius::Type.coerce_to(obj, Integer, :to_int) & 0xff
      write byte.chr
    end

    return obj
  end

  ##
  # Writes the given objects to ios as with IO#print.
  # Writes a record separator (typically a newline)
  # after any that do not already end with a newline
  # sequence. If called with an array argument, writes
  # each element on a new line. If called without arguments,
  # outputs a single record separator.
  #
  #  $stdout.puts("this", "is", "a", "test")
  # produces:
  #
  #  this
  #  is
  #  a
  #  test
  def puts(*args)
    if args.empty?
      write DEFAULT_RECORD_SEPARATOR
    else
      args.each do |arg|
        if arg.equal? nil
          str = ""
        elsif Thread.guarding? arg
          str = "[...]"
        elsif arg.kind_of?(Array)
          Thread.recursion_guard arg do
            arg.each do |a|
              puts a
            end
          end
        else
          str = arg.to_s
        end

        if str
          write str
          write DEFAULT_RECORD_SEPARATOR unless str.suffix?(DEFAULT_RECORD_SEPARATOR)
        end
      end
    end

    nil
  end

  def read(length=nil, buffer=nil)
    ensure_open_and_readable
    buffer = StringValue(buffer) if buffer

    unless length
      str = IO.read_encode self, read_all
      return str unless buffer

      return buffer.replace(str)
    end

    if @ibuffer.exhausted?
      buffer.clear if buffer
      return nil
    end

    str = ""
    needed = length
    while needed > 0 and not @ibuffer.exhausted?
      available = @ibuffer.fill_from self

      count = available > needed ? needed : available
      str << @ibuffer.shift(count)
      str = nil if str.empty?

      needed -= count
    end

    if str
      if buffer
        buffer.replace str.force_encoding(buffer.encoding)
      else
        str.force_encoding Encoding::ASCII_8BIT
      end
    else
      buffer.clear if buffer
      nil
    end
  end

  ##
  # Reads all input until +#eof?+ is true. Returns the input read.
  # If the buffer is already exhausted, returns +""+.
  def read_all
    str = ""
    until @ibuffer.exhausted?
      @ibuffer.fill_from self
      str << @ibuffer.shift
    end

    str
  end

  private :read_all

  # defined in bootstrap, used here.
  private :read_if_available

  ##
  # Reads at most maxlen bytes from ios using read(2) system
  # call after O_NONBLOCK is set for the underlying file descriptor.
  #
  # If the optional outbuf argument is present, it must reference
  # a String, which will receive the data.
  #
  # read_nonblock just calls read(2). It causes all errors read(2)
  # causes: EAGAIN, EINTR, etc. The caller should care such errors.
  #
  # read_nonblock causes EOFError on EOF.
  #
  # If the read buffer is not empty, read_nonblock reads from the
  # buffer like readpartial. In this case, read(2) is not called.
  def read_nonblock(size, buffer=nil)
    raise ArgumentError, "illegal read size" if size < 0
    ensure_open

    buffer = StringValue buffer if buffer

    if @ibuffer.size > 0
      return @ibuffer.shift(size)
    end

    if str = read_if_available(size)
      buffer.replace(str) if buffer
      return str
    else
      raise EOFError, "stream closed"
    end
  end

  ##
  # Reads a character as with IO#getc, but raises an EOFError on end of file.
  def readchar
    char = getc
    raise EOFError, 'end of file reached' unless char
    char
  end

  def readbyte
    byte = getbyte
    raise EOFError, "end of file reached" unless byte
    raise EOFError, "end of file" unless bytes
    byte
  end

  ##
  # Reads a line as with IO#gets, but raises an EOFError on end of file.
  def readline(sep=$/)
    out = gets(sep)
    raise EOFError, "end of file" unless out
    return out
  end

  ##
  # Reads all of the lines in ios, and returns them in an array.
  # Lines are separated by the optional sep_string. If sep_string
  # is nil, the rest of the stream is returned as a single record.
  # The stream must be opened for reading or an IOError will be raised.
  #
  #  f = File.new("testfile")
  #  f.readlines[0]   #=> "This is line one\n"
  def readlines(sep=$/)
    sep = StringValue sep if sep

    old_line = $_
    ary = Array.new
    while line = gets(sep)
      ary << line
    end
    $_ = old_line

    ary
  end

  ##
  # Reads at most maxlen bytes from the I/O stream. It blocks
  # only if ios has no data immediately available. It doesn‘t
  # block if some data available. If the optional outbuf argument
  # is present, it must reference a String, which will receive the
  # data. It raises EOFError on end of file.
  #
  # readpartial is designed for streams such as pipe, socket, tty,
  # etc. It blocks only when no data immediately available. This
  # means that it blocks only when following all conditions hold.
  #
  # the buffer in the IO object is empty.
  # the content of the stream is empty.
  # the stream is not reached to EOF.
  # When readpartial blocks, it waits data or EOF on the stream.
  # If some data is reached, readpartial returns with the data.
  # If EOF is reached, readpartial raises EOFError.
  #
  # When readpartial doesn‘t blocks, it returns or raises immediately.
  # If the buffer is not empty, it returns the data in the buffer.
  # Otherwise if the stream has some content, it returns the data in
  # the stream. Otherwise if the stream is reached to EOF, it raises EOFError.
  #
  #  r, w = IO.pipe           #               buffer          pipe content
  #  w << "abc"               #               ""              "abc".
  #  r.readpartial(4096)      #=> "abc"       ""              ""
  #  r.readpartial(4096)      # blocks because buffer and pipe is empty.
  #
  #  r, w = IO.pipe           #               buffer          pipe content
  #  w << "abc"               #               ""              "abc"
  #  w.close                  #               ""              "abc" EOF
  #  r.readpartial(4096)      #=> "abc"       ""              EOF
  #  r.readpartial(4096)      # raises EOFError
  #
  #  r, w = IO.pipe           #               buffer          pipe content
  #  w << "abc\ndef\n"        #               ""              "abc\ndef\n"
  #  r.gets                   #=> "abc\n"     "def\n"         ""
  #  w << "ghi\n"             #               "def\n"         "ghi\n"
  #  r.readpartial(4096)      #=> "def\n"     ""              "ghi\n"
  #  r.readpartial(4096)      #=> "ghi\n"     ""              ""
  # Note that readpartial behaves similar to sysread. The differences are:
  #
  # If the buffer is not empty, read from the buffer instead
  # of "sysread for buffered IO (IOError)".
  # It doesn‘t cause Errno::EAGAIN and Errno::EINTR. When readpartial
  # meets EAGAIN and EINTR by read system call, readpartial retry the system call.
  # The later means that readpartial is nonblocking-flag insensitive. It
  # blocks on the situation IO#sysread causes Errno::EAGAIN as if the fd is blocking mode.
  def readpartial(size, buffer=nil)
    raise ArgumentError, 'negative string size' unless size >= 0
    ensure_open

    if buffer
      buffer = StringValue(buffer)

      buffer.shorten! buffer.bytesize

      return buffer if size == 0

      if @ibuffer.size > 0
        data = @ibuffer.shift(size)
      else
        data = sysread(size)
      end

      buffer.replace(data)

      return buffer
    else
      return "" if size == 0

      if @ibuffer.size > 0
        return @ibuffer.shift(size)
      end

      return sysread(size)
    end
  end

  ##
  # Reassociates ios with the I/O stream given in other_IO or to
  # a new stream opened on path. This may dynamically change the
  # actual class of this stream.
  #
  #  f1 = File.new("testfile")
  #  f2 = File.new("testfile")
  #  f2.readlines[0]   #=> "This is line one\n"
  #  f2.reopen(f1)     #=> #<File:testfile>
  #  f2.readlines[0]   #=> "This is line one\n"
  def reopen(other, mode=undefined)
    if other.respond_to?(:to_io)
      flush

      if other.kind_of? IO
        io = other
      else
        io = other.to_io
        unless io.kind_of? IO
          raise TypeError, "#to_io must return an instance of IO"
        end
      end

      io.ensure_open
      io.reset_buffering

      reopen_io io
      Rubinius::Unsafe.set_class self, io.class
      if io.respond_to?(:path)
        @path = io.path
      end
    else
      flush unless closed?

      # If a mode isn't passed in, use the mode that the IO is already in.
      if undefined.equal? mode
        mode = @mode
        # If this IO was already opened for writing, we should
        # create the target file if it doesn't already exist.
        if (mode & RDWR == RDWR) || (mode & WRONLY == WRONLY)
          mode |= CREAT
        end
      else
        mode = IO.parse_mode(mode)
      end

      reopen_path Rubinius::Type.coerce_to_path(other), mode
      seek 0, SEEK_SET
    end

    self
  end

  ##
  # Internal method used to reset the state of the buffer, including the
  # physical position in the stream.
  def reset_buffering
    @ibuffer.unseek! self
  end

  ##
  # Positions ios to the beginning of input, resetting lineno to zero.
  #
  #  f = File.new("testfile")
  #  f.readline   #=> "This is line one\n"
  #  f.rewind     #=> 0
  #  f.lineno     #=> 0
  #  f.readline   #=> "This is line one\n"
  def rewind
    seek 0
    @lineno = 0
    return 0
  end

  ##
  # Seeks to a given offset +amount+ in the stream according to the value of whence:
  #
  # IO::SEEK_CUR  | Seeks to _amount_ plus current position
  # --------------+----------------------------------------------------
  # IO::SEEK_END  | Seeks to _amount_ plus end of stream (you probably
  #               | want a negative value for _amount_)
  # --------------+----------------------------------------------------
  # IO::SEEK_SET  | Seeks to the absolute location given by _amount_
  # Example:
  #
  #  f = File.new("testfile")
  #  f.seek(-13, IO::SEEK_END)   #=> 0
  #  f.readline                  #=> "And so on...\n"
  def seek(amount, whence=SEEK_SET)
    flush

    @ibuffer.unseek! self
    @eof = false

    prim_seek Integer(amount), whence

    return 0
  end

  def set_encoding(external, internal=nil, options=undefined)
    case external
    when Encoding
      @external = external
    when String
      @external = nil
    when nil
      if @mode == RDONLY || @external
        @external = nil
      else
        @external = Encoding.default_external
      end
    else
      @external = nil
      external = StringValue(external)
    end

    if @external.nil? and not external.nil?
      if index = external.index(":")
        internal = external[index+1..-1]
        external = external[0, index]
      end

      if external[3] == ?|
        if encoding = strip_bom
          external = encoding
        else
          external = external[4..-1]
        end
      end

      @external = Encoding.find external
    end

    unless undefined.equal? options
      # TODO: set the encoding options on the IO instance
      if options and not options.kind_of? Hash
        options = Rubinius::Type.coerce_to options, Hash, :to_hash
      end
    end

    case internal
    when Encoding
      @internal = nil if @external == internal
    when String
      # do nothing
    when nil
      internal = Encoding.default_internal
    else
      internal = StringValue(internal)
    end

    if internal.kind_of? String
      return self if internal == "-"
      internal = Encoding.find internal
    end

    @internal = internal unless internal && @external == internal

    self
  end

  def read_bom_byte
    read_ios, _, _ = IO.select [self], nil, nil, 0.1
    return getbyte if read_ios
  end

  def strip_bom
    return unless File::Stat.fstat(@descriptor).file?

    case b1 = getbyte
    when 0x00
      b2 = getbyte
      if b2 == 0x00
        b3 = getbyte
        if b3 == 0xFE
          b4 = getbyte
          if b4 == 0xFF
            return "UTF-32BE"
          end
          ungetbyte b4
        end
        ungetbyte b3
      end
      ungetbyte b2

    when 0xFF
      b2 = getbyte
      if b2 == 0xFE
        b3 = getbyte
        if b3 == 0x00
          b4 = getbyte
          if b4 == 0x00
            return "UTF-32LE"
          end
          ungetbyte b4
        else
          ungetbyte b3
          return "UTF-16LE"
        end
        ungetbyte b3
      end
      ungetbyte b2

    when 0xFE
      b2 = getbyte
      if b2 == 0xFF
        return "UTF-16BE"
      end
      ungetbyte b2

    when 0xEF
      b2 = getbyte
      if b2 == 0xBB
        b3 = getbyte
        if b3 == 0xBF
          return "UTF-8"
        end
        ungetbyte b3
      end
      ungetbyt b2
    end

    ungetbyte b1
    nil
  end

  ##
  # Returns status information for ios as an object of type File::Stat.
  #
  #  f = File.new("testfile")
  #  s = f.stat
  #  "%o" % s.mode   #=> "100644"
  #  s.blksize       #=> 4096
  #  s.atime         #=> Wed Apr 09 08:53:54 CDT 2003
  def stat
    ensure_open

    File::Stat.fstat @descriptor
  end

  ##
  # Returns the current "sync mode" of ios. When sync mode is true,
  # all output is immediately flushed to the underlying operating
  # system and is not buffered by Ruby internally. See also IO#fsync.
  #
  #  f = File.new("testfile")
  #  f.sync   #=> false
  def sync
    ensure_open
    @sync == true
  end

  ##
  # Sets the "sync mode" to true or false. When sync mode is true,
  # all output is immediately flushed to the underlying operating
  # system and is not buffered internally. Returns the new state.
  # See also IO#fsync.
  def sync=(v)
    ensure_open
    @sync = !!v
  end

  ##
  # Reads integer bytes from ios using a low-level read and returns
  # them as a string. Do not mix with other methods that read from
  # ios or you may get unpredictable results. Raises SystemCallError
  # on error and EOFError at end of file.
  #
  #  f = File.new("testfile")
  #  f.sysread(16)   #=> "This is line one"
  #
  #  @todo  Improve reading into provided buffer.
  #
  def sysread(number_of_bytes, buffer=undefined)
    flush
    raise IOError unless @ibuffer.empty?

    str = read_primitive number_of_bytes
    raise EOFError if str.nil?

    unless undefined.equal? buffer
      StringValue(buffer).replace str
    end

    str
  end

  ##
  # Seeks to a given offset in the stream according to the value
  # of whence (see IO#seek for values of whence). Returns the new offset into the file.
  #
  #  f = File.new("testfile")
  #  f.sysseek(-13, IO::SEEK_END)   #=> 53
  #  f.sysread(10)                  #=> "And so on."
  def sysseek(amount, whence=SEEK_SET)
    ensure_open
    if @ibuffer.write_synced?
      raise IOError unless @ibuffer.empty?
    else
      warn 'sysseek for buffered IO'
    end

    amount = Integer(amount)

    prim_seek amount, whence
  end

  def to_io
    self
  end

  ##
  # Returns true if ios is associated with a terminal device (tty), false otherwise.
  #
  #  File.new("testfile").isatty   #=> false
  #  File.new("/dev/tty").isatty   #=> true
  def tty?
    ensure_open
    FFI::Platform::POSIX.isatty(@descriptor) == 1
  end

  alias_method :isatty, :tty?

  def syswrite(data)
    data = String data
    return 0 if data.bytesize == 0

    ensure_open_and_writable
    @ibuffer.unseek!(self) unless @sync

    prim_write(data)
  end

  def ungetbyte(obj)
    ensure_open

    case obj
    when String
      str = obj
    when Integer
      @ibuffer.put_back(obj & 0xff)
      return
    when nil
      return
    else
      str = StringValue(obj)
    end

    str.bytes.reverse_each { |byte| @ibuffer.put_back byte }

    nil
  end

  def ungetc(obj)
    ensure_open

    case obj
    when String
      str = obj
    when Integer
      @ibuffer.put_back(obj)
      return
    when nil
      return
    else
      str = StringValue(obj)
    end

    str.bytes.reverse_each { |b| @ibuffer.put_back b }

    nil
  end

  def write(data)
    data = String data
    return 0 if data.bytesize == 0

    ensure_open_and_writable

    if !binmode? && external_encoding &&
       external_encoding != data.encoding &&
       external_encoding != Encoding::ASCII_8BIT
      unless data.ascii_only? && external_encoding.ascii_compatible?
        data.encode!(external_encoding)
      end
    end

    if @sync
      prim_write(data)
    else
      @ibuffer.unseek! self
      bytes_to_write = data.bytesize

      while bytes_to_write > 0
        bytes_to_write -= @ibuffer.unshift(data, data.bytesize - bytes_to_write)
        @ibuffer.empty_to self if @ibuffer.full? or sync
      end
    end

    data.bytesize
  end

  def write_nonblock(data)
    ensure_open_and_writable

    data = String data
    return 0 if data.bytesize == 0

    @ibuffer.unseek!(self) unless @sync

    raw_write(data)
  end

  def close
    begin
      flush
    ensure
      prim_close
    end

    if @pid and @pid != 0
      begin
        Process.wait @pid
      rescue Errno::ECHILD
        # If the child already exited
      end
      @pid = nil
    end

    return nil
  end

end

##
# Implements the pipe returned by IO::pipe.

class IO::BidirectionalPipe < IO

  def set_pipe_info(write)
    @write = write
    @sync = true
  end

  ##
  # Closes ios and flushes any pending writes to the
  # operating system. The stream is unavailable for
  # any further data operations; an IOError is raised
  # if such an attempt is made. I/O streams are
  # automatically closed when they are claimed by
  # the garbage collector.
  #
  # If ios is opened by IO.popen, close sets $?.
  def close
    @write.close unless @write.closed?

    super unless closed?

    nil
  end

  def closed?
    super and @write.closed?
  end

  def close_read
    raise IOError, 'closed stream' if closed?

    close
  end

  def close_write
    raise IOError, 'closed stream' if @write.closed?

    @write.close
  end

  # Expand these out rather than using some metaprogramming because it's a fixed
  # set and it's faster to have them as normal methods because then InlineCaches
  # work right.
  #
  def <<(obj)
    @write << obj
  end

  def print(*args)
    @write.print(*args)
  end

  def printf(fmt, *args)
    @write.printf(fmt, *args)
  end

  def putc(obj)
    @write.putc(obj)
  end

  def puts(*args)
    @write.puts(*args)
  end

  def syswrite(data)
    @write.syswrite(data)
  end

  def write(data)
    @write.write(data)
  end

  def write_nonblock(data)
    @write.write_nonblock(data)
  end

end
