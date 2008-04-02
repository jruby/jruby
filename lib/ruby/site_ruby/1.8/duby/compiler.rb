require 'duby'

module Duby
  module AST
    class Fixnum
      def compile(compiler)
        compiler.fixnum(literal)
      end
    end
    
    class String
      def compile(compiler)
        compiler.string(literal)
      end
    end
    
    class Float
      def compile(compiler)
        compiler.float(literal)
      end
    end
    
    class Boolean
      def compile(compiler)
        compiler.boolean(literal)
      end
    end
    
    class Body
      def compile(compiler)
        children.each {|child| child.compile(compiler)}
      end
    end
    
    class Local
      def compile(compiler)
        compiler.local(inferred_type, name)
      end
    end
    
    class LocalAssignment
      def compile(compiler)
        compiler.local_assign(inferred_type, name) {
          value.compile(compiler)
        }
      end
    end
  end
end