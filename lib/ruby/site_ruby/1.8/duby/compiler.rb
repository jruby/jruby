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
  end
end