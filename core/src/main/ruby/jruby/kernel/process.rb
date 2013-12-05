module Process
  class WaitThread < Thread
    # only created from Java and used for popen3 right now
    class << self
      private :new
    end
    
    def pid
      self[:pid]
    end
  end
  def self.spawn(*args)
    _spawn_internal(*JRuby::ProcessUtil.exec_args(args))
  end
end
