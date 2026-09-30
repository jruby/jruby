$autoload_required_loads << __FILE__
require File.expand_path("circular_second", __dir__)

class TestAutoload
  module Circular
    class First
    end
  end
end
