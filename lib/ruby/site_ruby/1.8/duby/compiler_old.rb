require 'compiler/builder'
require 'duby/typer'
require 'duby/signature'
require 'duby/mapper'
require 'duby/declaration'
require 'jruby'

# I don't like these at top-level, but reopened Java classes have trouble with const lookup
def log(str)
  puts str if $VERBOSE
end
    
class CompileError < Exception
  def initialize(position, message)
    full_message = "Compile error at #{position.file}:#{position.start_line}: #{message}"
    super(full_message)
  end
end

module Compiler
  module PrimitiveRuby
    JObject = java.lang.Object.java_class
    JClass = java.lang.Class.java_class
    JString = java.lang.String.java_class
    Void = java.lang.Void::TYPE
    System = java.lang.System.java_class
    PrintStream = java.io.PrintStream.java_class
    JInteger = java.lang.Integer.java_class
    Jbyte = Java::byte.java_class
    Jchar = Java::char.java_class
    Jshort = Java::short.java_class
    Jint = Java::int.java_class
    Jlong = Java::long.java_class
    Jfloat = Java::float.java_class
    Jdouble = Java::double.java_class
    Jboolean = Java::boolean.java_class
    JavaClass = Java::JavaClass
    
    # reload 
    module Java::OrgJrubyAst
      class Node
        def compile(builder)
          # default behavior is to raise, to expose missing nodes
          raise CompileError.new(position, "Unsupported syntax: #{self}")
        end
      end
  
      class ArgsNode
        def compile(builder)
          raise("PRuby only supports normal args") if opt_args || rest_arg != -1 || block_arg_node
          return unless args
          args.child_nodes.each do |arg|
            builder.local(arg.name)
          end
        end
      end
  
      class ArrayNode
        def compile(builder)
          # not implemented
          raise
        end
      end
      
      class BeginNode
        def compile(builder)
          body_node.compile(builder)
        end
      end
  
      class BlockNode
        def compile(builder)
          size = child_nodes.size
          if size == 0
            case type
            when Jint
              builder.iconst_0
            else
              builder.aconst_null
            end
          else
            i = 0
            while i < size
              node = child_nodes.get(i)
              node = node.next_node while NewlineNode === node
              next unless node

              builder.line node.position.start_line + 1

              node.compile(builder)
              
              if i + 1 < size
                builder.pop if builder.method?
              end
              
              i += 1
            end
          end
        end
      end
      
      class ClassNode
        def compile(builder)
          cb = builder.public_class(cpath.name)
          body_node.compile(cb)
        end
      end
      
      class CallNode
        def compile(builder)
          receiver_type = receiver_node.type(builder)
          
          if receiver_type.primitive?
            # we're performing an operation against a primitive, map it accordingly
            log "Compiling #{name} at #{position.start_line} as primitive op"
            compile_primitive(receiver_type, builder)
          elsif receiver_type.array?
            log "Compiling #{name} at #{position.start_line} as array op"
            compile_array(receiver_type, builder)
          else
            case name
            when "new"
              log "Compiling #{name} at #{position.start_line} as object instantiation"
              compile_new(receiver_type, builder)
            else
              log "Compiling #{name} at #{position.start_line} as call"
              compile_call(receiver_type, builder)
            end
          end
        end
        
        def compile_call(receiver_type, builder)
          case receiver_node
          when ConstNode
            # static call
            static = true
          else
            receiver_node.compile(builder)
          end

          # I removed this because inference is working...but will it be needed under some circumstances?
