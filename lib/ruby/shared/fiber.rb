class Fiber
  def self.current
    __current__
  end
  
  alias transfer resume
  
  def alive?
    __alive__
  end
end