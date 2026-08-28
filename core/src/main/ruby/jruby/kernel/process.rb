module Process
  ARGV0 = -$0
  private_constant :ARGV0
  module_function def argv0
    return ARGV0
  end

  class WaitThread < Thread
    # only created from Java and used for popen3 right now
    class << self
      private :new
    end

    def pid
      self[:pid]
    end
  end
  private_constant :WaitThread

  class Waiter < Thread
    # only created from Java and used for Process.detach right now
    class << self
      private :new
    end

    def pid
      self[:pid]
    end
  end
end
