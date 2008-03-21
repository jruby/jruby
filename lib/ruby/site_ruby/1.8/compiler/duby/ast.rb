module Compiler::Duby::AST
  # The top of the AST class hierarchy, this represents an abstract AST node.
  # It provides accessors for _children_, an array of all child nodes,
  # _parent_, a reference to this node's parent (nil if none), and _newline_,
  # whether this node represents a new line.
  class Node
    attr_accessor :children
    attr_accessor :parent
    attr_accessor :newline
    attr_accessor :inferred_type

    def initialize(parent, children = [])
      @parent = parent
      @children = children
      @newline = false
      @inferred_type = nil
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
          elsif ::Hash === child
            str << "\n#{indent_str} #{child.inspect}"
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
    
    def to_s
      "#{super}(#{name})"
    end
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
    
    def to_s
      "#{super}(#{literal.inspect})"
    end
  end
  
  module Scoped
    def scope
      @scope ||= begin
        scope = parent
        scope = scope.parent until scope.class.include? Scope
        scope
      end
    end
  end
  
  module Scope; end
  
  class TypeReference < Node
    include Named
    attr_accessor :array
    def initialize(name, array = false)
      @name = name
      @array = array
      super(nil)
    end
    
    def array?; @array; end
    
    def to_s
      "TypeReference(#{name}, array = #{array})"
    end
    
    def ==(other)
      to_s == other.to_s
    end
    
    def eql?(other)
      self == other
    end
    
    def hash
      to_s.hash
    end
    
    def is_parent(other)
      # default behavior now is to disallow any polymorphic types
      self == other
    end
    
    NoType = TypeReference.new(:notype)
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
    include Scoped
    def initialize(parent, name)
      super(parent)
      
      @name = name
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
  class Array < Node
    def initialize(parent)
      super(parent, yield(self))
    end
  end
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
  end
  class ClassDefinition < Node
    include Named
    attr_accessor :superclass, :body
    def initialize(parent, name)
      @superclass, @body = children = yield(self)
      @name = name
      super(parent, children)
    end
  end
  class Colon2 < Node; end
  class Condition < Node
    attr_accessor :predicate
    def initialize(parent)
      @predicate = (children = yield(self))[0]
      super(parent, children)
    end
  end
  class Constant < Node
    include Named
    def initialize(parent, name)
      @name = name
      super(parent, [])
    end
  end
  class FunctionalCall < Node
    include Named
    attr_accessor :parameters, :block
    def initialize(parent, name)
      @parameters, @block = children = yield(self)
      @name = name
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
  end
  class Field < Node
    include Named
    def initialize(parent, name)
      super(parent, [])
      @name = name
    end
  end
  class Fixnum < Node
    include Literal
    def initialize(parent, literal)
      super(parent)
      @literal = literal
    end
  end
  class Float < Node
    include Literal
    def initialize(parent, literal)
      super(parent)
      @literal = literal
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
  class LocalDeclaration < Node
    include Named
    include Valued
    include Scoped
    def initialize(parent, name)
      @type, @value = children = yield(self)
      @name = name
      super(parent, children)
    end
    
    def to_s
      "LocalDeclaration(type = #{type}, name = #{name}, scope = #{scope})"
    end
  end
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
  class MethodDefinition < Node
    include Named
    include Scope
    attr_accessor :signature, :arguments, :body
    def initialize(parent, name)
      @signature, @arguments, @body = children = yield(self)
      @name = name
      super(parent, children)
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
  end
  class Module < Node; end
  class Noop < Node; end
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
  class Script < Node
    include Scope
    attr_accessor :body
    def initialize(parent)
      @body = (children = yield(self))[0]
      super(parent, children)
    end
  end
  class Self < Node; end
  class String < Node
    include Literal
    def initialize(parent, literal)
      super(parent)
      @literal = literal
    end
  end
  class Symbol < Node; end
  class VoidType < Node; end
  class While < Node; end
end