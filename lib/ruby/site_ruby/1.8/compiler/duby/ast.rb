module Compiler::Duby
  class Node
    attr_accessor :children
    attr_accessor :parent
    attr_accessor :newline

    def initialize(parent, children = [])
      @parent = parent
      @children = children
      @newline = false
    end
    
    def inspect(indent = 0)
      indent_str = ' ' * indent
      str = indent_str + to_s
      children.each do |child|
        if child
          if ::Array === child
            child.each {|ary_child|
              str << "\n#{ary_child.inspect(indent + 1)}"
            }
          else
            str << "\n#{child.inspect(indent + 1)}"
          end
        end
      end
      str
    end
    
    def to_s
      self.class.name.split("::")[-1]
    end
      
    def [](index)
      children[index]
    end

    def each(&b)
      children.each(&b)
    end
  end
  
  module Named
    attr_accessor :name
  end
  
  module Typed
    attr_accessor :type
  end
  
  module Valued
    include Typed
    attr_accessor :value
  end
  
  module Literal
    include Typed
    attr_accessor :literal
  end

  class Arguments < Node
    attr_accessor :args, :opt_args, :rest_arg, :block_arg
    def initialize(parent)
      @args, @opt_args, @rest_arg, @block_arg = children = yield(self)
      super(parent, children)
    end
  end
  class Argument < Node
    include Typed
  end
  class RequiredArgument < Argument
    include Named
    def initialize(parent, name)
      super(parent)
      
      @name = name
    end
    
    def to_s
      "RequiredArgument(#{name})"
    end
  end
  class OptionalArgument < Argument
    include Named
    attr_accessor :child
    def initialize(parent)
      @child = (children = yield(self))[0]
      @name = @child.name
      super(parent, children)
    end
    
    def to_s
      "OptionalArgument(#{name})"
    end
  end
  class RestArgument < Argument
    include Named
    def initialize(parent, name)
      super(parent)
      
      @name = name
    end
    
    def to_s
      "RestArgument(#{name})"
    end
  end
  class BlockArgument < Argument
    include Named
    def initialize(parent, name)
      super(parent)
      
      @name = name
    end
    
    def to_s
      "BlockArgument(#{name})"
    end
  end
  class Array < Node
    def initialize(parent)
      super(parent, yield(self))
    end
  end
  class Begin < Node; end
  class Body < Node
    def initialize(parent)
      super(parent, yield(self))
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
    
    def to_s
      "Call(#{name})"
    end
  end
  class Class < Node; end
  class Colon2 < Node; end
  class Const < Node; end
  class Defn < Node; end
  class Defs < Node; end
  class FunctionalCall < Node
    include Named
    attr_accessor :parameters, :block
    def initialize(parent, name)
      @parameters, @block = children = yield(self)
      @name = name
      super(parent, children)
    end
    
    def to_s
      "FunctionalCall(#{name})"
    end
  end
  class Fixnum < Node
    include Literal
    def initialize(parent, literal)
      super(parent)
      @literal = literal
    end
    
    def to_s
      "Fixnum(#{literal})"
    end
  end
  class Float < Node
    include Literal
    def initialize(parent, literal)
      super(parent)
      @literal = literal
    end
    
    def to_s
      "Float(#{literal})"
    end
  end
  class Hash < Node; end
  class If < Node
    attr_accessor :condition, :body, :else
    def initialize(parent)
      @condition, @body, @else = children = yield(self)
      super(parent, children)
    end
  end
  class FieldDeclaration < Node
    include Named
    include Valued
    def initialize(parent, name)
      @type, @value = children = yield(self)
      @name = name
      super(parent, children)
    end
    
    def to_s
      "FieldDeclaration(type = #{type}, name = #{name})"
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
    
    def to_s
      "FieldAssignment(#{name})"
    end
  end
  class Field < Node
    include Named
    def initialize(parent, name)
      super(parent, [])
      @name = name
    end
    
    def to_s
      "Field(#{name})"
    end
  end
  class LocalDeclaration < Node
    include Named
    include Valued
    def initialize(parent, name)
      @type, @value = children = yield(self)
      @name = name
      super(parent, children)
    end
    
    def to_s
      "LocalDeclaration(type = #{type}, name = #{name})"
    end
  end
  class LocalAssignment < Node
    include Named
    include Valued
    def initialize(parent, name)
      @value = (children = yield(self))[0]
      @name = name
      super(parent, children)
    end
    
    def to_s
      "LocalAssignment(#{name})"
    end
  end
  class Local < Node
    include Named
    def initialize(parent, name)
      super(parent, [])
      @name = name
    end
    
    def to_s
      "Local(#{name})"
    end
  end
  class Module < Node; end
  class Not < Node
    def initialize(parent)
      super(parent, yield(self))
    end
  end
  class Return < Node; end
  class Root < Node; end
  class Self < Node; end
  class Str < Node; end
  class Symbol < Node; end
  class VCall < Node; end
  class While < Node; end
end