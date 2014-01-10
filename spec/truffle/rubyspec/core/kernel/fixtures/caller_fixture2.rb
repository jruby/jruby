module CallerFixture
  def block
    @block
  end
  module_function :block

  def block=(block)
    @block = block
  end
  module_function :block=

  def capture(&block)
    @block = block
  end
  module_function :capture

  def caller_of(block)
    eval("caller(0)", block.binding)
  end
  module_function :caller_of

  def eval_caller(depth)
    eval("caller(#{depth})")
  end
  module_function :eval_caller

  class InitializeRecorder
    attr_reader :caller_on_initialize
    def initialize(level)
      @caller_on_initialize = caller(level)
    end
  end
end
