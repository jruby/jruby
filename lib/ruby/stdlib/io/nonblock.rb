class IO
  public
  def nonblock?
    JRuby.reference(self).blocking?
  end

  def nonblock=(blocking)
    JRuby.reference(self).blocking = blocking
  end

  def nonblock(nonblocking = true)
    JRuby.reference(self).blocking = !nonblocking;
    if block_given?
      begin
        yield self
      ensure
        JRuby.reference(self).blocking = nonblocking;
      end
    end
  end
end