warn "io/console on JRuby shells out to stty for most operations"

# attempt to call stty; if failure, fall back on stubbed version
result = begin
  `stty -a`
rescue Exception
  nil
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