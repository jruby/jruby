module ObjectSpecs
  class IVars
    def initialize
      @secret = 99
    end
  end

  module InstExec
    def self.included(base)
      base.instance_exec { @@count = 2 }
    end
  end

  module InstExecIncluded
    include InstExec
  end
end
