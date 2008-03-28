require 'duby'

module Duby
  module Compiler
    class C
      class MathCompiler
        def call(compiler, name, recv, args)
          recv.call

          compiler.src << " #{name} "

          args.call
        end
      end
      
      attr_accessor :filename, :src

      def initialize(filename)
        @filename = filename
        @src = ""
        
        self.type_mapper[AST::TypeReference.new(:fixnum)] = "int"
        
        self.call_compilers[AST::TypeReference.new(:fixnum)] = MathCompiler.new
      end

      def compile(ast)
        ast.compile(this)
      end
      
      def define_method(name, signature, args, body)
        @src << "#{type_mapper[signature[:return]]} #{name}("
        
        args.call
        
        @src << ") {"
        
        body.call
        
        @src << "}\n\n"
      end
      
      def declare_argument(name, type)
        @src << "#{type_mapper[type]} #{name}"
      end
      
      def branch(condition, body_proc, else_proc)
        @src << "if ("
        
        condition.call
        
        @src << ") {"
        
        body_proc.call
        
        if else_proc
          @src << "} else {"
          
          else_proc.call
        end
        
        @src << "}"
      end
      
      def call(name, recv_type, recv, args)
        call_compilers[recv_type].call(self, name, recv, args)
      end
      
      def call_compilers
        @call_compilers ||= {}
      end
      
      def self_call(name, args)
        @src << "#{name}("
        
        args.call
        
        @src << ")"
      end
      
      def local(name)
        @src << name
      end
      
      def fixnum(value)
        @src << value.to_s
      end
      
      def newline
        @src << ";\n"
      end
      
      def ret
        @src << "return "
        
        yield
      end
      
      def generate
        @src
      end
      
      def type_mapper
        @type_mapper ||= {}
      end
    end
  end
  
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

if __FILE__ == $0
  ast = Duby::AST.parse(File.read(ARGV[0]))
  
  typer = Duby::Typer::Simple.new(:script)
  ast.infer(typer)
  typer.resolve(true)
  
  compiler = Duby::Compiler::C.new("#{ARGV[0]}.c")
  ast.compile(compiler)
  
  File.open(compiler.filename, "w") {|file| file.write(compiler.generate)}
end