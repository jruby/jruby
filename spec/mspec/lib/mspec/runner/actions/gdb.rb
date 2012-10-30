require 'mspec/runner/actions/filter'

class GdbAction < ActionFilter
  def before(state)
    Kernel.yield_gdb(true) if self === state.description
  end

  def register
    super
    MSpec.register :before, self
  end

  def unregister
    super
    MSpec.unregister :before, self
  end
end
