$autoload_required_loads << __FILE__
require File.expand_path("circular_first", __dir__)

class TestAutoload
  module Circular
    class Second
    end
  end
end
