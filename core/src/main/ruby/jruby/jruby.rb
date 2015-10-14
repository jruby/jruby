require 'java'

module JRuby
  def self.init_asm
    begin
      JRuby.const_set(:TraceClassVisitor, org.jruby.org.objectweb.asm.util.TraceClassVisitor)
      JRuby.const_set(:ClassReader, org.jruby.org.objectweb.asm.ClassReader)
    rescue
      JRuby.const_set(:TraceClassVisitor, org.objectweb.asm.util.TraceClassVisitor)
      JRuby.const_set(:ClassReader, org.objectweb.asm.ClassReader)
    end
  end
  
  class << self
    # Get a Java integration reference to the given object
    def reference(obj); end

    # Turn a Java integration reference to a Ruby object back into a normal Ruby
    # object reference.
    def dereference(obj); end

    # Get the current JRuby runtime.
    def runtime
      # reference nil, since it is guaranteed to be a normal object
      reference0(nil).runtime
    end

    # Run the provided (required) block with the "global runtime" set to the
    # current runtime, for libraries that expect to operate against the global
    # runtime.
    def with_current_runtime_as_global
      current = runtime
      global = org.jruby.Ruby.global_runtime

      begin
        if current != global
          current.use_as_global_runtime
        end
        yield
      ensure
        if org.jruby.Ruby.global_runtime != global
          global.use_as_global_runtime
        end
      end
    end

    # Parse the given block or the provided content, returning a JRuby AST node.
    def parse(content = nil, filename = (default_filename = true; '-'), extra_position_info = false, &block)
      if block
        block_r = reference0(block)
        body = block_r.body

        if org.jruby.runtime.CompiledBlock === body
          raise ArgumentError, "cannot get parse tree from compiled block"
        end

        body.body_node
      else
        content = content.to_str
        filename = filename.to_str unless default_filename

        runtime.parse(reference0(content).byte_list, filename, nil, 0, extra_position_info)
      end
    end
    alias ast_for parse

    # Parse and compile the given block or provided content, returning a new
    # CompiledScript instance.
    def compile(content = nil, filename = (default_filename = true; '-'), extra_position_info = false, &block)
      node = if default_filename
        parse(content, &block)
      else
        parse(content, filename, extra_position_info, &block)
      end
      
      content = content.to_str
      filename = filename.to_str unless default_filename

      if filename == "-e"
        classname = "__dash_e__"
      else
        classname = filename.gsub(/\\/, '/')
        classname.gsub!(/\.rb/, '')
        classname.gsub!(/-/, 'dash')
      end

      inspector = org.jruby.compiler.ASTInspector.new
      inspector.inspect(node)

      generator = org.jruby.compiler.impl.StandardASMCompiler.new(classname, filename)

      compiler = runtime.instance_config.new_compiler
      compiler.compile_root(node, generator, inspector)

      bytes = generator.class_byte_array

      script = CompiledScript.new
      script.name = filename
      script.class_name = classname
      script.original_script = content
      script.code = bytes

      script
    end
  end

  # NOTE: This is not a public API and is subject to change at our whim
  module IR
    def self.debug=(value)
      org.jruby.RubyInstanceConfig.IR_DEBUG = !!value
    end

    def self.debug
      org.jruby.RubyInstanceConfig.IR_DEBUG
    end

    def self.compiler_debug=(value)
      org.jruby.RubyInstanceConfig.IR_COMPILER_DEBUG = !!value
    end

    def self.compiler_debug
      org.jruby.RubyInstanceConfig.IR_COMPILER_DEBUG
    end

    def self.visualize=(value)
      org.jruby.RubyInstanceConfig.IR_VISUALIZER = !!value
    end

    def self.visualize
      org.jruby.RubyInstanceConfig.IR_VISUALIZER
    end
  end
  
  class CompiledScript
    attr_accessor :name, :class_name, :original_script, :code
    
    def to_s
      @original_script
    end
    
    def inspect
      "\#<JRuby::CompiledScript #{@name}>"
    end
    
    def inspect_bytecode
      JRuby.init_asm
      writer = java.io.StringWriter.new
      reader = ClassReader.new(@code)
      tracer = TraceClassVisitor.new(java.io.PrintWriter.new(writer))
      
      reader.accept(tracer, ClassReader::SKIP_DEBUG)
      
      writer.to_s
    end
  end
end
