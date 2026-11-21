module Signal

  def trap(sig, cmd = nil, &block)
    if sig.nil? || !sig.kind_of?(Integer) && !sig.kind_of?(Symbol) && !sig.kind_of?(String)
      raise ArgumentError.new("bad signal type #{sig.class}")
    end
    if sig.kind_of?(Integer)
      signo = sig
      sig = Signal.signame(signo) rescue nil
      raise ArgumentError.new("invalid signal number (#{signo})") unless sig
    end
    sig = sig.to_s
    sig = sig[3..-1] if sig.start_with?('SIG') && sig.length > 3

    if %w(SEGV BUS ILL FPE VTALRM).include?(sig)
      raise ArgumentError.new("can't trap reserved signal: SIG%s" % sig)
    end

    oldhandler, installed = if block
      Signal::__jtrap_kernel(block, sig)
    else
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
        Signal::__jtrap_kernel(proc{|s|cmd.call s}, sig)
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
