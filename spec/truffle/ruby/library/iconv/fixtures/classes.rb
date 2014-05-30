begin
  require 'iconv'

  module IconvSpecs
    class IconvSubclass < Iconv
    end
  end
rescue LoadError
  # do nothing
end

