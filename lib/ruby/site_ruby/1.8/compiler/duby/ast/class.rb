module Compiler::Duby::AST
  class ClassDefinition < Node
    include Named
    attr_accessor :superclass, :body
        
    def initialize(parent, name)
      @superclass, @body = children = yield(self)
      @name = name
      super(parent, children)
    end
  end
      
  class FieldAssignment < Node
    include Named
    include Valued
        
    def initialize(parent, name)
      @value = (children = yield(self))[0]
      @name = name
      super(parent, children)
    end
  end
      
  class Field < Node
    include Named
        
    def initialize(parent, name)
      super(parent, [])
      @name = name
    end
  end
end