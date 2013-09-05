class Fiber
  def self.current
    __current__
  end
  
  # FIXME: not quite right
  alias transfer resume
  
  def alive?
    __alive__
  end
end