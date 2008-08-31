module Duby::AST
  class PrintLine < Node
    attr_accessor :parameters
    
    def initialize(parent)
      @parameters = children = yield(self)
      super(parent, children)
    end

    def infer(typer)
      resolved = parameters.select {|param| param.infer(typer); param.resolved?}
      resolved! if resolved.size == parameters.size
      TypeReference::NoType
    end
  end
end