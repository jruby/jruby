module JRuby
  class << self
    # Get a Java integration reference to the given (Ruby) object.
    # @note implemented in *org.jruby.ext.jruby.JRubyLibrary*
    def reference(obj); end if false

    # Turn a Java integration reference (to a Ruby object) back into a normal Ruby object reference.
    # @note implemented in *org.jruby.ext.jruby.JRubyLibrary*
    def dereference(obj); end if false

    # Get the current JRuby runtime.
    # @note implemented in *org.jruby.ext.jruby.JRubyLibrary*
    def runtime; end if false

    # Provide the "identity" hash for an object (that `System.identityHashCode` would produce).
    # @note implemented in *org.jruby.ext.jruby.JRubyLibrary*
    def identity_hash(obj); end if false

    # Run the provided (required) block with the "global runtime" set to the current runtime,
    # for libraries that expect to operate against the global runtime.
    # @note Mostly meant for dealing with legacy JRuby extensions.
    def with_current_runtime_as_global; end if false

    # Change the current threads context classloader.
    # By, default call with no arguments to replace it with JRuby's class loader.
    # @note implemented in *org.jruby.ext.jruby.JRubyLibrary*
    def set_context_class_loader(loader = nil); end if false

    # Parse the given block or the provided content, returning a JRuby AST node.
    # @note implemented in *org.jruby.ext.jruby.JRubyLibrary*
    def parse(content = nil, filename = DEFAULT_FILENAME, extra_position_info = false, lineno = 0); end if false

    # @note implemented in *org.jruby.ext.jruby.JRubyLibrary*
    def compile_ir(content = nil, filename = DEFAULT_FILENAME, extra_position_info = false, &block); end if false

    # Parse and compile the given block or provided content.
    # @return [JRuby::CompiledScript]
    # @note implemented in *org.jruby.ext.jruby.JRubyLibrary*
    def compile(content, filename = '', extra_position_info = false); end if false

    # Get all known subclasses of passed class.
    # If recurse: true, include all (non-direct) descendants recursively.
    # @return Enumerable[Class]
    def subclasses_of(klass, recurse: false) end if false

  end

  # NOTE: This is not a public API and is subject to change at our whim.
  # This is used by our AST tool.
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
  end
  deprecate_constant :IR

  # Helper struct returned from `JRuby.compile`.
  # @see JRuby#compile
  class CompiledScript

    attr_reader :name, :class_name, :original_script, :code

    # @private
    def initialize(filename, class_name, content, bytes)
      @name = filename
      @class_name = class_name
      @original_script = content
      @code = bytes
    end

    # Returns the original (.rb script content's
    def to_s
      @original_script
    end

    def inspect
      "\#<#{self.class.name} #{@name}>"
    end

    # Inspects the compiled (Java) byte-code.
    def inspect_bytecode
      writer = java.io.StringWriter.new
      reader = JRuby::ASM::ClassReader.new(@code)
      tracer = JRuby::ASM::TraceClassVisitor.new(java.io.PrintWriter.new(writer))

      reader.accept(tracer, JRuby::ASM::ClassReader::SKIP_DEBUG)

      writer.to_s
    end

  end

  autoload :ASM, 'org/jruby/kernel/asm.rb'

end
