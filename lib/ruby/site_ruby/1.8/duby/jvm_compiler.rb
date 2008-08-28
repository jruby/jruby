require 'duby'
require 'compiler/builder'

module Duby
  module Compiler
    class JVM
      class MathCompiler
        def call(compiler, call)
          call.target.compile(compiler)
          call.parameters.each {|param| param.compile(compiler)}

          target_type = call.target.inferred_type
          case target_type
          when AST.type(:fixnum)
            case call.name
            when '-'
              compiler.method.isub
            when '+'
              compiler.method.iadd
            else
              compiler.method.invokevirtual(
                compiler.type_mapper[call.target.inferred_type],
                call.name,
                [compiler.type_mapper[call.inferred_type], *call.parameters.map {|param| compiler.type_mapper[param.inferred_type]}])
            end
          else
            compiler.method.invokevirtual(
              compiler.type_mapper[call.target.inferred_type],
              call.name,
              [compiler.type_mapper[call.inferred_type], *call.parameters.map {|param| compiler.type_mapper[param.inferred_type]}])
          end
        end
      end
      
      attr_accessor :filename, :src, :method

      def initialize(filename)
        @filename = filename
        @src = ""
        
        self.type_mapper[AST.type(:fixnum)] = Java::int
        self.type_mapper[AST.type(:string, true)] = Java::java.lang.String[]
        
        self.call_compilers[AST.type(:fixnum)] = MathCompiler.new

        @file = ::Compiler::FileBuilder.new(filename)
        @class = @file.public_class(filename.split('.')[0])
      end

      def compile(ast)
        ast.compile(this)
      end

      def define_main(body)
        oldmethod, @method = @method, @class.static_method("main", type_mapper[AST.type(:void)], type_mapper[AST.type(:string, true)])

        @method.start

        body.compile(self)

        @method.returnvoid
        @method.stop
        
        @method = oldmethod
      end
      
      def define_method(name, signature, args, body)
        oldmethod, @method = @method, @class.static_method(name.to_s, type_mapper[signature[:return]], *args.args.map {|arg| type_mapper[arg.inferred_type]})

        @method.start
        
        #args.call
        
        body.compile(self)

        case signature[:return]
        when AST.type(:fixnum)
          @method.ireturn
        else
          @method.aload 0
          @method.areturn
        end
        
        @method.stop

        @method = oldmethod
      end
      
      def declare_argument(name, type)
        # declare local vars for arguments here
      end
      
      def branch(iff)
        elselabel = @method.label
        donelabel = @method.label
        
        # this is ugly...need a better way to abstract the idea of compiling a
        # conditional branch while still fitting into JVM opcodes
        predicate = iff.condition.predicate
        case iff.condition.predicate
        when AST::Call
          case predicate.target.inferred_type
          when AST.type(:fixnum)
            # fixnum conditional, so we need to use JVM opcodes
            case predicate.parameters[0].inferred_type
            when AST.type(:fixnum)
              # fixnum on fixnum, easy
              case predicate.name
              when '<'
                predicate.target.compile(self)
                predicate.parameters[0].compile(self)
                @method.if_icmpge(elselabel)
              else
                raise "Unknown :fixnum on :fixnum predicate operation: " + predicate.name
              end
            else
              raise "Unknown :fixnum on " + predicate.parameters[0].inferred_type + " predicate operations: " + predicate.name
            end
          else
            raise "Unknown " + predicate.target.inferred_type + " on " + predicate.parameters[0].inferred_type + " predicate operations: " + predicate.name
          end
        end

        iff.body.compile(self)

        @method.goto(donelabel)

        elselabel.set!

        iff.else.compile(self) if iff.else

        donelabel.set!
      end
      
      def call(call)
        call_compilers[call.target.inferred_type].call(self, call)
      end
      
      def call_compilers
        @call_compilers ||= {}
      end
      
      def self_call(fcall)
        fcall.parameters.each {|param| param.compile(self)}
        @method.invokestatic(
          @method.this,
          fcall.name,
          [type_mapper[fcall.inferred_type], *fcall.parameters.map {|param| type_mapper[param.inferred_type]}])
      end
      
      def local(name, type)
        case type
        when AST.type(:fixnum)
          @method.iload(@method.local(name))
        else
          @method.aload(@method.local(name))
        end
      end
      
      def fixnum(value)
        @method.push_int(value)
      end
      
      def newline
        # TODO: line numbering
      end
      
      def generate
        @file.generate{|filename, builder| File.open(filename, 'w') {|f| f.write(builder.generate)}}
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
  
  compiler = Duby::Compiler::JVM.new(ARGV[0])
  ast.compile(compiler)
  
  compiler.generate
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
        
        fixnum_type = AST.type(:fixnum)
        
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