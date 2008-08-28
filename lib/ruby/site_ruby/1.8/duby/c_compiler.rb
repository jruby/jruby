require 'duby'
require 'duby/compiler'

module Duby
  module Compiler
    class C
      class MathCompiler
        def call(compiler, call)
          call.target.compile(compiler)

          compiler.src << " #{call.name} "

          call.parameters.each {|param| param.compile(compiler)}
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

      def define_main(body)
        old_src, @src = @src, "int main(int argc, char **argv)\n{\n"

        body.compile(self)

        @src << "}\n\n"

        @src = old_src + @src
      end
      
      def define_method(name, signature, args, body)
        old_src, @src =  @src, "#{type_mapper[signature[:return]]} #{name}("
        
        args.compile(self)
        
        @src << ")\n{\n"
        
        body.compile(self)
        
        @src << "\n}\n\n"

        @src = old_src + @src
      end
      
      def declare_argument(name, type)
        @src << "#{type_mapper[type]} #{name}"
      end
      
      def branch(iff)
        @src << "if ("
        
        iff.condition.compile(self)
        
        @src << ") {"
        
        iff.body.compile(self)
        
        if iff.else
          @src << "} else {"
          
          iff.else.compile(self)
        end
        
        @src << "}"
      end
      
      def call(call)
        call_compilers[call.target.inferred_type].call(self, call)
      end
      
      def call_compilers
        @call_compilers ||= {}
      end
      
      def self_call(fcall)
        @src << "#{fcall.name}("
        
        fcall.parameters.each {|param| param.compile(self)}
        
        @src << ")"
      end
      
      def local(name, type)
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