require 'compiler/duby/ast'
require 'compiler/duby/transform'

module Compiler::Duby
  module Typer
    class InferenceError < Exception
      attr_accessor :node
      def initialize(msg, node = nil)
        super(msg)
        @node = node
      end
    end
    
    class Simple
      include Compiler::Duby
      attr_accessor :self_type

      def initialize(self_type)
        @self_type = type_reference(self_type)
      end

      def default_type
        nil
      end
      def fixnum_type
        AST::TypeReference.new :fixnum
      end
      def float_type
        AST::TypeReference.new :float
      end
      def string_type
        AST::TypeReference.new :string
      end
      def learn_local_type(scope, name, type)
        name = name.intern unless Symbol === name

        puts "* Learned local type in #{scope} : #{name} = #{type}" if $DEBUG

        get_local_type_hash(scope)[name] = type
      end
      def local_type(scope, name)
        name = name.intern unless Symbol === name

        puts "* Retrieved local type in #{scope} : #{name} = #{get_local_type_hash(scope)[name]}" if $DEBUG

        get_local_type_hash(scope)[name]
      end
      def local_types
        @local_types ||= {}
      end
      def get_local_type_hash(scope)
        local_types[scope] ||= {}
      end
      def learn_method_type(target_type, name, parameter_types, type)
        name = name.intern unless Symbol === name

        puts "* Learned method: #{target_type} : #{name} : (#{parameter_types.join(',')}) = #{type}" if $DEBUG

        get_method_type_hash(target_type, name, parameter_types)[:type] = type
      end
      def method_type(target_type, name, parameter_types)
        name = name.intern unless Symbol === name

        get_method_type_hash(target_type, name, parameter_types)[:type]
      end
      def method_types
        @method_types ||= {}
      end
      def get_method_type_hash(target_type, name, parameter_types)
        method_types[target_type] ||= {}
        method_types[target_type][name] ||= {}
        method_types[target_type][name][parameter_types.size] ||= {}
        
        current = method_types[target_type][name][parameter_types.size]

        parameter_types.each {|type| current[type] ||= {}; current = current[type]}

        current
      end
      def type_reference(name)
        AST::TypeReference.new(name)
      end

      def deferred_nodes
        @deferred_nodes ||= []
      end

      def defer_inference(node)
        return if deferred_nodes.include? node
        puts "* Deferring inference for #{node}" if $DEBUG
        deferred_nodes << node
      end

      def resolve_deferred(raise = false)
        count = deferred_nodes.size + 1
        count.times do |i|
          old_deferred = @deferred_nodes
          @deferred_nodes = deferred_nodes.select do |node|
            type = node.infer_type(self)

            puts "* [Cycle #{i}]: Inferred type for #{node}: #{type || 'FAILED'}" if $DEBUG

            type == default_type
          end
          
          if @deferred_nodes.size == 0
            puts "* Inference cycle #{i} resolved all types, exiting" if $DEBUG
            break
          elsif old_deferred == @deferred_nodes
            puts "* Inference cycle #{i} made no progress, bailing out" if $DEBUG
            break
          end
        end

        # done with n sweeps, if any remain raise errors
        if raise && !deferred_nodes.empty?
          raise InferenceError.new("Could not infer typing for the following nodes:\n  " + deferred_nodes.map {|e| "#{e} (child of #{e.parent})"}.join("\n  "))
        end
      end
    end
  end
  InferenceError = Typer::InferenceError
  SimpleTyper = Typer::Simple
end

if __FILE__ == $0
  ast = Compiler::Duby::AST.parse(File.read(ARGV[0]))
  typer = Compiler::Duby::SimpleTyper.new(:script)
  ast.infer(typer)
  typer.resolve_deferred(true)
  
  p ast
end