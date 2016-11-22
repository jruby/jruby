module Signal
  def trap(sig, cmd = nil, &block)
    sig = SIGNALS[sig] if sig.kind_of?(Integer)
    sig = sig.to_s.sub(/^SIG(.+)/,'\1')

    if RESERVED_SIGNALS.include?(sig)
      raise ArgumentError.new("can't trap reserved signal: SIG%s" % sig)
    end

    oldhandler, installed = if block
      raise SecurityError.new("Insecure: tainted signal trap") if block.tainted?
      Signal::__jtrap_kernel(block, sig)
    else
      raise SecurityError.new("Insecure: tainted signal trap") if cmd.tainted?
      cmd = cmd.to_s if Symbol === cmd
      case cmd
      when Proc
        Signal::__jtrap_kernel(cmd, sig)
      when 'EXIT'
        Signal::__jtrap_kernel(proc{exit}, sig)
      when NilClass, 'SIG_IGN', 'IGNORE'
        Signal::__jtrap_restore_kernel(sig)
      when 'SIG_DFL', 'DEFAULT'
        Signal::__jtrap_platform_kernel(sig)
      when 'SYSTEM_DEFAULT'
        Signal::__jtrap_osdefault_kernel(sig)
      when String
        Signal::__jtrap_kernel(proc{eval cmd, TOPLEVEL_BINDING}, sig)
      else
        Signal::__jtrap_kernel(proc{cmd.call}, sig)
      end
    end

    if !installed
      warn "The signal #{sig} is in use by the JVM and will not work correctly on this platform"
    end

    oldhandler
  end
  
  module_function :trap
end

module Kernel
  def trap(sig, cmd = nil, &block)
    ::Signal.trap(sig, cmd, &block)
  end
  module_function :trap
end

class Object
  SIGNALS = {
    0  => "SIGEXIT",
    1  => "SIGHUP", 2  => "SIGINT", 3  => "SIGQUIT", 4  => "SIGILL", 5  => "SIGTRAP", 6  => "SIGABRT",
    7  => "SIGPOLL", 8  => "SIGFPE", 9  => "SIGKILL", 10 => "SIGBUS", 11 => "SIGSEGV", 12 => "SIGSYS",
    13 => "SIGPIPE", 14 => "SIGALRM", 15 => "SIGTERM", 16 => "SIGURG", 17 => "SIGSTOP", 18 => "SIGTSTP",
    19 => "SIGCONT", 20 => "SIGCHLD", 21 => "SIGTTIN", 22 => "SIGTTOU", 24 => "SIGXCPU", 25 => "SIGXFSZ",
    26 => "SIGVTALRM", 27 => "SIGPROF", 30 => "SIGUSR1", 31 => "SIGUSR2"
  }

  RESERVED_SIGNALS = %w(SEGV BUS ILL FPE VTALRM)
end
