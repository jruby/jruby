require File.expand_path('../../fixtures/match_operators', __FILE__)

class OperatorImplementor
  def !~(val)
    return val
  end
end
