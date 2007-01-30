module Signal
  def self.trap(sig)
    warn "Signal.trap: Signals are not implemented in JRuby and will not work"
    # do nothing
  end
end

class Continuation
  def [](*args)
    call(*args)
  end
  
  def call(*args)
    warn "Continuation#call: Continuations are not implemented in JRuby and will not work"
  end
end
