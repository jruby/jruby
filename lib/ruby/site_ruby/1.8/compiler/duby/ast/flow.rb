module Compiler::Duby::AST
  class Condition < Node
    attr_accessor :predicate
        
    def initialize(parent)
      @predicate = (children = yield(self))[0]
      super(parent, children)
    end
  end
      
  class If < Node
    attr_accessor :condition, :body, :else
    
    def initialize(parent)
      @condition, @body, @else = children = yield(self)
      super(parent, children)
    end
  end
  
  class Loop < Node
    attr_accessor :condition, :body, :check_first, :negative
    
    def initialize(parent, check_first, negative)
      @condition, @body = children = yield(self)
      @check_first = check_first
      @negative = negative
      super(parent, children)
    end

    def check_first?; @check_first; end
    def negative?; @negative; end

    def to_s
      "Loop(check_first = #{check_first?}, negative = #{negative?})"
    end
  end
  
  class Not < Node
    def initialize(parent)
      super(parent, yield(self))
    end
  end
  
  class Return < Node
    include Valued
        
    def initialize(parent)
      @value = (children = yield(self))[0]
      super(parent, children)
    end
  end
      
  class While < Node; end
end