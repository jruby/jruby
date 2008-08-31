require 'ffi'


module PTY
  private
  module LibC
    extend JRuby::FFI::Library
    attach_function :forkpty, [ :buffer_out, :buffer_out, :buffer_in, :buffer_in ], :pid_t
    attach_function :openpty, [ :buffer_out, :buffer_out, :buffer_out, :buffer_in, :buffer_in ], :int
    attach_function :login_tty, [ :int ], :int
    attach_function :close, [ :int ], :int
    attach_function :strerror, [ :int ], :string
    attach_function :fork, [], :pid_t
    attach_function :execv, [ :string, :buffer_in ], :int
    attach_function :execvp, [ :string, :buffer_in ], :int
    attach_function :dup2, [ :int, :int ], :int
  end
  Buffer = JRuby::FFI::Buffer
  def self.build_args(args)
    cmd = args.shift
    cmd_args = args.map do |arg|
      MemoryPointer.from_string(arg)
    end
    exec_args = MemoryPointer.new(:pointer, 1 + cmd_args.length + 1)
    exec_cmd = MemoryPointer.from_string(cmd)
    exec_args[0].put_pointer(0, exec_cmd)
    cmd_args.each_with_index do |arg, i|
      exec_args[i + 1].put_pointer(0, arg)
    end
    [ cmd, exec_args ]
  end
  public
  def self.getpty(*args, &block)
    mfdp = Buffer.new :int
    name = Buffer.new 1024
    #
    # All the execv setup is done in the parent, since doing anything other than
    # execv in the child after fork is really flakey
    #
    exec_cmd, exec_args = build_args(args)
    pid = LibC.forkpty(mfdp, name, nil, nil)
    raise "forkpty failed: #{LibC.strerror(FFI.errno)}" if pid < 0    
    if pid == 0
      LibC.execvp(exec_cmd, exec_args)
      exit 1
    end
    masterfd = mfdp.get_int(0)
    if block_given?
      yield masterfd, masterfd, pid
      LibC.close(masterfd)
    else
      [ masterfd, masterfd, pid ]
    end
  end
  def self.getpty2(*args, &block)
    mfdp = Buffer.alloc_out :int
    sfdp = Buffer.alloc_out :int
    name = Buffer.alloc_out 1024
    #
    # All the execv setup is done in the parent, since doing anything other than
    # execv in the child after fork is really flakey
    #
    exec_cmd, exec_args = build_args(args)
    retval = LibC.openpty(mfdp, sfdp, name, nil, nil)
    raise "openpty failed: #{LibC.strerror(FFI.errno)}" unless retval == 0
    pid = LibC.fork()
    if pid < 0
      error = FFI.errno
      LibC.close(mfdp.get_int(0))
      LibC.close(sfdp.get_int(0))
      raise "fork failed: #{LibC.strerror(error)}"
    end
    if pid == 0
      LibC.close(mfdp.get_int(0))
      # Make the slave fd the new stdin/out/err
      fd = sfdp.get_int(0)
      LibC.dup2(fd, 0)
      LibC.dup2(fd, 1)
      LibC.dup2(fd, 2)
      LibC.close(fd)
      LibC.login_tty(0)
      LibC.execvp(exec_cmd, exec_args)
      exit 1
    end
    slavefd = sfdp.get_int(0)
    LibC.close(slavefd) # not needed in the parent
    masterfd = mfdp.get_int(0)
    if block_given?
      yield masterfd, masterfd, pid
      LibC.close(masterfd)
    else
      [ masterfd, masterfd, pid ]
    end
  end
  
  
end
module LibC
  extend JRuby::FFI::Library
  attach_function :close, [ :int ], :int
  attach_function :write, [ :int, :buffer_in, :size_t ], :ssize_t
  attach_function :read, [ :int, :buffer_out, :size_t ], :ssize_t
end
PTY.getpty("/bin/ls", "-alR", "/") { |rfd, wfd, pid|
  puts "child pid=#{pid}"
  while LibC.read(rfd, buf = 0.chr * 256, 256) > 0
    puts "Received from child='#{buf.strip}'"
  end
}