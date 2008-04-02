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