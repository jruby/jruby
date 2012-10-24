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

# attempt to call stty; if failure, fall back on stubbed version

if RbConfig::CONFIG['host_os'].downcase =~ /darwin/
  result = begin
    require 'ffi'
    module DarwinConsole
      extend FFI::Library
      ffi_lib FFI::Library::LIBC
      typedef :ulong, :tcflag_t
      typedef :ulong, :speed_t


      # Special Control Characters
      VEOF     = 0 #  ICANON
      VEOL     = 1 #  ICANON
      VEOL2    = 2 #  ICANON together with IEXTEN
      VERASE   = 3 #  ICANON
      VWERASE  = 4 #  ICANON together with IEXTEN
      VKILL    = 5 #  ICANON
      VREPRINT = 6 #  ICANON together with IEXTEN
      VINTR    = 8 #  ISIG
      VQUIT    = 9 #  ISIG
      VSUSP    = 10 #  ISIG
      VDSUSP   = 11 #  ISIG together with IEXTEN
      VSTART   = 12 #  IXON, IXOFF
      VSTOP    = 13 #  IXON, IXOFF
      VLNEXT   = 14 #  IEXTEN
      VDISCARD = 15 #  IEXTEN
      VMIN     = 16 #  !ICANON
      VTIME    = 17 #  !ICANON
      VSTATUS  = 18 #  ICANON together with IEXTEN
      NCCS     = 20

      # Input flags - software input processing
      IGNBRK          = 0x00000001 #  ignore BREAK condition
      BRKINT          = 0x00000002 #  map BREAK to SIGINTR
      IGNPAR          = 0x00000004 #  ignore (discard) parity errors
      PARMRK          = 0x00000008 #  mark parity and framing errors
      INPCK           = 0x00000010 #  enable checking of parity errors
      ISTRIP          = 0x00000020 #  strip 8th bit off chars
      INLCR           = 0x00000040 #  map NL into CR
      IGNCR           = 0x00000080 #  ignore CR
      ICRNL           = 0x00000100 #  map CR to NL (ala CRMOD)
      IXON            = 0x00000200 #  enable output flow control
      IXOFF           = 0x00000400 #  enable input flow control
      IXANY           = 0x00000800 #  any char will restart after stop
      IMAXBEL         = 0x00002000 #  ring bell on input queue full
      IUTF8           = 0x00004000 #  maintain state for UTF-8 VERASE

      # Output flags - software output processing
      OPOST           = 0x00000001 #  enable following output processing
      ONLCR           = 0x00000002 #  map NL to CR-NL (ala CRMOD)
      OXTABS          = 0x00000004 #  expand tabs to spaces
      ONOEOT          = 0x00000008 #  discard EOT's (^D) on output)

      # Control flags - hardware control of terminal
      CIGNORE         = 0x00000001 #  ignore control flags
      CSIZE           = 0x00000300 #  character size mask
      CS5             = 0x00000000 #  5 bits (pseudo)
      CS6             = 0x00000100 #  6 bits
      CS7             = 0x00000200 #  7 bits
      CS8             = 0x00000300 #  8 bits
      CSTOPB          = 0x00000400 #  send 2 stop bits
      CREAD           = 0x00000800 #  enable receiver
      PARENB          = 0x00001000 #  parity enable
      PARODD          = 0x00002000 #  odd parity, else even
      HUPCL           = 0x00004000 #  hang up on last close
      CLOCAL          = 0x00008000 #  ignore modem status lines
      CCTS_OFLOW      = 0x00010000 #  CTS flow control of output
      CRTS_IFLOW      = 0x00020000 #  RTS flow control of input
      CDTR_IFLOW      = 0x00040000 #  DTR flow control of input
      CDSR_OFLOW      = 0x00080000 #  DSR flow control of output
      CCAR_OFLOW      = 0x00100000 #  DCD flow control of output
      CRTSCTS         = CCTS_OFLOW | CRTS_IFLOW
      MDMBUF          = 0x00100000 #  old name for CCAR_OFLOW


      # "Local" flags - dumping ground for other state
      ECHOKE          = 0x00000001 #  visual erase for line kill
      ECHOE           = 0x00000002 #  visually erase chars
      ECHOK           = 0x00000004 #  echo NL after line kill
      ECHO            = 0x00000008 #  enable echoing
      ECHONL          = 0x00000010 #  echo NL even if ECHO is off
      ECHOPRT         = 0x00000020 #  visual erase mode for hardcopy
      ECHOCTL         = 0x00000040 #  echo control chars as ^(Char)
      ISIG            = 0x00000080 #  enable signals INTR, QUIT, [D]SUSP
      ICANON          = 0x00000100 #  canonicalize input lines
      ALTWERASE       = 0x00000200 #  use alternate WERASE algorithm
      IEXTEN          = 0x00000400 #  enable DISCARD and LNEXT
      EXTPROC         = 0x00000800 #  external processing
      TOSTOP          = 0x00400000 #  stop background jobs from output
      FLUSHO          = 0x00800000 #  output being flushed (state)
      NOKERNINFO      = 0x02000000 #  no kernel output from VSTATUS
      PENDIN          = 0x20000000 #  XXX retype pending input (state)
      NOFLSH          = 0x80000000 #  don't flush after interrupt


      # Commands passed to tcsetattr() for setting the termios structure.
      TCSANOW         = 0 #  make change immediate
      TCSADRAIN       = 1 #  drain output, then change
      TCSAFLUSH       = 2 #  drain output, flush input
      TCSASOFT        = 0x10 #  flag - don't alter h.w. state


      TCIFLUSH        = 1
      TCOFLUSH        = 2
      TCIOFLUSH       = 3
      TCOOFF          = 1
      TCOON           = 2
      TCIOFF          = 3
      TCION           = 4

      class Termios < FFI::Struct
        layout \
          :c_iflag, :tcflag_t,
          :c_oflag, :tcflag_t,
          :c_cflag, :tcflag_t,
          :c_lflag, :tcflag_t,
          :cc_t, [ :uchar, NCCS ],
          :c_ispeed, :speed_t,
          :c_ospeed, :speed_t
      end


      attach_function :tcsetattr, [ :int, :int, Termios ], :int
      attach_function :tcgetattr, [ :int, Termios ], :int
      attach_function :cfgetispeed, [ Termios ], :speed_t
      attach_function :cfgetospeed, [ Termios ], :speed_t
      attach_function :cfsetispeed, [ Termios, :speed_t ], :int
      attach_function :cfsetospeed, [ Termios, :speed_t ], :int
      attach_function :cfmakeraw, [ Termios ], :int
      attach_function :tcflush, [ :int, :int ], :int
    end

    class IO
      include DarwinConsole

      def ttymode
        termios = Termios.new
        tcgetattr(fileno, termios)
        if block_given?
          yield tmp = termios.dup
          tcsetattr(fileno, TCSADRAIN, tmp)
        end
        termios
      end

      def ttymode_yield(block, &setup)
        begin
          orig_termios = ttymode &setup
          block.call(self)
        ensure
          tcsetattr(fileno, TCSADRAIN, orig_termios)
        end
      end

      def raw(*, &block)
        ttymode_yield(block) do |t|
          cfmakeraw(t)
          t[:c_lflag] &= ~(ECHOE|ECHOK)
        end
      end

      def raw!(*)
        ttymode do |t|
          cfmakeraw(t)
          t[:c_lflag] &= ~(ECHOE|ECHOK)
        end
      end

      def cooked(*, &block)
        ttymode_yield(block) do |t|
          t[:c_iflag] |= (BRKINT|ISTRIP|ICRNL|IXON)
          t[:c_oflag] |= OPOST
          t[:c_lflag] |= (ECHO|ECHOE|ECHOK|ECHONL|ICANON|ISIG|IEXTEN)
        end
      end

      def cooked!(*)
        ttymode do |t|
          t[:c_iflag] |= (BRKINT|ISTRIP|ICRNL|IXON)
          t[:c_oflag] |= OPOST
          t[:c_lflag] |= (ECHO|ECHOE|ECHOK|ECHONL|ICANON|ISIG|IEXTEN)
        end
      end

      def echo=(echo)
        ttymode do |t|
          if echo
            t[:c_lflag] |= (ECHO | ECHOE | ECHOK | ECHONL)
          else
            t[:c_lflag] &= ~(ECHO | ECHOE | ECHOK | ECHONL)
          end
        end
      end

      def echo?
        (ttymode[:c_lflag] & (ECHO | ECHONL)) != 0
      end

      def noecho(&block)
        ttymode_yield(block) { |t| t[:c_lflag] &= ~(ECHO | ECHOE | ECHOK | ECHONL) }
      end

      def getch(*)
        getc
      end

      # Not all systems return same format of stty -a output
      IEEE_STD_1003_2 = '(?<rows>\d+) rows; (?<columns>\d+) columns'
      UBUNTU = 'rows (?<rows>\d+); columns (?<columns>\d+)'

      def winsize
        match = `stty -a`.match(/#{IEEE_STD_1003_2}|#{UBUNTU}/)
        [match[:rows].to_i, match[:columns].to_i]
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
    true
  rescue Exception => ex
    $stderr.puts "failed to load darwin termios #{ex}"
    false
  end
else
  result = begin
    old_stderr = $stderr.dup
    $stderr.reopen('/dev/null')
    `stty -a`
    $?.exitstatus != 0
  rescue Exception
    nil
  ensure
    $stderr.reopen(old_stderr)
  end
end

if !result || RbConfig::CONFIG['host_os'] =~ /(mswin)|(win32)|(ming)/
  warn "io/console not supported; tty will not be manipulated"

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
elsif !IO.method_defined?:ttymode
  warn "io/console on JRuby shells out to stty for most operations"

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

    # Not all systems return same format of stty -a output
    IEEE_STD_1003_2 = '(?<rows>\d+) rows; (?<columns>\d+) columns'
    UBUNTU = 'rows (?<rows>\d+); columns (?<columns>\d+)'

    def winsize
      match = `stty -a`.match(/#{IEEE_STD_1003_2}|#{UBUNTU}/)
      [match[:rows].to_i, match[:columns].to_i]
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
