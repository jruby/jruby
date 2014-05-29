require 'mspec/runner/actions/filter'

class DebugAction < ActionFilter
  def before(state)
    require 'rubygems'
    require 'ruby-debug'
    Kernel.debugger if self === state.description
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
