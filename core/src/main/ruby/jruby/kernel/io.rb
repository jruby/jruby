class IO
  class EAGAINWaitReadable < Errno::EAGAIN
    include IO::WaitReadable
  end

  class EAGAINWaitWritable < Errno::EAGAIN
    include IO::WaitWritable
  end

  if Errno::EAGAIN == Errno::EWOULDBLOCK
    IO::EWOULDBLOCKWaitReadable = IO::EAGAINWaitReadable
    IO::EWOULDBLOCKWaitWritable = IO::EAGAINWaitWritable
  else
    class EWOULDBLOCKWaitReadable < Errno::EWOULDBLOCK
      include IO::WaitReadable
    end

    class EWOULDBLOCKWaitWritable < Errno::EWOULDBLOCK
      include IO::WaitWritable
    end
  end

  class EINPROGRESSWaitWritable < Errno::EINPROGRESS
    include IO::WaitWritable
  end

  # We provided this as an unofficial way to do "open4" on JRuby (since open4 gem depends on fork),
  # and unfortunately people started using it. So I think we're stuck with it now (at least until
  # we can fix the open4 gem to do what we do below).
  # FIXME: I don't think spawn works on Windows yet, but the old IO.popen4 did.
  # FIXME: Mostly copied from open3.rb impl of popen3.
  def self.popen4(*cmd, **opts)
    in_r, in_w = IO.pipe
    opts[:in] = in_r
    in_w.sync = true

    out_r, out_w = IO.pipe
    opts[:out] = out_w

    err_r, err_w = IO.pipe
    opts[:err] = err_w

    child_io = [in_r, out_w, err_w]
    parent_io = [in_w, out_r, err_r]

    pid = spawn(*cmd, opts)
    child_io.each {|io| io.close }
    result = [pid, *parent_io]
    if block_given?
      begin
        return yield(*result)
      ensure
        parent_io.each{|io| io.close unless io.closed?}
        Process.waitpid(pid)
      end
    end
    result
  end
  class Buffer
    include Comparable

    class LockedError < RuntimeError; end
    class AllocationError < RuntimeError; end
    class AccessError < RuntimeError; end
    class InvalidatedError < RuntimeError; end
    class MaskError < RuntimeError; end

    EXTERNAL = 1
    INTERNAL = 2
    MAPPED = 3
    SHARED = 4
    LOCKED = 5
    PRIVATE = 6
    READONLY = 7
    LITTLE_ENDIAN = 8
    BIG_ENDIAN = 9
    HOST_ENDIAN = 10
    NETWORK_ENDIAN = 11

    PAGE_SIZE = 0
    DEFAULT_SIZE = 0

    java_import java.nio.ByteBuffer

    def self.for(type)
    end

    def self.map
    end

    def self.size_of
    end

    def initialize(size = DEFAULT_SIZE, flags = io_flags_for_size(size))
      warn "Buffer is an experimental feature"
      base = nil
      if size.kind_of? ByteBuffer
        base = size
        size = base.capacity
      end
      buffer_initialize(base, size, flags, nil)
    end

    private def buffer_initialize(base, size, flags, source)
      if base
      elsif size
        if flags & INTERNAL != 0
          base = ByteBuffer.allocate(size)
        elsif flags & MAPPED != 0
          base = io_buffer_map_memory(size, flags)
        end
        # raise BufferAllocationError, "Could not allocate buffer!"
      else
        return
      end
      @base = base
      @size = size
      @flags = flags
      @source = source
    end

    private def io_buffer_map_memory(size, flags)
      # TODO: flags
      map_mode = FileChannel::MapMode::READ_WRITE;
      if flags & SHARED != 0
        # ???
      else
        map_mode = FileChannel::MapMode::PRIVATE
      end
      begin
        FileChannel.map(map_mode, 0, size)
      rescue => e
        raise SystemError, "failed to memory map buffer"
      end
    end

    private def io_flags_for_size(size)
      return size > PAGE_SIZE ? MAPPED : INTERNAL
    end

    def initialize_copy(copy)
    end

    def inspect
    end

    def hexdump
    end

    def to_s
    end

    def size
      @size
    end

    def valid?
      true
    end

    def transfer
    end

    def null?
      @base == nil
    end

    def empty?
      @size == 0
    end

    def external?
      @base.direct?
    end

    def internal?
      !external?
    end

    def mapped?
      @base.kind_of MappedByteBuffer
    end

    def shared?
    end

    def locked?
    end

    def readonly?
      @base.read_only?
    end

    def locked
    end

    def slice(offset = 0, length = @size - offset)
      if offset < 0
        raise ArgumentError, "Offset can't be negative!"
      end
      if (length < 0)
        raise ArgumentError, "Length can't be negative!"
      end
      # TODO: honor offset and length
      Buffer.new(@base.slice)
    end

    def <=>(other)
    end

    def resize
    end

    def clear
    end

    def free
    end

    def get_value
    end

    def get_values
    end

    def each
    end

    def values
    end

    def each_byte
    end

    def set_value
    end

    def set_values
    end

    def copy
    end

    def get_string(str, offset = 0, length = nil, encoding = nil)
      @base.position(offset)
      if length
        buffer = Java::byte[length].new
      else
        buffer = Java::byte[@base.size].new
      end
      @base.get(buffer)
      str = String.from_java_bytes(buffer)
      str.force_encoding(encoding) if encoding
      str
    end

    def set_string(str, offset = 0, length = nil, source_offset = 0)
      @base.position(offset)
      @base.put(str.to_java_bytes, source_offset, length ? length : str.length)
    end

    def &
    end

    def |
    end

    def ^
    end

    def ~
    end

    def and!
    end

    def or!
    end

    def xor!
    end

    def not!
    end

    def read(io, length)
      @base.position(0)
      @base.limit(length)
      i = io.to_channel.read(@base)
    end

    def pread
    end

    def write(io, length)
      @base.position(0)
      @base.limit(length)
      io.to_channel.write(@base)
    end

    def pwrite
    end
  end
end