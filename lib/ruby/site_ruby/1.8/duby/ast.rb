require 'duby/transform'

module Duby
  module AST
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
        @resolved = false
      end

      def log(message)
        puts "* [AST] [#{simple_name}] " + message if $DEBUG
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

      def simple_name
        self.class.name.split("::")[-1]
      end

      def to_s; simple_name; end

      def [](index) children[index] end

      def each(&b) children.each(&b) end

      def resolved!
        log "resolved!"
        @resolved = true
      end

      def resolved?; @resolved end
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

    class Colon2 < Node; end

    class Constant < Node
      include Named
      def initialize(parent, name)
        @name = name
        super(parent, [])
      end
    end

    class Self < Node; end

    class VoidType < Node; end

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
        "Type(#{name}#{array? ? ' array' : ''})"
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

      def compatible?(other)
        # default behavior is only exact match right now
        self == other
      end

      def narrow(other)
        # only exact match allowed for now, so narrowing is a noop
        self
      end

      NoType = TypeReference.new(:notype)
    end

    class TypeDefinition < TypeReference
      attr_accessor :superclass

      def initialize(name, superclass)
        super(name, false)

        @superclass = superclass
      end
    end
    
    # Shortcut method to construct type references
    def self.type(typesym, array = false)
      TypeReference.new(typesym, array)
    end
  end
end

require 'duby/ast/local'
require 'duby/ast/call'
require 'duby/ast/flow'
require 'duby/ast/literal'
require 'duby/ast/method'
require 'duby/ast/class'
require 'duby/ast/structure'