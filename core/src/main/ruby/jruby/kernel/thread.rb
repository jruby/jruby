class Thread
  @ignore_deadlock = false

  class << self
    attr_accessor :ignore_deadlock
  end
end