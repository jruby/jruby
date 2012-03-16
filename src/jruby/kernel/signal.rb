module Signal
  def trap(sig, cmd = nil, &block)
    sig = SIGNALS[sig] if sig.kind_of?(Fixnum)
    sig = sig.to_s.sub(/^SIG(.+)/,'\1')

    if block
      Signal::__jtrap_kernel(block, sig)
    elsif cmd
      case cmd
      when Proc
        Signal::__jtrap_kernel(cmd, sig)
      when 'EXIT'
        Signal::__jtrap_kernel(proc{exit}, sig)
      when 'SIG_IGN', 'IGNORE'
        Signal::__jtrap_ignore_kernel(sig)
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
  rescue Exception
    warn "The signal #{sig} is in use by the JVM and will not work correctly on this platform"
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
    1  => "SIGHUP", 2  => "SIGINT", 3  => "SIGQUIT", 4  => "SIGILL", 5  => "SIGTRAP", 6  => "SIGABRT",
    7  => "SIGPOLL", 8  => "SIGFPE", 9  => "SIGKILL", 10 => "SIGBUS", 11 => "SIGSEGV", 12 => "SIGSYS",
    13 => "SIGPIPE", 14 => "SIGALRM", 15 => "SIGTERM", 16 => "SIGURG", 17 => "SIGSTOP", 18 => "SIGTSTP",
    19 => "SIGCONT", 20 => "SIGCHLD", 21 => "SIGTTIN", 22 => "SIGTTOU", 24 => "SIGXCPU", 25 => "SIGXFSZ",
    26 => "SIGVTALRM", 27 => "SIGPROF", 30 => "SIGUSR1", 31 => "SIGUSR2"
  }
end
