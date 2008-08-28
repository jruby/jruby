require 'duby'
require 'duby/compiler'

module Duby
  module Compiler
    class C
      class MathCompiler
        def call(compiler, name, recv, args)
          recv.compile(compiler)

          compiler.src << " #{name} "

          args.each {|arg| arg.compile(compiler)}
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
        ast.compile(self)
      end
      
      def define_method(name, signature, args, body)
        @src << "#{type_mapper[signature[:return]]} #{name}("
        
        args.compile(self)
        
        @src << ") {"
        
        body.compile(self)
        
        @src << "}\n\n"
      end
      
      def declare_argument(name, type)
        @src << "#{type_mapper[type]} #{name}"
      end
      
      def branch(condition, body, els)
        @src << "if ("
        
        condition.compile(self)
        
        @src << ") {"
        
        body.compile(self)
        
        if els
          @src << "} else {"
          
          els.compile(self)
        end
        
        @src << "}"
      end
      
      def call(name, recv, args)
        call_compilers[recv.inferred_type].call(self, name, recv, args)
      end
      
      def call_compilers
        @call_compilers ||= {}
      end
      
      def self_call(name, args)
        @src << "#{name}("
        
        args.each {|arg| arg.compile(self)}
        
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