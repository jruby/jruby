module ::Kernel
  SIGNALS = {
    1  => "SIGHUP", 2  => "SIGINT", 3  => "SIGQUIT", 4  => "SIGILL", 5  => "SIGTRAP", 6  => "SIGABRT",
    7  => "SIGPOLL", 8  => "SIGFPE", 9  => "SIGKILL", 10 => "SIGBUS", 11 => "SIGSEGV", 12 => "SIGSYS",
    13 => "SIGPIPE", 14 => "SIGALRM", 15 => "SIGTERM", 16 => "SIGURG", 17 => "SIGSTOP", 18 => "SIGTSTP",
    19 => "SIGCONT", 20 => "SIGCHLD", 21 => "SIGTTIN", 22 => "SIGTTOU", 24 => "SIGXCPU", 25 => "SIGXFSZ",
    26 => "SIGVTALRM", 27 => "SIGPROF", 30 => "SIGUSR1", 31 => "SIGUSR2"
  }
  
  begin
    JavaSignal = Java::sun.misc.Signal
    def __jtrap(*args, &block)
      sig = args.shift
      sig = SIGNALS[sig] if sig.kind_of?(Fixnum)
      sig = sig.to_s.sub(/^SIG(.+)/,'\1')

      block = args.shift unless args.empty?

      signal_object = JavaSignal.new(sig) rescue nil
      return unless signal_object
      
      Signal::__jtrap_kernel(block, signal_object, sig)
    rescue Exception
      warn "The signal #{sig} is in use by the JVM and will not work correctly on this platform"
    end
  rescue NameError
    def __jtrap(*args, &block)
      warn "trap not supported or not allowed by this VM"
    end
  end
end
