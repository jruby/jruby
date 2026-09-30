$autoload_required_loads << __FILE__

class TestAutoload
  module Imported
    java_import java.util.ArrayList
  end
end
