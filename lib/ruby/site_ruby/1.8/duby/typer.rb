require 'duby/ast'
require 'duby/transform'

module Duby
  module Typer
    class InferenceError < Exception
      attr_accessor :node
      def initialize(msg, node = nil)
        super(msg)
        @node = node
      end
    end
    
    class BaseTyper
      include Duby
      
      def log(message)
        puts "* [#{name}] #{message}" if $DEBUG
      end
    end
    
    class Simple < BaseTyper
      attr_accessor :known_types

      def initialize(self_type)
        @known_types = {}
        
        @known_types[:self] = type_reference(self_type)
        @known_types[:fixnum] = type_reference(:fixnum)
        @known_types[:float] = type_reference(:float)
        @known_types[:string] = type_reference(:string)
        @known_types[:boolean] = type_reference(:boolean)
      end
      
      def name
        "Simple"
      end
      
      def self_type
        known_types[:self]
      end

      def default_type
        nil
      end
      
      def fixnum_type
        known_types[:fixnum]
      end
      
      def float_type
        known_types[:float]
      end
      
      def string_type
        known_types[:string]
      end
      
      def boolean_type
        known_types[:boolean]
      end
      
      def define_type(name, superclass)
        name = name.intern unless Symbol === name
        raise InferenceError.new("Duplicate type definition: #{name} < #{superclass}") if known_types[name]
        
        known_types[name] = AST::TypeDefinition.new(name, AST::TypeReference.new(superclass))
        
        old_self, known_types[:self] = known_types[:self], known_types[name]
        yield
        known_types[:self] = old_self
        
        known_types[name]
      end
      
      def learn_local_type(scope, name, type)
        name = name.intern unless Symbol === name

        log "Learned local type under #{scope} : #{name} = #{type}"

        get_local_type_hash(scope)[name] = type
      end
      
      def local_type(scope, name)
        name = name.intern unless Symbol === name

        log "Retrieved local type in #{scope} : #{name} = #{get_local_type_hash(scope)[name]}"

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

        log "Learned method #{name} (#{parameter_types}) on #{target_type} = #{type}"

        get_method_type_hash(target_type, name, parameter_types)[:type] = type
      end
      
      def method_type(target_type, name, parameter_types)
        name = name.intern unless Symbol === name

        simple_type = get_method_type_hash(target_type, name, parameter_types)[:type]
        
        if !simple_type
          log "Method type for \"#{name}\" #{parameter_types} on #{target_type} not found."
          
          # allow plugins a go
          Duby.typer_plugins.each do |plugin|
            log "Invoking plugin: #{plugin}"
            
            break if simple_type = plugin.method_type(self, target_type, name, parameter_types)
          end
          
        else
          log "Method type for \"#{name}\" #{parameter_types} on #{target_type} = #{simple_type}"
        end
        
        simple_type
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

      def defer(node)
        return if deferred_nodes.include? node
        log "Deferring inference for #{node}"
        
        deferred_nodes << node
      end

      def resolve(raise = false)
        count = deferred_nodes.size + 1
        
        log "Entering type inference cycle"
            
        count.times do |i|
          old_deferred = @deferred_nodes
          @deferred_nodes = deferred_nodes.select do |node|
            type = node.infer(self)

            log "[Cycle #{i}]: Inferred type for #{node}: #{type || 'FAILED'}"

            type == default_type
          end
          
          if @deferred_nodes.size == 0
            log "Inference cycle #{i} resolved all types, exiting"
            break
          elsif old_deferred == @deferred_nodes
            log "Inference cycle #{i} made no progress, bailing out"
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
  
  def self.typer_plugins
    @typer_plugins ||= []
  end
end

if __FILE__ == $0
  ast = Duby::AST.parse(File.read(ARGV[0]))
  typer = Duby::Typer::Simple.new(:script)
  ast.infer(typer)
  begin
    typer.resolve(true)
  rescue Duby::Typer::InferenceError => e
    puts e.message
  end
  
  puts "\nAST:"
  p ast
end