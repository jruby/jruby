require 'duby'
require 'duby/jvm/method_lookup'
require 'duby/typer'
require 'duby/plugin/math'
require 'duby/plugin/java'
require 'compiler/builder'

module Duby
  module Compiler
    class JVM
      import java.lang.System
      import java.io.PrintStream
      include Duby::JVM::MethodLookup
      
      class << self
        attr_accessor :verbose

        def log(message)
          puts "* [#{name}] #{message}" if JVM.verbose
        end
      end

      module JVMLogger
        def log(message); JVM.log(message); end
      end
      include JVMLogger
      
      class MathCompiler
        include JVMLogger
        
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
                compiler.mapped_type(call.target.inferred_type),
                call.name,
                [compiler.mapped_type(call.inferred_type), *call.parameters.map {|param| compiler.mapped_type(param.inferred_type)}])
            end
          when AST.type(:long)
            case call.name
            when '-'
              compiler.method.lsub
            when '+'
              compiler.method.ladd
            else
              compiler.method.invokevirtual(
                compiler.mapped_type(call.target.inferred_type),
                call.name,
                [compiler.mapped_type(call.inferred_type), *call.parameters.map {|param| compiler.mapped_type(param.inferred_type)}])
            end
          else
            compiler.method.invokevirtual(
              compiler.mapped_type(call.target.inferred_type),
              call.name,
              [compiler.mapped_type(call.inferred_type), *call.parameters.map {|param| compiler.mapped_type(param.inferred_type)}])
          end
        end
      end

      class InvokeCompiler
        include JVMLogger
        include Duby::JVM::MethodLookup
        
        def call(compiler, call)
          meta = call.target.inferred_type.meta?
          
          mapped_target = compiler.mapped_type(call.target.inferred_type)
          mapped_params = call.parameters.map {|param| compiler.mapped_type(param.inferred_type)}

          raise "Invoke attempted on primitive type: #{call.target.inferred_type}" if (mapped_target.primitive?)

          if meta
            if call.name == 'new'
              # object construction
              constructor = find_method(mapped_target, call.name, mapped_params, meta)
              compiler.method.new mapped_target
              compiler.method.dup
              call.parameters.each {|param| param.compile(compiler)}
              compiler.method.invokespecial(
                mapped_target,
                "<init>",
                [nil, *constructor.parameter_types])
            else
              method = find_method(mapped_target, call.name, mapped_params, meta)
              call.parameters.each {|param| param.compile(compiler)}
              compiler.method.invokestatic(
                mapped_target,
                call.name,
                [compiler.mapped_type(call.inferred_type), *method.parameter_types])
            end
          else
            method = find_method(mapped_target, call.name, mapped_params, meta)
            call.target.compile(compiler)
            call.parameters.each {|param| param.compile(compiler)}
            compiler.method.invokevirtual(
              compiler.mapped_type(call.target.inferred_type),
              call.name,
              [compiler.mapped_type(call.inferred_type), *method.parameter_types])
          end
        end
      end
      
      attr_accessor :filename, :src, :method

      def initialize(filename)
        @filename = filename
        @src = ""

        self.type_mapper[AST.type(:fixnum)] = Java::int.java_class
        self.type_mapper[AST.type(:long)] = Java::long.java_class
        self.type_mapper[AST.type(:string)] = Java::java.lang.String.java_class
        self.type_mapper[AST.type(:string, true)] = Java::java.lang.String[].java_class
        
        self.call_compilers[AST.type(:fixnum)] =
          self.call_compilers[AST.type(:long)] = MathCompiler.new
        self.call_compilers.default = InvokeCompiler.new

        @file = ::Compiler::FileBuilder.new(filename)
        @class = @file.public_class(filename.split('.')[0])
      end

      def compile(ast)
        ast.compile(self)
        log "Compilation successful!"
      end

      def define_main(body)
        oldmethod, @method = @method, @class.public_static_method("main", nil, mapped_type(AST.type(:string, true)))

        log "Starting main method"

        @method.start

        body.compile(self)

        @method.returnvoid
        @method.stop
        
        @method = oldmethod

        log "Main method complete!"
      end
      
      def define_method(name, signature, args, body)
        arg_types = args.args ? args.args.map {|arg| mapped_type(arg.inferred_type)} : []
        oldmethod, @method = @method, @class.public_static_method(name.to_s, mapped_type(signature[:return]), *arg_types)

        log "Starting new method #{name}(#{arg_types})"

        @method.start
        
        body.compile(self)

        case signature[:return]
        when AST.type(:notype)
          @method.returnvoid
        when AST.type(:fixnum)
          @method.ireturn
        else
          @method.aload 0
          @method.areturn
        end
        
        @method.stop

        @method = oldmethod

        log "Method #{name}(#{arg_types}) complete!"
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
          [mapped_type(fcall.inferred_type), *fcall.parameters.map {|param| mapped_type(param.inferred_type)}])
      end
      
      def local(name, type)
        case type
        when AST.type(:fixnum)
          @method.iload(@method.local(name))
        when AST.type(:long)
          @method.lload(@method.local(name))
        else
          @method.aload(@method.local(name))
        end
      end

      def local_assign(name, type)
        yield
        case type
        when AST.type(:fixnum)
          @method.istore(@method.local(name))
        when AST.type(:long)
          @method.lstore(@method.local(name))
        else
          @method.astore(@method.local(name))
        end
      end
      
      def fixnum(value)
        @method.push_int(value)
      end

      def string(value)
        @method.ldc(value)
      end
      
      def newline
        # TODO: line numbering
      end
      
      def generate
        log "Generating classes..."
        @file.generate do |filename, builder|
          log "  #{builder.class_name}"
          if block_given?
            yield filename, builder
          else
            File.open(filename, 'w') {|f| f.write(builder.generate)}
          end
        end
        log "...done!"
      end
      
      def type_mapper
        @type_mapper ||= {}
      end

      def mapped_type(type)
        return nil if type == AST::TypeReference::NoType
        type_mapper[type] || Java::JavaClass.for_name(type.name)
      end

      def import(short, long)
        # TODO hacky..we map both versions because some get expanded during inference
        type_mapper[AST::type(short, false, true)] = Java::JavaClass.for_name(long)
        type_mapper[AST::type(long, false, true)] = Java::JavaClass.for_name(long)
      end

      def println(printline)
        @method.getstatic System, "out", PrintStream
        printline.parameters.each {|param| param.compile(self)}
        mapped_params = printline.parameters.map {|param| mapped_type(param.inferred_type)}
        method = find_method(PrintStream.java_class, "println", mapped_params, false)
        if (method)
          @method.invokevirtual(
            PrintStream,
            "println",
            [method.return_type, *method.parameter_types])
        else
          log "Could not find a match for #{PrintStream}.println(#{mapped_params})"
          fail "Could not compile"
        end
      end
    end
  end
end

if __FILE__ == $0
  Duby::Typer.verbose = true
  Duby::AST.verbose = true
  Duby::Compiler::JVM.verbose = true
  ast = Duby::AST.parse(File.read(ARGV[0]))
  
  typer = Duby::Typer::Simple.new(:script)
  ast.infer(typer)
  typer.resolve(true)
  
  compiler = Duby::Compiler::JVM.new(ARGV[0])
  compiler.compile(ast)
  
  compiler.generate
end
