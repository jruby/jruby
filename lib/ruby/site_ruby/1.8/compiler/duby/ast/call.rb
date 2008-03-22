module Compiler::Duby::AST
  class FunctionalCall < Node
    include Named
    attr_accessor :parameters, :block
        
    def initialize(parent, name)
      @parameters, @block = children = yield(self)
      @name = name
      super(parent, children)
    end
        
    def infer_type(typer)
      unless @inferred_type
        receiver_type = typer.self_type
        parameter_types = parameters.map {|param| param.infer_type(typer)}
        @inferred_type = typer.method_type(receiver_type, name, parameter_types)
          
        unless @inferred_type
          typer.defer_inference(self)
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
        
    def infer_type(typer)
      unless @inferred_type
        receiver_type = target.infer_type(typer)
        parameter_types = parameters.map {|param| param.infer_type(typer)}
        @inferred_type = typer.method_type(receiver_type, name, parameter_types)
          
        unless @inferred_type
          typer.defer_inference(self)
        end
      end
        
      @inferred_type
    end
  end
end