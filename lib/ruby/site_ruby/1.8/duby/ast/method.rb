module Duby::AST
  class Arguments < Node
    attr_accessor :args, :opt_args, :rest_arg, :block_arg
    
    def initialize(parent)
      @args, @opt_args, @rest_arg, @block_arg = children = yield(self)
      super(parent, children)
    end
    
    def infer(typer)
      unless @inferred_type
        @inferred_type = args ? args.map {|arg| arg.infer(typer)} : []
      end
    end
  end
      
  class Argument < Node
    include Typed
  end
      
  class RequiredArgument < Argument
    include Named
    include Scoped
    
    def initialize(parent, name)
      super(parent)

      @name = name
    end
    
    def infer(typer)
      unless @inferred_type
        # if not already typed, check parent of parent (MethodDefinition) for signature info
        method_def = parent.parent
        signature = method_def.signature

        # if signature, search for this argument
        if signature[name.intern]
          @inferred_type = typer.learn_local_type(scope, name, signature[name.intern])
        else
          @inferred_type = typer.local_type(scope, name)
        end
          
        unless @inferred_type
          typer.defer(self)
        end
      end
        
      @inferred_type
    end
  end
      
  class OptionalArgument < Argument
    include Named
    include Scoped
    attr_accessor :child
    
    def initialize(parent)
      @child = (children = yield(self))[0]
      @name = @child.name
      super(parent, children)
    end
  end
      
  class RestArgument < Argument
    include Named
    include Scoped
    
    def initialize(parent, name)
      super(parent)

      @name = name
    end
  end
      
  class BlockArgument < Argument
    include Named
    
    def initialize(parent, name)
      super(parent)

      @name = name
    end
  end
      
  class MethodDefinition < Node
    include Named
    include Scope
    attr_accessor :signature, :arguments, :body
        
    def initialize(parent, name)
      @signature, @arguments, @body = children = yield(self)
      @name = name
      super(parent, children)
    end
        
    def infer(typer)
      arguments.infer(typer)
      forced_type = signature[:return]
      inferred_type = body.infer(typer)
        
      if !inferred_type
        typer.defer(self)
      else
        if forced_type != TypeReference::NoType && !forced_type.is_parent(inferred_type)
          raise InferenceError.new("Inferred return type is incompatible with declared", self)
        end

        @inferred_type = typer.learn_method_type(typer.self_type, name, arguments.inferred_type, inferred_type)
        signature[:return] = @inferred_type
      end
        
      @inferred_type
    end
  end
      
  class StaticMethodDefinition < Node
    include Named
    include Scope
    attr_accessor :signature, :arguments, :body
        
    def initialize(parent, name)
      @signature, @arguments, @body = children = yield(self)
      @name = name
      super(parent, children)
    end
        
    def infer(typer)
      arguments.infer(typer)
      forced_type = signature[:return]
      inferred_type = body.infer(typer)
        
      if !inferred_type
        typer.defer(self)
      else
        if forced_type != TypeReference::NoType && !forced_type.is_parent(inferred_type)
          raise InferenceError.new("Inferred return type is incompatible with declared", self)
        end

        # TODO: this doesn't separate static from instance
        @inferred_type = typer.learn_method_type(typer.self_type, name, arguments.inferred_type, inferred_type)
      end
        
      @inferred_type
    end
  end
end