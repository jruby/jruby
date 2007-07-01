class Proc
  # Include or extend this into an existing proc to make it invoke itself upon
  # any method
  module CatchAll
    def method_missing(name, *args, &ignored_block)
      self.call(*args)
    end
  end
end