#          # inefficient to cast every time; better inference will help
#          builder.checkcast(receiver_type)

          compile_args(builder)

          if static
            builder.invokestatic receiver_type, mapped_name(builder), signature(builder)
          else
            if (receiver_type.interface?)
              builder.invokeinterface receiver_type, mapped_name(builder), signature(builder)
            else
              builder.invokevirtual receiver_type, mapped_name(builder), signature(builder)
            end
          end
        end
        
        def compile_args(builder)
          args_list = args_node.child_nodes.to_a
          args_list.each_index do |idx|
            node = args_list[idx]
            node.compile(builder)
          end
        end
        
        def compile_primitive(type, builder)
          receiver_node.compile(builder)

          if !args_node
            case type
            when Jboolean, Jbyte, Jshort, Jchar
              # TODO: cast and do the same as int
              raise CompileError.new(position, "Unary primitive operations on #{type} not supported")
            when Jint
              case name
              when "-@"
                builder.ineg
              when "+@"
                # do nothing
              else
                raise CompileError.new(position, "Primitive int operation #{name} not supported")
              end
            when Jlong
              case name
              when "-@"
                builder.lneg
              when "+@"
                # do nothing
              else
                raise CompileError.new(position, "Primitive long operation #{name} not supported")
              end
            when Jfloat
              case name
              when "-@"
                builder.fneg
              when "+@"
                # do nothing
              else
                raise CompileError.new(position, "Primitive float operation #{name} not supported")
              end
            when Jdouble
              case name
              when "-@"
                builder.dneg
              when "+@"
                # do nothing
              else
                raise CompileError.new(position, "Primitive double operation #{name} not supported")
              end
            else
              raise CompileError.new(position, "Unary primitive operations on #{type} not supported")
            end
          elsif args_node.size != 1
            raise CompileError.new(position, "Binary primitive operations require exactly one argument")
          else
            node = args_node.get(0)
            # TODO: check or cast types according to receiver's type
            node.compile(builder)

            case type
            when Jboolean, Jbyte, Jshort, Jchar
              # TODO: cast and do the same as int
              raise CompileError.new(position, "Binary primitive operations on #{type} not supported")
            when Jint
              case name
              when "+"
                builder.iadd
              when "-"
                builder.isub
              when "/"
                builder.idiv
              when "*"
                builder.imul
              when "&"
                builder.iand
              when "|"
                builder.ior
              when "^"
                builder.ixor
              else
                raise CompileError.new(position, "Primitive int operation #{name} not supported")
              end
            when Jlong
              case name
              when "+"
                builder.ladd
              when "-"
                builder.lsub
              when "/"
                builder.ldiv
              when "*"
                builder.lmul
              when "&"
                builder.land
              when "|"
                builder.lor
              when "^"
                builder.lxor
              else
                raise CompileError.new(position, "Primitive long operation #{name} not supported")
              end
            when Jfloat
              case name
              when "+"
                builder.fadd
              when "-"
                builder.fsub
              when "/"
                builder.fdiv
              when "*"
                builder.fmul
              else
                raise CompileError.new(position, "Primitive float operation #{name} not supported")
              end
            when Jdouble
              case name
              when "+"
                builder.dadd
              when "-"
                builder.dsub
              when "/"
                builder.ddiv
              when "*"
                builder.dmul
              else
                raise CompileError.new(position, "Primitive double operation #{name} not supported")
              end
            else
              raise CompileError.new(position, "Primitive #{type} operations not supported")
            end
          end
        end
        
        def compile_array(type, builder)
          receiver_node.compile(builder)

          case name
          when "length"
            if args_node
              raise CompileError.new(position, "Array length does not take an argument")
            end
            
            builder.arraylength
          when "[]"
            if !args_node || args_node.size != 1
              raise CompileError.new(position, "Array accessignment must have exactly one argument")
            end

            node = args_node.get(0)
            # TODO: check or cast to int for indexing
            node.compile(builder)
            
            if type.component_type.primitive?
              case type.component_type
              when Jboolean, Jbyte
                builder.baload
              when Jchar
                builder.caload
              when Jshort
                builder.saload
              when Jint
                builder.iaload
              when Jlong
                builder.laload
              when Jfloat
                builder.faload
              when Jdouble
                builder.daload
              end
            else
              builder.aaload
            end
          when "[]="
            if !args_node || args_node.size != 2
              raise CompileError.new(position, "Array assignment must have exactly two arguments")
            end

            # TODO: check or cast to int for indexing
            args_node.get(0).compile(builder)
            # TODO: check type matches?
            args_node.get(1).compile(builder)
            
            builder.aastore
          else
            raise CompileError.new(position, "Array operation #{name} not supported")
          end
        end
        
        def compile_new(type, builder)
          builder.new type
          builder.dup
          
          compile_args(builder)
          
          builder.invokespecial type, mapped_name(builder), signature(builder)
        end
      end
  
      class Colon2Node
      end
      
      class ConstNode
      end
      
      class DefnNode
        def compile(builder)
          first_real_node = body_node
          first_real_node = body_node.child_nodes[0] if BlockNode === body_node
          while NewlineNode === first_real_node
            first_real_node = first_real_node.next_node
          end
          
          # determine signature from declaration line
          signature = first_real_node.signature(builder) if HashNode === first_real_node
          
          signature ||= [Void]
          
          log "Compiling instance method for #{name} as #{signature.join(',')}"
          builder.method(mapped_name(builder), *signature) do |method|
            # Run through any type declarations first
            first_real_node.declare_types(method) if HashNode === first_real_node

            # declare args that may not have been declared already
            args_node.compile(method)
            
            body_node.compile(method) if body_node
            
            # Expectation is that last element leaves the right type on stack
            if signature[0].primitive?
              case signature[0]
              when Void
                method.returnvoid
              when Jboolean, Jbyte
                method.breturn
              when Jchar
                method.creturn
              when Jshort
                method.sreturn
              when Jint
                method.ireturn
              when Jlong
                method.lreturn
              when Jfloat
                method.freturn
              when Jdouble
                method.dreturn
              else
                raise CompileError.new(position, "Unknown return type: #{signature[0]}")
              end
            else
              method.areturn
            end
          end
        end
      end
      
      class DefsNode
        def compile(builder)
          first_real_node = body_node
          first_real_node = body_node.child_nodes[0] if BlockNode === body_node
          while NewlineNode === first_real_node
            first_real_node = first_real_node.next_node
          end
          
          # determine signature from declaration line
          signature = first_real_node.signature(builder) if HashNode === first_real_node
          
          signature ||= [Void]
          
          log "Compiling static method for #{name} as #{signature.join(',')}"
          builder.static_method(name, *signature) do |method|
            # Run through any type declarations first
            first_real_node.declare_types(method) if HashNode === first_real_node

            # declare args that may not have been declared already
            args_node.compile(method)
            
            body_node.compile(method) if body_node
            
            # Expectation is that last element leaves the right type on stack
            if signature[0].primitive?
              case signature[0]
              when Void
                method.returnvoid
              when Jboolean, Jbyte
                method.breturn
              when Jchar
                method.creturn
              when Jshort
                method.sreturn
              when Jint
                method.ireturn
              when Jlong
                method.lreturn
              when Jfloat
                method.freturn
              when Jdouble
                method.dreturn
              else
                raise CompileError.new(position, "Unknown return type: #{signature[0]}")
              end
            else
              method.areturn
            end
          end
        end
      end
  
      class FCallNode
        def compile(builder)
          case name
          when "puts"
            compile_puts(builder)
          when "import"
            compile_import(builder)
          else
            if (builder.static)
              arg_types = []
              args_node.child_nodes.each do |node|
                node.compile(builder)
                arg_types << node.type(builder)
              end

              builder.invokestatic builder.this, name, builder.static_signature(name, arg_types)
            else
              builder.aload 0
              arg_types = []
              args_node.child_nodes.each do |node|
                node.compile(builder)
                arg_types << node.type(builder)
              end

              builder.invokevirtual builder.this, name, builder.instance_signature(name, arg_types)
            end
          end
        end
        
        def compile_puts(builder)
          log "Compiling special #{name} at #{position.start_line}"
          builder.getstatic System, "out", [PrintStream]

          arg_types = []
          args_node.child_nodes.each do |node|
            node.compile(builder)
            arg_types << node.type(builder)
          end

          builder.invokevirtual PrintStream, "println", special_signature(PrintStream, builder)
          builder.aconst_null
        end
        
        def compile_import(builder)
          log "Compiling import at #{position.start_line}"
          args_node.child_nodes.each do |node|
            case node
            when StrNode
              builder.import(node.value)
            else
              raise CompileError.new(position, "Imports only allow strings right now")
            end
          end
        end
      end
      
      class FixnumNode
        def compile(builder)
          builder.ldc_int(value)
        end
      end
      
      class FloatNode
        def compile(builder)
          builder.ldc_float(value)
        end
      end
      
      class HashNode
        def compile(builder)
          @declared ||= false
          
          if @declared
            # hash was used for type declaration, so we just push a null to skip it
            # TODO: it would be nice if we could just skip the null too, but BlockNode wants to pop it
            builder.aconst_null
          else
            raise CompileError.new(position, "Literal hash syntax not yet supported")
          end
        end
      end
      
      class IfNode
        def compile(builder)
          else_lbl = builder.label
          done = builder.label
          condition = self.condition
          condition = condition.next_node while NewlineNode === condition
          
          case condition
          when CallNode
            args = condition.args_node
            receiver_type = condition.receiver_node.type(builder)
            
            if receiver_type.primitive?
              case condition.name
              when "<"
                raise CompileError.new(position, "Primitive < must have exactly one argument") if !args || args.size != 1

                condition.receiver_node.compile(builder)
                args.get(0).compile(builder)

                # >= is else for <
                case receiver_type
                when Jint
                  builder.if_icmpge(else_lbl)
                else
                  raise CompileError.new(position, "Primitive < is only supported for int")
                end
              when ">"
                raise CompileError.new(position, "Primitive > must have exactly one argument") if !args || args.size != 1

                condition.receiver_node.compile(builder)
                args.get(0).compile(builder)

                # <= is else for >
                case receiver_type
                when Jint
                  builder.if_icmple(else_lbl)
                else
                  raise CompileError.new(position, "Primitive > is only supported for int")
                end
              when "=="
                raise CompileError.new(position, "Primitive == must have exactly one argument") if !args || args.size != 1

                condition.receiver_node.compile(builder)
                args.get(0).compile(builder)

                # ne is else for ==
                case receiver_type
                when Jint
                  builder.if_icmpne(else_lbl)
                else
                  raise CompileError.new(position, "Primitive == is only supported for int")
                end
              else
                raise CompileError.new(position, "Conditional not supported: #{condition.inspect}")
              end
              
              then_body.compile(builder)
              builder.goto(done)

              else_lbl.set!
              else_body.compile(builder)

              done.set!
            else
              raise CompileError.new(position, "Conditional on non-primitives not supported: #{condition.inspect}")
            end
          else
            raise CompileError.new(position, "Non-call conditional not supported: #{condition.inspect}")
          end
        end
      end
      
      class InstAsgnNode
        def compile(builder)
          builder.field(mapped_name(builder), value_node.type(builder))
          
          # assignment consumes the value, so we dup it
          # TODO inefficient if we don't need the result
          value_node.compile(builder)
          builder.dup
          
          builder.putfield(mapped_name(builder))
        end
      end
      
      class InstVarNode
        def compile(builder)
          builder.getfield(mapped_name(builder))
        end
      end
      
      class LocalAsgnNode
        def compile(builder)
          local_index = builder.local(name, value_node.type(builder))
          
          # assignment consumes a value, so we dup it
          # TODO: inefficient if we don't actually need the result
          value_node.compile(builder)
          builder.dup
          
          case type(builder)
          when Jboolean
            builder.bistore(local_index)
          when Jint
            builder.istore(local_index)
          when Jlong
            builder.lstore(local_index)
          when Jfloat
            builder.fstore(local_index)
          when Jdouble
            builder.dstore(local_index)
          else
            builder.astore(local_index)
          end
        end
      end
      
      class LocalVarNode
        def compile(builder)
          local_index = builder.local(name)
          case type(builder)
          when Jboolean
            builder.biload(local_index)
          when Jint
            builder.iload(local_index)
          when Jlong
            builder.lload(local_index)
          when Jfloat
            builder.fload(local_index)
          when Jdouble
            builder.dload(local_index)
          else
            builder.aload(local_index)
          end
        end
      end
      
      class ModuleNode
        def compile(builder)
          builder.package(cpath.name) {
            body_node.compile(builder)
          }
        end
      end
  
      class NewlineNode
        def compile(builder)
          builder.line position.start_line
          next_node.compile(builder)
        end
      end
      
      class ReturnNode
        def compile(builder)
          value_node.compile(builder)
          builder.areturn
        end
      end
  
      class RootNode
        def compile(builder)
          # builder is class builder
      
          if body_node
            body_node.compile(builder)
          end
        end
      end
      
      class SelfNode
        def compile(builder)
          builder.local("this")
        end
      end
  
      class StrNode
        def compile(builder)
          builder.ldc value
        end
      end
      
      class SymbolNode
      end
  
      class VCallNode
        def compile(builder)
          if builder.static
            builder.invokestatic builder.this, name, builder.static_signature(name, [])
          else
            builder.aload 0

            builder.invokevirtual builder.this, name, builder.instance_signature(name, [])
          end
        end
      end
      
      class WhileNode
        def compile(builder)
          begin_lbl = builder.label
          end_lbl = builder.label
          cond_lbl = builder.label
          
          case body_node.type(builder)
          when Jint
            builder.iconst_0
          else
            builder.aconst_null
          end
          
          if evaluate_at_start
            builder.goto cond_lbl
          end
          
          begin_lbl.set!
          builder.pop
          body_node.compile(builder)
          
          cond_lbl.set!
          compile_condition(builder, begin_lbl)
          end_lbl.set!
        end
        
        def compile_condition(builder, begin_lbl)
          condition = condition_node
          condition = condition.next_node while NewlineNode === condition
          
          case condition
          when CallNode
            args = condition.args_node
            receiver_type = condition.receiver_node.type(builder)
            
            if receiver_type.primitive?
              case condition.name
              when "<"
                raise CompileError.new(position, "Primitive < must have exactly one argument") if !args || args.size != 1

                condition.receiver_node.compile(builder)
                args.get(0).compile(builder)

                case receiver_type
                when Jint
                  builder.if_icmplt(begin_lbl)
                else
                  raise CompileError.new(position, "Primitive < is only supported for int")
                end
              when ">"
                raise CompileError.new(position, "Primitive > must have exactly one argument") if !args || args.size != 1

                condition.receiver_node.compile(builder)
                args.get(0).compile(builder)

                case receiver_type
                when Jint
                  builder.if_icmpgt(begin_lbl)
                else
                  raise CompileError.new(position, "Primitive < is only supported for int")
                end
              else
                raise CompileError.new(position, "Conditional not supported: #{condition.inspect}")
              end
            else
              raise CompileError.new(position, "Conditional on non-primitives not supported: #{condition.inspect}")
            end
          else
            raise CompileError.new(position, "Non-call conditional not supported: #{condition.inspect}")
          end
        end
      end
    end
  end
end

if $0 == __FILE__
  n = JRuby.parse(File.read(ARGV[0]), ARGV[0])
  compiler = Compiler::FileBuilder.new(ARGV[0])
  begin
    n.compile(compiler)

    compiler.generate do |filename, builder|
      puts "Compiling #{builder.class_name.gsub('/', '.')}.class"
      
      class_name = builder.class_name
      if class_name.rindex('/')
        dir = class_name[0..class_name.rindex('/')]
        FileUtils.mkdir_p(dir)
      end
      
      File.open(filename, 'w') {|file| file.write(builder.generate)}
    end
  rescue CompileError => e
    puts e
    puts e.backtrace
  end
end