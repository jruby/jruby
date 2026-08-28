module ASimpleLib
  A_CONST = 42
  class Error < RuntimeError
    def initialize
      super
    end
  end
end