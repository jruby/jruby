module ::Kernel
  SIGNALS = {
    1  => "SIGHUP", 2  => "SIGINT", 3  => "SIGQUIT", 4  => "SIGILL", 5  => "SIGTRAP", 6  => "SIGABRT",
    7  => "SIGPOLL", 8  => "SIGFPE", 9  => "SIGKILL", 10 => "SIGBUS", 11 => "SIGSEGV", 12 => "SIGSYS",
    13 => "SIGPIPE", 14 => "SIGALRM", 15 => "SIGTERM", 16 => "SIGURG", 17 => "SIGSTOP", 18 => "SIGTSTP",
    19 => "SIGCONT", 20 => "SIGCHLD", 21 => "SIGTTIN", 22 => "SIGTTOU", 24 => "SIGXCPU", 25 => "SIGXFSZ",
    26 => "SIGVTALRM", 27 => "SIGPROF", 30 => "SIGUSR1", 31 => "SIGUSR2"
  }
  begin
    def trap(*args, &block)
      sig = args.first
      sig = SIGNALS[sig] if sig.kind_of?(Fixnum)
      sig = sig.to_s.sub(/^SIG(.+)/,'\1')
      signal_class = Java::sun.misc.Signal
      signal_class.send :attr_accessor, :prev_handler
      signal_object = signal_class.new(sig)
      signal_handler = Java::sun.misc.SignalHandler.impl do
        begin
          block.call
        ensure
          # re-register the handler
          signal_class.handle(signal_object, signal_handler)
        end
      end
      signal_object.prev_handler = signal_class.handle(signal_object, signal_handler)
    end
  rescue NameError
    def trap(*args, &block)
      warn "trap not supported by this VM"
    end
  end
end
