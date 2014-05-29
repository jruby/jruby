module KernelSpecs
  class Method
    def callcc
      super
    end
  end

  def self.before_and_after
    i = "before"
    cont = callcc { |c| c }
    if cont # nil the second time
      i = "after"
      cont.call
    end
    i
  end
end
