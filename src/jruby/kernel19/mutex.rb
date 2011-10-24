# Mutex addition for 1.9 mode

class Mutex
  def synchronize
    self.lock
    begin
      yield
    ensure
      self.unlock rescue nil
    end
  end
end