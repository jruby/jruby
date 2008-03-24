module Duby::AST
  class LocalAssignment < Node
    include Named
    include Valued
    include Scoped
    
    def initialize(parent, name)
      @value = (children = yield(self))[0]
      @name = name
      super(parent, children)
    end

    def to_s
      "LocalAssignment(name = #{name}, scope = #{scope})"
    end
    
    def infer(typer)
      unless @inferred_type
        @inferred_type = typer.learn_local_type(scope, name, value.infer(typer))

        unless @inferred_type
          typer.defer(self)
        end
      end

      @inferred_type
    end
  end

  class Local < Node
    include Named
    include Scoped
    
    def initialize(parent, name)
      super(parent, [])
      @name = name
    end

    def to_s
      "Local(name = #{name}, scope = #{scope})"
    end
    
    def infer(typer)
      unless @inferred_type
        @inferred_type = typer.local_type(scope, name)

        unless @inferred_type
          typer.defer(self)
        end
      end

      @inferred_type
    end
  end
end