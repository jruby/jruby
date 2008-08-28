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
        compiler.local(name, inferred_type)
      end
    end
    
    class LocalAssignment
      def compile(compiler)
        compiler.local_assign(name, inferred_type) {
          value.compile(compiler)
        }
      end
    end
    
    class Script
      def compile(compiler)
        compiler.define_main(body)
      end
    end
    
    class MethodDefinition
      def compile(compiler)
        compiler.define_method(name, signature, arguments, body)
      end
    end
    
    class Arguments
      def compile(compiler)
        args.each {|arg| compiler.declare_argument(arg.name, arg.inferred_type)} if args
      end
    end
    
    class Noop
      def compile(compiler)
        # nothing
      end
    end
    
    class If
      def compile(compiler)
        compiler.branch(self)
      end
    end
    
    class Condition
      def compile(compiler)
        predicate.compile(compiler)
      end
    end
    
    class FunctionalCall
      def compile(compiler)
        compiler.self_call(self)
      end
    end
    
    class Call
      def compile(compiler)
        compiler.call(self)
      end
    end
  end
end