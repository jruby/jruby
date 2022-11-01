#  Created by Ari Brown on 2008-02-23.
#  For rubinius. All pwnage reserved.
#  
#  Used in pwning teh nubs with FFI instead of C

# ** Syslog(Module)

# Included Modules: Syslog::Constants

# require 'syslog'

# A Simple wrapper for the UNIX syslog system calls that might be handy
# if you're writing a server in Ruby.  For the details of the syslog(8)
# architecture and constants, see the syslog(3) manual page of your
# platform.
require 'ffi'
JRuby::Util.load_ext("org.jruby.ext.syslog.SyslogLibrary")
if FFI::Platform::IS_WINDOWS
  raise LoadError, "Syslog not supported on this platform"
end

module Syslog
  include Constants

  module Level
    if defined? LOG_EMERG
      LOG_EMERG = Syslog::Constants::LOG_EMERG

      def emerg(*args)
        Syslog.log(LOG_EMERG, *args)
      end
    end
    if defined? LOG_ALERT
      LOG_ALERT = Syslog::Constants::LOG_ALERT

      def alert(*args)
        Syslog.log(LOG_ALERT, *args)
      end
    end
    if defined? LOG_CRIT
      LOG_CRIT = Syslog::Constants::LOG_CRIT

      def crit(*args)
        Syslog.log(LOG_CRIT, *args)
      end
    end
    if defined? LOG_ERR
      LOG_ERR = Syslog::Constants::LOG_ERR

      def err(*args)
        Syslog.log(LOG_ERR, *args)
      end
    end
    if defined? LOG_WARNING
      LOG_WARNING = Syslog::Constants::LOG_WARNING

      def warning(*args)
        Syslog.log(LOG_WARNING, *args)
      end
    end
    if defined? LOG_NOTICE
      LOG_NOTICE = Syslog::Constants::LOG_NOTICE

      def notice(*args)
        Syslog.log(LOG_NOTICE, *args)
      end
    end
    if defined? LOG_INFO
      LOG_INFO = Syslog::Constants::LOG_INFO

      def info(*args)
        Syslog.log(LOG_INFO, *args)
      end
    end
    if defined? LOG_DEBUG
      LOG_DEBUG = Syslog::Constants::LOG_DEBUG

      def debug(*args)
        syslog_write(LOG_DEBUG, *args)
      end
    end
  end

  module Facility
    if defined? LOG_AUTH
      LOG_AUTH = Syslog::Constants::LOG_AUTH
    end
    if defined? LOG_AUTHPRIV
      LOG_AUTHPRIV = Syslog::Constants::LOG_AUTHPRIV
    end
    if defined? LOG_CONSOLE
      LOG_CONSOLE = Syslog::Constants::LOG_CONSOLE
    end
    if defined? LOG_CRON
      LOG_CRON = Syslog::Constants::LOG_CRON
    end
    if defined? LOG_DAEMON
      LOG_DAEMON = Syslog::Constants::LOG_DAEMON
    end
    if defined? LOG_FTP
      LOG_FTP = Syslog::Constants::LOG_FTP
    end
    if defined? LOG_KERN
      LOG_KERN = Syslog::Constants::LOG_KERN
    end
    if defined? LOG_LPR
      LOG_LPR = Syslog::Constants::LOG_LPR
    end
    if defined? LOG_MAIL
      LOG_MAIL = Syslog::Constants::LOG_MAIL
    end
    if defined? LOG_NEWS
      LOG_NEWS = Syslog::Constants::LOG_NEWS
    end
    if defined? LOG_NTP
      LOG_NTP = Syslog::Constants::LOG_NTP
    end
    if defined? LOG_SECURITY
      LOG_SECURITY = Syslog::Constants::LOG_SECURITY
    end
    if defined? LOG_SYSLOG
      LOG_SYSLOG = Syslog::Constants::LOG_SYSLOG
    end
    if defined? LOG_USER
      LOG_USER = Syslog::Constants::LOG_USER
    end
    if defined? LOG_UUCP
      LOG_UUCP = Syslog::Constants::LOG_UUCP
    end
    if defined? LOG_LOCAL0
      LOG_LOCAL0 = Syslog::Constants::LOG_LOCAL0
    end
    if defined? LOG_LOCAL1
      LOG_LOCAL1 = Syslog::Constants::LOG_LOCAL1
    end
    if defined? LOG_LOCAL2
      LOG_LOCAL2 = Syslog::Constants::LOG_LOCAL2
    end
    if defined? LOG_LOCAL3
      LOG_LOCAL3 = Syslog::Constants::LOG_LOCAL3
    end
    if defined? LOG_LOCAL4
      LOG_LOCAL4 = Syslog::Constants::LOG_LOCAL4
    end
    if defined? LOG_LOCAL5
      LOG_LOCAL5 = Syslog::Constants::LOG_LOCAL5
    end
    if defined? LOG_LOCAL6
      LOG_LOCAL6 = Syslog::Constants::LOG_LOCAL6
    end
    if defined? LOG_LOCAL7
      LOG_LOCAL7 = Syslog::Constants::LOG_LOCAL7
    end
  end
  
  module Foreign
    extend FFI::Library
    ffi_lib FFI::Platform::LIBC

    # methods
    attach_function :open, "openlog", [:pointer, :int, :int], :void
    attach_function :close, "closelog", [], :void
    attach_function :write, "syslog", [:int, :string, :varargs], :void
    attach_function :set_mask, "setlogmask", [:int], :int
  end
  
  class << self

    ##
    # returns the ident of the last open call
    def ident
      @opened ? @ident : nil
    end
    
    ##
    # returns the options of the last open call
    def options
      @opened ? @options : nil
    end

    ##
    # returns the facility of the last open call
    def facility
      @opened ? @facility : nil
    end

    ##
    # mask
    #   mask=(mask)
    #
    # Returns or sets the log priority mask.  The value of the mask
    # is persistent and will not be reset by Syslog::open or
    # Syslog::close.
    #
    # Example:
    #   Syslog.mask = Syslog::LOG_UPTO(Syslog::LOG_ERR)
    def mask
      @mask ||= -1
      @opened ? @mask : nil
    end
    attr_writer :mask

    ##
    #   open(ident = $0, logopt = Syslog::LOG_PID | Syslog::LOG_CONS, facility = Syslog::LOG_USER) [{ |syslog| ... }]
    #
    # Opens syslog with the given options and returns the module
    # itself.  If a block is given, calls it with an argument of
    # itself.  If syslog is already opened, raises RuntimeError.
    #
    # Examples:
    #   Syslog.open('ftpd', Syslog::LOG_PID | Syslog::LOG_NDELAY, Syslog::LOG_FTP)
    #   open!(ident = $0, logopt = Syslog::LOG_PID | Syslog::LOG_CONS, facility = Syslog::LOG_USER)
    #   reopen(ident = $0, logopt = Syslog::LOG_PID | Syslog::LOG_CONS, facility = Syslog::LOG_USER)
    def open(ident=nil, opt=nil, fac=nil)
      raise "Syslog already open" unless not @opened

      ident ||= $0
      opt ||= Constants::LOG_PID | Constants::LOG_CONS
      fac ||= Constants::LOG_USER

      @ident = ident
      @options = opt
      @facility = fac
      @ident_memory = if ident
        FFI::MemoryPointer.from_string(ident)
      else
        nil
      end
      Foreign.open(@ident_memory, opt, fac)

      @opened = true

      # Calling set_mask twice is the standard way to set the 'default' mask
      @mask = Foreign.set_mask(0)
      Foreign.set_mask(@mask)

      if block_given?
        begin
          yield self
        ensure
          close
        end
      end

      self
    end

    ##
    # like open, but closes it first
    def reopen(*args, &block)
      close
      open(*args, &block)
    end

    alias_method :open!, :reopen

    ##
    # Is it open?
    def opened?
      @opened || false
    end

    ##
    # Close the log
    # close will raise an error if it is already closed
    def close
      raise "Syslog not opened" unless @opened

      Foreign.close
      @ident = nil
      @options = @facility = @mask = -1;
      @opened = false
    end

    ##
    #   log(Syslog::LOG_CRIT, "The %s is falling!", "sky")
    #  
    # Doesn't take any platform specific printf statements
    #   logs things to $stderr
    #   log(Syslog::LOG_CRIT, "Welcome, %s, to my %s!", "leethaxxor", "lavratory")
    def log(pri, *args)
      write(pri, *args)
    end

    ##
    # handy little shortcut for LOG_EMERG as the priority
    def emerg(*args);  write(Syslog::LOG_EMERG,   *args); end

    ##
    # handy little shortcut for LOG_ALERT as the priority
    def alert(*args);  write(Syslog::LOG_ALERT,   *args); end

    ##
    # handy little shortcut for LOG_ERR as the priority
    def err(*args);    write(Syslog::LOG_ERR,     *args); end

    ##
    # handy little shortcut for LOG_CRIT as the priority
    def crit(*args);   write(Syslog::LOG_CRIT,    *args); end

    ##
    # handy little shortcut for LOG_WARNING as the priority
    def warning(*args);write(Syslog::LOG_WARNING, *args); end

    ##
    # handy little shortcut for LOG_NOTICE as the priority
    def notice(*args); write(Syslog::LOG_NOTICE,  *args); end

    ##
    # handy little shortcut for LOG_INFO as the priority
    def info(*args);   write(Syslog::LOG_INFO,    *args); end

    ##
    # handy little shortcut for LOG_DEBUG as the priority
    def debug(*args);  write(Syslog::LOG_DEBUG,   *args); end

    ##
    #   LOG_MASK(pri)
    #
    # HACK copied from macro
    # Creates a mask for one priority.
    def LOG_MASK(pri)
      1 << pri
    end

    ##
    #   LOG_UPTO(pri)
    # HACK copied from macro
    # Creates a mask for all priorities up to pri.
    def LOG_UPTO(pri)
      (1 << ((pri)+1)) - 1
    end

    def inspect
      if @opened
        "<#%s: opened=true, ident=\"%s\", options=%d, facility=%d, mask=%d>" %
        [self.name, @ident, @options, @facility, @mask]
      else
        "<##{self.name}: opened=false>"
      end
    end

    ##
    #   Syslog.instance # => Syslog
    # Returns the Syslog module
    def instance
      self
    end

    FORMAT_STRING = '%s'
    def write(pri, format, *args)
      raise "Syslog must be opened before write" unless @opened

      message = format % args
      Foreign.write(pri, FORMAT_STRING, :string, message, :pointer, nil)
    end
    private :write
  end
end

