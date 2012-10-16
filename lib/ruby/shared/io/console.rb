# This implementation of io/console is a little hacky. It shells out to `stty`
# for most operations, which does not work on Windows, in secured environments,
# and so on. In addition, because on Java 6 we can't actually launch
# subprocesses with tty control, stty will not actually manipulate the
# controlling terminal.
#
# For platforms where shelling to stty does not work, most operations will
# just be pass-throughs. This allows them to function, but does not actually
# change any tty flags.
#
# Finally, since we're using stty to shell out, we can only manipulate stdin/
# stdout tty rather than manipulating whatever terminal is actually associated
# with the IO we're calling against. This will produce surprising results if
# anyone is actually using io/console against non-stdio ttys...but that case
# seems like it would be pretty rare.
#
# Note: we are incorporating this into 1.7.0 since RubyGems uses io/console
# when pushing gems, in order to mask the password entry. Worst case is that
# we don't actually disable echo and the password is shown...we will try to
# do a better version of this in 1.7.1.

warn "io/console on JRuby shells out to stty for most operations"

# attempt to call stty; if failure, fall back on stubbed version
result = begin
  old_stderr = $stderr.dup
  $stderr.reopen('/dev/null')
  `stty -a`
rescue Exception
  nil
ensure
  $stderr.reopen(old_stderr)
end

if !result || $?.exitstatus != 0 || RbConfig::CONFIG['host_os'] =~ /(mswin)|(win32)|(ming)/
  # Windows version is always stubbed for now
  class IO
    def raw(*)
      yield self
    end

    def raw!(*)
    end

    def cooked(*)
      yield self
    end

    def cooked!(*)
    end

    def getch(*)
      getc
    end

    def echo=(echo)
    end

    def echo?
      true
    end

    def noecho
      yield self
    end

    def winsize
      [25, 80]
    end

    def winsize=(size)
    end

    def iflush
    end

    def oflush
    end

    def ioflush
    end
  end
else
  # Non-Windows assumes stty command is available
  class IO
    def raw(*)
      saved = `stty -g`
      `stty raw`
      yield self
    ensure
      `stty #{saved}`
    end

    def raw!(*)
      `stty raw`
    end

    def cooked(*)
      saved = `stty -g`
      `stty cooked`
      yield self
    ensure
      `stty #{saved}`
    end

    def cooked!(*)
      `stty -raw`
    end

    def getch(*)
      getc
    end

    def echo=(echo)
      `stty #{echo ? 'echo' : '-echo'}`
    end

    def echo?
      (`stty -a` =~ (/ -echo /)) ? false : true
    end

    def noecho
      saved = `stty -g`
      `stty -echo`
      yield self
    ensure
      `stty #{saved}`
    end

    def winsize
      match = `stty -a`.match(/(\d+) rows; (\d+) columns/)
      [match[1].to_i, match[2].to_i]
    end

    def winsize=(size)
      `stty rows #{size[0]} cols #{size[1]}`
    end

    def iflush
    end

    def oflush
    end

    def ioflush
    end
  end
end