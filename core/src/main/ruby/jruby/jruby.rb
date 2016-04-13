module JRuby
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
      current = JRuby.runtime
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

    # Change the current threads context classloader.  By, default call
    # with no arguments to replace it with JRuby's class loader.
    def set_context_class_loader(loader = JRuby.runtime.jruby_class_loader)
      java.lang.Thread.currentThread.setContextClassLoader loader
    end

    DEFAULT_FILENAME = '-'.dup; private_constant :DEFAULT_FILENAME

    # Parse the given block or the provided content, returning a JRuby AST node.
    def parse(content = nil, filename = DEFAULT_FILENAME, extra_position_info = false, lineno = 0, &block)
      if block
        block_r = reference0(block)
        body = block_r.body

        if org.jruby.runtime.CompiledBlock === body
          raise ArgumentError, "cannot get parse tree from compiled block"
        end

        body.body_node
      else
        content = content.to_str
        filename = filename.to_str unless filename.equal?(DEFAULT_FILENAME)

        signature = [org.jruby.util.ByteList, java.lang.String, org.jruby.runtime.DynamicScope, Java::int, Java::boolean]
        runtime.java_send :parse, signature, reference0(content).byte_list, filename, nil, lineno, extra_position_info
      end
    end
    alias ast_for parse

    def compile_ir(content = nil, filename = DEFAULT_FILENAME, extra_position_info = false, &block)
      runtime = JRuby.runtime
      manager = org.jruby.ir.IRManager.new(runtime.instance_config)
      manager.dry_run = true
      if filename.equal?(DEFAULT_FILENAME)
        node = parse(content, &block)
      else
        node = parse(content, filename, extra_position_info, &block)
      end

      scope = org.jruby.ir.IRBuilder.build_root(manager, node).scope
      scope.top_level_binding_scope = node.scope

      scope
    end

    # Parse and compile the given block or provided content, returning a new
    # CompiledScript instance.
    def compile(content = nil, filename = DEFAULT_FILENAME, extra_position_info = false, &block)
      irscope = compile_ir(content, filename, extra_position_info, &block)

      visitor = org.jruby.ir.targets.JVMVisitor.new
      context = org.jruby.ir.targets.JVMVisitorMethodContext.new
      bytes = visitor.compile_to_bytecode(irscope, context)
      static_scope = irscope.static_scope;
      top_self = JRuby.runtime.top_self
      static_scope.module = top_self.class

      CompiledScript.new(filename, irscope.name, content, bytes)
    end
  end

  # NOTE: This is not a public API and is subject to change at our whim
  # @private
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

    attr_reader :name, :class_name, :original_script, :code

    # @private
    def initialize(filename, class_name, content, bytes)
      @name = filename
      @class_name = class_name
      @original_script = content
      @code = bytes
    end

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

  # @private
  def self.init_asm
    return if const_defined? :TraceClassVisitor
    begin
      const_set(:TraceClassVisitor, org.jruby.org.objectweb.asm.util.TraceClassVisitor)
      const_set(:ClassReader, org.jruby.org.objectweb.asm.ClassReader)
    rescue
      const_set(:TraceClassVisitor, org.objectweb.asm.util.TraceClassVisitor)
      const_set(:ClassReader, org.objectweb.asm.ClassReader)
    end
  end

end
