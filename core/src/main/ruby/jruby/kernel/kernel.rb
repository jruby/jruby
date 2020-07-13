module Kernel
  module_function
  def exec(*args)
    _exec_internal(*JRuby::ProcessUtil.exec_args(args))
  end

  # Replaces Java version for better caching
  def initialize_dup(original)
    initialize_copy(original)
  end

  # Replaces Java version for better caching
  def initialize_clone(original)
    initialize_copy(original)
  end
end
