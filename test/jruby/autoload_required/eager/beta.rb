$autoload_required_loads << __FILE__

class TestAutoload
  module Eager
    class Beta < Alpha
    end
  end
end
