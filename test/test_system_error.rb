require 'test/unit'

class TestSystemError < Test::Unit::TestCase
  def setup
    #sucked these right from ruby 1.8.6 on osx
    @r186_errcodes=[["ENOTCONN", 57, "Socket is not connected"], ["EXDEV", 18, "Cross-device link"], ["ENOLCK", 77, "No locks available"], ["ENOTSOCK", 38, "Socket operation on non-socket"], ["ENOLINK", 97, "ENOLINK (Reserved)"], ["ENETDOWN", 50, "Network is down"], ["EAGAIN", 35, "Resource temporarily unavailable"], ["EWOULDBLOCK", 35, "Resource temporarily unavailable"], ["EROFS", 30, "Read-only file system"], ["ENOMSG", 91, "No message of desired type"], ["EPROTONOSUPPORT", 43, "Protocol not supported"], ["EHOSTDOWN", 64, "Host is down"], ["EINTR", 4, "Interrupted system call"], ["ENFILE", 23, "Too many open files in system"], ["EBUSY", 16, "Resource busy"], ["EDEADLK", 11, "Resource deadlock avoided"], ["EILSEQ", 92, "Illegal byte sequence"], ["ENOBUFS", 55, "No buffer space available"], ["EBADF", 9, "Bad file descriptor"], ["ENOSPC", 28, "No space left on device"], ["ENOSR", 98, "No STREAM resources"], ["EADDRINUSE", 48, "Address already in use"], ["EDQUOT", 69, "Disc quota exceeded"], ["ENOENT", 2, "No such file or directory"], ["EISDIR", 21, "Is a directory"], ["ELOOP", 62, "Too many levels of symbolic links"], ["EPROTOTYPE", 41, "Protocol wrong type for socket"], ["ETIMEDOUT", 60, "Operation timed out"], ["ECONNABORTED", 53, "Software caused connection abort"], ["EFAULT", 14, "Bad address"], ["EDOM", 33, "Numerical argument out of domain"], ["EBADMSG", 94, "Bad message"], ["EPFNOSUPPORT", 46, "Protocol family not supported"], ["EINPROGRESS", 36, "Operation now in progress"], ["E2BIG", 7, "Argument list too long"], ["ETXTBSY", 26, "Text file busy"], ["ENODATA", 96, "No message available on STREAM"], ["ENOSYS", 78, "Function not implemented"], ["EDESTADDRREQ", 39, "Destination address required"], ["ESHUTDOWN", 58, "Can't send after socket shutdown"], ["ENODEV", 19, "Operation not supported by device"], ["EMLINK", 31, "Too many links"], ["EPROTO", 100, "Protocol error"], ["ENETUNREACH", 51, "Network is unreachable"], ["ENOMEM", 12, "Cannot allocate memory"], ["EIO", 5, "Input/output error"], ["EMFILE", 24, "Too many open files"], ["EIDRM", 90, "Identifier removed"], ["ESOCKTNOSUPPORT", 44, "Socket type not supported"], ["EHOSTUNREACH", 65, "No route to host"], ["EEXIST", 17, "File exists"], ["ENAMETOOLONG", 63, "File name too long"], ["EUSERS", 68, "Too many users"], ["EISCONN", 56, "Socket is already connected"], ["ECHILD", 10, "No child processes"], ["ESPIPE", 29, "Illegal seek"], ["EREMOTE", 71, "Too many levels of remote in path"], ["EADDRNOTAVAIL", 49, "Can't assign requested address"], ["ENOPROTOOPT", 42, "Protocol not available"], ["ECONNREFUSED", 61, "Connection refused"], ["ESRCH", 3, "No such process"], ["EINVAL", 22, "Invalid argument"], ["EOVERFLOW", 84, "Value too large to be stored in data type"], ["ECONNRESET", 54, "Connection reset by peer"], ["ENOTBLK", 15, "Block device required"], ["ERANGE", 34, "Result too large"], ["ENOEXEC", 8, "Exec format error"], ["EAFNOSUPPORT", 47, "Address family not supported by protocol family"], ["ETIME", 101, "STREAM ioctl timeout"], ["EFBIG", 27, "File too large"], ["ESTALE", 70, "Stale NFS file handle"], ["EPERM", 1, "Operation not permitted"], ["EMSGSIZE", 40, "Message too long"], ["ENOTEMPTY", 66, "Directory not empty"], ["ENOTDIR", 20, "Not a directory"], ["ETOOMANYREFS", 59, "Too many references: can't splice"], ["EMULTIHOP", 95, "EMULTIHOP (Reserved)"], ["EPIPE", 32, "Broken pipe"], ["EACCES", 13, "Permission denied"], ["ENETRESET", 52, "Network dropped connection on reset"], ["EOPNOTSUPP", 102, "Operation not supported"], ["ENOSTR", 99, "Not a STREAM"], ["ENOTTY", 25, "Inappropriate ioctl for device"], ["ENXIO", 6, "Device not configured"], ["EALREADY", 37, "Operation already in progress"]].sort_by{|v| v[1] }
  end

#  def test_has_186_ERRNO_constants
#    @r186_errcodes.each do |e,c,m|
#      assert Errno.constants.include?(e), "missing constant #{e}"
#    end
#    allcodes = Errno.constants.map{|x| eval "Errno::#{x}::Errno"}
#    dupes = allcodes.delete_if{|k| allcodes.rindex(k) == allcodes.index(k)}.uniq
#    #puts "dupes: #{dupes.join(',')}"
#    assert_equal [35, 35], allcodes #EAGAIN and EWOULDBLOCK
#    assert_equal [35], dupes
#  end

  def test_can_raise_errno_without_message
    @r186_errcodes.each do |e,c,m|
      err = Errno.const_get(e)
      assert_raise_msg(err,m) do 
        raise err
      end
    end    
  end

  def assert_raise_msg(error, message)
    begin 
      yield
    rescue Exception => e
      assert_kind_of error, e
      assert_equal message, e.message
    end
  end
  
  def print_report
    @r186_errcodes.each do |e,c|
      if Errno.constants.include?(e)
#        a = (eval "Errno::#{e}::Errno")
#        if a != e
#          puts "mismatch code val #{e} should be #{c}, was #{a} "  
#        end
      else
        puts "     int    #{e} = #{c};"
      end
    end
  end

end
