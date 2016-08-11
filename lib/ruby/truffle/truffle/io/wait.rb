class IO
  def nread
    raise NotImplementedError
  end

  def ready?
    not Kernel.select([self], [], [], 0).nil?
  end

  def wait(timeout = nil)
    Kernel.select([self], [], [], timeout).nil? ? nil : self
  end

  alias_method :wait_readable, :wait

  def wait_writable(timeout = nil)
    Kernel.select([], [self], [], timeout).nil? ? nil : self
  end
end
