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
  
  compiler = Duby::Compiler::JVM.new("#{ARGV[0].split('.')[0]}.class")
  ast.compile(compiler)
  
  File.open(compiler.filename, "w") {|file| file.write(compiler.generate)}
end
__END__
require 'duby'

module Duby
  module Compiler
    class JVM
      class CallCompiler < Proc
        def call(method_builder, name, recv_type, signature, recv, args)
          call(method_builder, name, recv_type, signature, recv, args)
        end
      end
      class MathCompiler
        def call(compiler, name, recv_type, recv, args)
          recv.call

          compiler.src << " #{name} "

          args.call
        end
      end
      
      class BranchCompiler < Proc
        def branch(method_builder, predicate, body_callback, else_callback)
          call(method_builder, predicate, body_callback, else_callback)
        end
      end
      
      IntCallCompiler = proc do |builder, name, recv_callback, args_callback|
        recv_callback.call
        args_callback.call
      end
      
      IntReturnCompiler = proc do |builder, result|
        result.call
        builder.ireturn
      end
      
      IntLocalCompiler = proc do |builder, index|
        builder.iload(index)
      end
      
      attr_accessor :filename, :src

      def initialize(filename)
        @filename = filename
        @file_builder = FileBuilder.new(filename)
        
        fixnum_type = AST::TypeReference.new(:fixnum)
        
        self.type_mapper[fixnum_type] = Java::int.java_class
        
        self.call_compilers[fixnum_type] = MathCompiler.new
        
        self.return_compilers[fixnum_type] = IntReturnCompiler
        
        self.local_compilers[fixnum_type] = IntLocalCompiler
        
        self.branch_compilers[fixnum_type] = IntBranchCompiler
      end

      def compile(ast)
        ast.compile(this)
      end
      
      def define_method(name, signature, args, body)
        # if we encounter a loose method, init class builder with script name
        @class_builder ||= @file_builder.public_class(@filename)
        
        @class_builder.method(name, signature.map {|type| type_mapper[type]}) do |method_builder|
          old_builder, @method_builder = @method_builder, method_builder
          body.call
          @method_builder = old_builder
        end
      end
      
      def declare_argument(name, type)
        #@src << "#{type_mapper[type]} #{name}"
      end
      
      def branch(condition_type, condition, body_proc, else_proc)
        #condition compiles itself as appropriate type of jump
        branch_compilers[condition_type].call(self, condition, body_proc, else_proc)
      end
      
      def call(name, recv_type, signature, recv, args)
        call_compilers[recv_type].call(self, name, signature, recv, args)
      end
      
      def call_compilers
        @call_compilers ||= {}
      end
      
      def self_call(name, signature, args)
        args.call
        call_compilers[self_type].self_call(self, name, signature, args)
      end
      
      def local(local_type, name)
        index = @method_builder.local(name)
        local_compilers[local_type].call(@method_builder, name)
      end
      
      def fixnum(value)
        @method_builder.ldc_int(value)
      end
      
      def newline
        @method_builder.pop
      end
      
      def ret(result_type, &result)
        return_compilers[result_type].call(@method_builder, result)
      end
      
      def generate
        @file_builder.generate
      end
      
      def type_mapper
        @type_mapper ||= {}
      end
      
      def branch_compilers
        @branch_compilers ||= {}
      end
      
      def local_compilers
        @local_compilers ||= {}
      end
      
      def return_compilers
        @return_compilers ||= {}
      end
    end
  end
end

if __FILE__ == $0
  ast = Duby::AST.parse(File.read(ARGV[0]))
  
  typer = Duby::Typer::Simple.new(:script)
  ast.infer(typer)
  typer.resolve(true)
  
  compiler = Duby::Compiler::JVM.new("#{ARGV[0]}.class")
  ast.compile(compiler)
  
  File.open(compiler.filename, "w") {|file| file.write(compiler.generate)}
end