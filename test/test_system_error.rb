require 'test/unit'

class TestSystemError < Test::Unit::TestCase
  def setup
    #sucked these right from ruby 1.8.6 on osx
    @r186_errcodes=[["ENOTCONN", 57], ["EXDEV", 18], ["ENOLCK", 77], ["ENOTSOCK", 38], ["ENOLINK", 97], ["ENETDOWN", 50], ["EAGAIN", 35], ["EROFS", 30], ["ENOMSG", 91], ["EPROTONOSUPPORT", 43], ["EHOSTDOWN", 64], ["EINTR", 4], ["ENFILE", 23], ["EBUSY", 16], ["EDEADLK", 11], ["EILSEQ", 92], ["ENOBUFS", 55], ["EBADF", 9], ["ENOSPC", 28], ["ENOSR", 98], ["EADDRINUSE", 48], ["EDQUOT", 69], ["ENOENT", 2], ["EISDIR", 21], ["ELOOP", 62], ["EPROTOTYPE", 41], ["ETIMEDOUT", 60], ["ECONNABORTED", 53], ["EFAULT", 14], ["EDOM", 33], ["EBADMSG", 94], ["EPFNOSUPPORT", 46], ["EINPROGRESS", 36], ["E2BIG", 7], ["ETXTBSY", 26], ["ENODATA", 96], ["ENOSYS", 78], ["EDESTADDRREQ", 39], ["ESHUTDOWN", 58], ["ENODEV", 19], ["EMLINK", 31], ["EPROTO", 100], ["ENETUNREACH", 51], ["ENOMEM", 12], ["EIO", 5], ["EMFILE", 24], ["EIDRM", 90], ["ESOCKTNOSUPPORT", 44], ["EHOSTUNREACH", 65], ["EEXIST", 17], ["ENAMETOOLONG", 63], ["EUSERS", 68], ["EISCONN", 56], ["ECHILD", 10], ["ESPIPE", 29], ["EREMOTE", 71], ["EADDRNOTAVAIL", 49], ["ENOPROTOOPT", 42], ["ECONNREFUSED", 61], ["ESRCH", 3], ["EINVAL", 22], ["EAGAIN", 35], ["EOVERFLOW", 84], ["ECONNRESET", 54], ["ENOTBLK", 15], ["ERANGE", 34], ["ENOEXEC", 8], ["EAFNOSUPPORT", 47], ["ETIME", 101], ["EFBIG", 27], ["ESTALE", 70], ["EPERM", 1], ["EMSGSIZE", 40], ["ENOTEMPTY", 66], ["ENOTDIR", 20], ["ETOOMANYREFS", 59], ["EMULTIHOP", 95], ["EPIPE", 32], ["EACCES", 13], ["ENETRESET", 52], ["EOPNOTSUPP", 102], ["ENOSTR", 99], ["ENOTTY", 25], ["ENXIO", 6], ["EALREADY", 37]].sort{|x,y| x[1]<=>y[1]}
  end
  def test_has_186_ERRNO_constants
    @r186_errcodes.each do |e,c|
      assert Errno.constants.include?(e), "missing constant #{e}"
    end
    allcodes = Errno.constants.map{|x| eval "Errno::#{x}::Errno"}
    dupes = allcodes.delete_if{|k| allcodes.rindex(k) == allcodes.index(k)}.uniq
    #puts "dupes: #{dupes.join(',')}"
    assert_equal allcodes.size, allcodes.uniq.size
    assert_equal [], dupes


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
