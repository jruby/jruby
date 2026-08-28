# flock support for Solaris

begin
  require 'ffi'

  module JRuby::Fcntl
    F_RDLCK = 1
    F_SETLK = 6
    F_SETLKW = 7
    F_UNLCK = 3
    F_WRLCK = 2

    extend FFI::Library
    ffi_lib 'c'
    attach_function :fcntl, [:short, :short, :varargs], :int

    class Flock < FFI::Struct
      self.size = 44
      layout :l_type, :short, 0,
             :l_whence, :short, 2,
             :l_start, :off_t, 4,
             :l_len, :off_t, 12,
             :l_sysid, :int, 20,
             :l_pid, :int, 24,
             :l_pad, :int, 28
    end
  end

  class File
    # Adapted from MRI's missing/flock.c
    def flock(operation)
      type = case (operation & ~LOCK_NB)
             when LOCK_SH
               JRuby::Fcntl::F_RDLCK
             when LOCK_EX
               JRuby::Fcntl::F_WRLCK
             when LOCK_UN
               JRuby::Fcntl::F_UNLCK
             else
               raise Errno::EINVAL
             end

      flock = JRuby::Fcntl::Flock.new
      flock[:l_type] = type
      flock[:l_whence] = File::SEEK_SET
      flock[:l_start] = flock[:l_len] = 0

      while JRuby::Fcntl.fcntl(fileno, (operation & LOCK_NB) != 0 ? JRuby::Fcntl::F_SETLK : JRuby::Fcntl::F_SETLKW, :pointer, flock) != 0
        errno = FFI.errno
        case errno
        when Errno::EAGAIN::Errno, Errno::EWOULDBLOCK::Errno, Errno::EACCES::Errno
          return false if operation & LOCK_NB != 0

          sleep 0.1
          next
        when Errno::EINTR::Errno
          # try again
          next
        else
          raise SystemCallError.new('fcntl', FFI.errno)
        end
      end

      return 0
    end
  end

rescue LoadError

  # Could not load FFI or fcntl not available, define File#flock to raise NotImplementedError
  class ::File
    def flock(*)
      raise NotImplementedError.new("fcntl-based flock not available on this platform")
    end
  end

end
