module Compiler::Duby::AST
  class Body < Node
    def initialize(parent)
      super(parent, yield(self))
    end
        
    # Type of a block is the type of its final element
    def infer_type(typer)
      unless @inferred_type
        if children.size == 0
          @inferred_type = typer.default_type
        else
          children.each {|child| @inferred_type = child.infer_type(typer)}
        end
          
        unless @inferred_type
          typer.defer_inference(self)
        end
      end

      @inferred_type
    end
  end
  
  class Noop < Node
    def infer_type(typer)
      @inferred_type ||= typer.default_type
    end
  end
  
  class Script < Node
    include Scope
    attr_accessor :body
    
    def initialize(parent)
      @body = (children = yield(self))[0]
      super(parent, children)
    end
    
    def infer_type(typer)
      @inferred_type ||= body.infer_type(typer) || (typer.defer_inference(self); nil)
    end
  end
end