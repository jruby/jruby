module Kernel
  if JRuby::Util.windows? || !JRuby::Util.native_exec? || !JRuby::Util.native_posix?
    module_function def exec(*args)
      JRuby::Util.exec_old(*JRuby::ProcessUtil.exec_args(args))
    end
  end

  # Replaces Java version for better caching
  module_function def initialize_dup(original)
    initialize_copy(original)
  end

  # Replaces Java version for better caching
  module_function def initialize_clone(original, freeze: false)
    initialize_copy(original)
  end

  def tap(&b)
    # workaround for yield to lambda not triggering arity error
    # see https://bugs.ruby-lang.org/issues/12705
    (b && b.lambda?) ? b.call(self) : yield(self)
    self
  end
end
