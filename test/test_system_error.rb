require 'test/unit'
require 'rbconfig'

class TestSystemError < Test::Unit::TestCase
  def setup
    # FIXME: Might be fragile, depends on platform data.
    # sucked these right from ruby 1.8.6 on osx, adjusted on Linux.
    @r186_errcodes=[
      ["ENOTCONN", 57, "is not connected"], ["EXDEV", 18, /cross-device link/i],
      ["ENOLCK", 77, "No locks available"], ["ENOTSOCK", 38, "Socket operation on non-socket"],
      ["ENOLINK", 97, /link/i], ["ENETDOWN", 50, "Network is down"],
      ["EAGAIN", 35, "Resource temporarily unavailable"], ["EROFS", 30, "Read-only file system"],
      ["ENOMSG", 91, "No message of desired type"], ["EPROTONOSUPPORT", 43, "Protocol not supported"],
      ["EHOSTDOWN", 64, "Host is down"], ["EINTR", 4, "Interrupted system call"],
      ["ENFILE", 23, "Too many open files in system"], ["EBUSY", 16, /resource busy/i],
      ["EDEADLK", 11, "Resource deadlock avoided"], ["EILSEQ", 92, /Illegal|Invalid .*/],
      ["ENOBUFS", 55, "No buffer space available"], ["EBADF", 9, "Bad file descriptor"],
      ["ENOSPC", 28, "No space left on device"], ["ENOSR", 98, /STREAM.* resources/i],
      ["EADDRINUSE", 48, "Address already in use"],
      ["EDQUOT", 69, "quota exceeded"], ["ENOENT", 2, "No such file or directory"],
      ["EISDIR", 21, "Is a directory"], ["ELOOP", 62, "Too many levels of symbolic links"],
      ["EPROTOTYPE", 41, "Protocol wrong type for socket"], ["ETIMEDOUT", 60, "timed out"],
      ["ECONNABORTED", 53, "Software caused connection abort"], ["EFAULT", 14, "Bad address"],
      ["EDOM", 33, "Numerical argument out of domain"], ["EBADMSG", 94, "Bad message"],
      ["EPFNOSUPPORT", 46, "Protocol family not supported"], ["EINPROGRESS", 36, "Operation now in progress"],
      ["E2BIG", 7, "Argument list too long"], ["ETXTBSY", 26, "Text file busy"],
      ["ENODATA", 96, /No (data|message) available/], ["ENOSYS", 78, "Function not implemented"],
      ["EDESTADDRREQ", 39, "Destination address required"], ["ESHUTDOWN", 58, /Can.*t send after .* shutdown/],
      ["ENODEV", 19, /device/], ["EMLINK", 31, "Too many links"],
      ["EPROTO", 100, "Protocol error"], ["ENETUNREACH", 51, "Network is unreachable"],
      ["ENOMEM", 12, "Cannot allocate memory"], ["EIO", 5, "Input/output error"],
      ["EMFILE", 24, "Too many open files"], ["EIDRM", 90, "Identifier removed"],
      ["ESOCKTNOSUPPORT", 44, "Socket type not supported"], ["EHOSTUNREACH", 65, "No route to host"],
      ["EEXIST", 17, "File exists"], ["ENAMETOOLONG", 63, "File name too long"],
      ["EUSERS", 68, "Too many users"], ["EISCONN", 56, "is already connected"],
      ["ECHILD", 10, "No child processes"], ["ESPIPE", 29, "Illegal seek"],
      ["EREMOTE", 71, "remote"], ["EADDRNOTAVAIL", 49, "assign requested address"],
      ["ENOPROTOOPT", 42, "Protocol not available"],
      ["ECONNREFUSED", 61, "Connection refused"], ["ESRCH", 3, "No such process"],
      ["EINVAL", 22, "Invalid argument"], ["EOVERFLOW", 84, "Value too large"],
      ["ECONNRESET", 54, "Connection reset by peer"], ["ENOTBLK", 15, "Block device required"],
      ["ERANGE", 34, /result/i], ["ENOEXEC", 8, "Exec format error"],
      ["EAFNOSUPPORT", 47, "Address family not supported by protocol"],
      ["ETIME", 101, /time/i], ["EFBIG", 27, "File too large"],
      ["ESTALE", 70, "Stale NFS file handle"], ["EPERM", 1, "Operation not permitted"],
      ["EMSGSIZE", 40, "Message too long"], ["ENOTEMPTY", 66, "Directory not empty"],
      ["ENOTDIR", 20, "Not a directory"],
      ["ETOOMANYREFS", 59, /Too many references: can.*t splice/],
      ["EMULTIHOP", 95, /MULTIHOP/i], ["EPIPE", 32, "Broken pipe"],
      ["EACCES", 13, "Permission denied"], ["ENETRESET", 52, "Network dropped connection on reset"],
      ["EOPNOTSUPP", 102, "Operation not supported"], ["ENOSTR", 99, /Not a STREAM/i],
      ["ENOTTY", 25, "Inappropriate ioctl for device"],
      ["EALREADY", 37, "Operation already in progress"]].sort_by{|v| v[1] }

      if ($VERBOSE)
        print_report
      end
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
    end if Config::CONFIG['host_os'].downcase =~ /windows|mswin|darwin|linux/ 
  end

  def assert_raise_msg(error, message)
    begin 
      yield
    rescue Exception => e
      assert_kind_of error, e
      assert_match message, e.message
    end
  end
  
  def print_report
    @r186_errcodes.each do |e,c|
      if Errno.constants.include?(e)
        a = (eval "Errno::#{e}::Errno")
        if a != c
          puts "mismatch code val #{e} should be #{c}, was #{a} "
        end
      else
        puts "     int    #{e} = #{c};"
      end
    end if Config::CONFIG['host_os'].downcase =~ /windows|mswin|darwin|linux/ 
  end

end
