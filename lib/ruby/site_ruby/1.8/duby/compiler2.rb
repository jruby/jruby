require 'duby'

module Duby
  module AST
    class Script
      def compile(compiler)
        # preparations for the .c file would go here
        body.compile(compiler)
      end
    end
    
    class Body
      def compile(compiler)
        last = children[-1]
        children.each do |child|
          child.compile(compiler)
          compiler.newline
        end
      end
    end
    
    class MethodDefinition
      def compile(compiler)
        args_callback = proc {arguments.compile(compiler)}
        body_callback = proc {body.compile(compiler)}
        compiler.define_method(name, signature, args_callback, body_callback)
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
    
    class Fixnum
      def compile(compiler)
        compiler.fixnum(literal)
      end
    end
    
    class If
      def compile(compiler)
        cond_callback = proc { condition.compile(compiler) }
        body_callback = proc { body.compile(compiler) }
        else_callback = proc { self.else.compile(compiler)}
        
        compiler.branch(cond_callback, body_callback, else_callback)
      end
    end
    
    class Condition
      def compile(compiler)
        predicate.compile(compiler)
      end
    end
    
    class FunctionalCall
      def compile(compiler)
        args_callback = proc { parameters.each {|param| param.compile(compiler)}}
        
        compiler.self_call(name, args_callback)
      end
    end
    
    class Call
      def compile(compiler)
        recv_callback = proc { target.compile(compiler) }
        args_callback = proc { parameters.each {|param| param.compile(compiler)}}
        
        compiler.call(name, target.inferred_type, recv_callback, args_callback)
      end
    end
    
    class Local
      def compile(compiler)
        compiler.local(name)
      end
    end
  end
end