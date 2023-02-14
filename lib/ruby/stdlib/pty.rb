if RbConfig::CONFIG['host_os'] !~ /mswin|win32|mingw/
  require 'subspawn/replace-pty' # use SubSpawn's PTY & Process implementation as our own
  return
end

require 'ffi'
require 'fcntl'

module PTY
  module LibUtil
    extend FFI::Library
    ffi_lib FFI::Library::LIBC
    # openpty(3) is in libutil on linux and BSD, libc on MacOS
    if FFI::Platform.linux? || (FFI::Platform.bsd? && !FFI::Platform.mac?)
      ffi_lib ['libutil.so.1', 'util']
    end
    attach_function :openpty, [ :buffer_out, :buffer_out, :buffer_in, :buffer_in, :buffer_in ], :int
  end

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
      SubSpawn.pty_spawn(*args, &block)
    end
    alias :getpty :spawn

    def open
      masterfd, slavefd, slave_name = openpty

      master = IO.for_fd(masterfd, IO::RDWR)
      slave = File.for_fd(slavefd, IO::RDWR)

      File.chmod(0600, slave_name)

      slave.define_singleton_method(:path) do
        slave_name
      end

      slave.define_singleton_method(:tty?) do
        true
      end

      slave.define_singleton_method(:inspect) do
        "#<File:#{slave_name}>"
      end

      master.define_singleton_method(:inspect) do
        "#<IO:masterpty:#{slave_name}>"
      end

      fds = [master, slave]
      fds.each do |fd|
        fd.sync = true

        fd.close_on_exec = true
      end

      return fds unless block_given?

      begin
        retval = yield(fds.dup)
      ensure
        fds.reject(&:closed?).each(&:close)
      end

      retval
    end

    def check(target_pid, exception = false)
      pid, status = Process.waitpid2(target_pid, Process::WNOHANG|Process::WUNTRACED)

      # I sometimes see #<Process::Status: pid 0 signal 36> here.
      # See Github issue #3117
      if pid == target_pid && status
        if exception
          raise ChildExited.new(status)
        else
          return status
        end
      end
    rescue SystemCallError
      nil
    end

    private

    def openpty
      master = FFI::Buffer.alloc_out :int
      slave  = FFI::Buffer.alloc_out :int
      name   = FFI::Buffer.alloc_out 1024

      result = LibUtil.openpty(master, slave, name, nil, nil)
      if result != 0
        raise(exception_for_errno(FFI.errno) || SystemCallError.new("errno=#{FFI.errno}"))
      end

      [master.get_int(0), slave.get_int(0), name.get_string(0)]
    end

    def get_shell
      if shell = ENV['SHELL']
        return shell
      elsif pwent = Etc.getpwuid(Process.uid) && pwent.shell
        return pwent.shell
      else
        "/bin/sh"
      end
    end

    def exception_for_errno(errno)
      Errno.constants.each do |name|
        err = Errno.const_get(name)
        if err.constants.include?(:Errno) && err.const_get(:Errno) == errno
          return err
        end
      end
      SystemCallError.new("errno=#{errno}")
    end
  end
end
