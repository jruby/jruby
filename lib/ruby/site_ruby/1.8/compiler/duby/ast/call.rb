module Duby::AST
  class FunctionalCall < Node
    include Named
    attr_accessor :parameters, :block
        
    def initialize(parent, name)
      @parameters, @block = children = yield(self)
      @name = name
      super(parent, children)
    end
        
    def infer(typer)
      @self_type ||= typer.self_type
      
      unless @inferred_type
        receiver_type = @self_type
        parameter_types = parameters.map {|param| param.infer(typer)}
        @inferred_type = typer.method_type(receiver_type, name, parameter_types)
          
        if @inferred_type
          resolved!
        else
          typer.defer(self)
        end
      end
        
      @inferred_type
    end
  end
  
  class Call < Node
    include Named
    attr_accessor :target, :parameters, :block
        
    def initialize(parent, name)
      @target, @parameters, @block = children = yield(self)
      @name = name
      super(parent, children)
    end
        
    def infer(typer)
      unless @inferred_type
        receiver_type = target.infer(typer)
        parameter_types = parameters.map {|param| param.infer(typer)}
        @inferred_type = typer.method_type(receiver_type, name, parameter_types)
          
        if @inferred_type
          resolved!
        else
          typer.defer(self)
        end
      end
        
      @inferred_type
    end
  end
end