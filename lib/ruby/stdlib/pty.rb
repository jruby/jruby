require 'ffi'

module PTY
  class ChildExited < RuntimeError
    attr_reader :status

    def initialize(status)
      @status = status
    end

    def inspect
      "<#{self.class.name}: #{status}>"
    end
  end

  class << self
    def spawn(*args, &block)
      if args.size > 1
        self.fork_exec_pty(*args, &block)
      else
        self.fork_exec_pty("/bin/sh", "-c", *args, &block)
      end
    end
    alias :getpty :spawn

    def open(*args)
      raise NotImplementedError
    end

    def check(target_pid, exception = false)
      pid, status = Process.waitpid2(target_pid, Process::WNOHANG|Process::WUNTRACED)

      # I sometimes see #<Process::Status: pid 0 signal 36> here.
      if pid == target_pid && status
        if exception
          raise ChildExited.new(status)
        else
          status
        end
      end
    rescue SystemCallError
      nil
    end
  end

  private
  module LibUtil
    extend FFI::Library
    ffi_lib FFI::Library::LIBC
    # forkpty(3) is in libutil on linux and BSD, libc on MacOS
    if FFI::Platform.linux? || (FFI::Platform.bsd? && !FFI::Platform.mac?)
      ffi_lib 'libutil'
    end
    attach_function :forkpty, [ :buffer_out, :buffer_out, :buffer_in, :buffer_in ], :pid_t
  end
  module LibC
    extend FFI::Library
    ffi_lib FFI::Library::LIBC
    attach_function :close, [ :int ], :int
    attach_function :strerror, [ :int ], :string
    attach_function :execv, [ :string, :buffer_in ], :int
    attach_function :execvp, [ :string, :buffer_in ], :int
    attach_function :dup2, [ :int, :int ], :int
    attach_function :dup, [ :int ], :int
    attach_function :_exit, [ :int ], :void
  end
  Buffer = FFI::Buffer
  def self.build_args(args)
    cmd, argv0 = args.shift
    cmd_args = args.map do |arg|
      FFI::MemoryPointer.from_string(arg)
    end
    exec_args = FFI::MemoryPointer.new(:pointer, 1 + cmd_args.length + 1)
    exec_cmd = FFI::MemoryPointer.from_string(argv0 || cmd)
    exec_args[0].put_pointer(0, exec_cmd)
    cmd_args.each_with_index do |arg, i|
      exec_args[i + 1].put_pointer(0, arg)
    end
    [ cmd, exec_args ]
  end
  def self.fork_exec_pty(*args)
    mfdp = Buffer.alloc_out :int
    name = Buffer.alloc_out 1024
    exec_cmd, exec_args = build_args(args)
    pid = LibUtil.forkpty(mfdp, name, nil, nil)
    #
    # We want to do as little as possible in the child process, since we're running
    # without any GC thread now, so test for the child case first
    #
    if pid == 0
      LibC.execvp(exec_cmd, exec_args)
      LibC._exit(1)
    end
    raise "forkpty failed: #{LibC.strerror(FFI.errno)}" if pid < 0
    masterfd = mfdp.get_int(0)
    rfp = FFI::IO.for_fd(masterfd, "r")
    wfp = FFI::IO.for_fd(LibC.dup(masterfd), "w")
    if block_given?
      retval = yield rfp, wfp, pid
      begin; rfp.close; rescue Exception; end
      begin; wfp.close; rescue Exception; end
      retval
    else
      [ rfp, wfp, pid ]
    end
  end
end
