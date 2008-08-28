require 'compiler/bytecode'
require 'compiler/signature'
require 'fileutils'

module Compiler
  module Util
    def type_from_dotted(dotted_name)
      JavaUtilities.get_proxy_class(dotted_name).java_class
    end
  end
  
  module TypeNamespace
    include Util
    
    def import(strcls)
      bits = strcls.split(".")
      log "Importing #{type_from_dotted(strcls)} as #{bits[-1]}" 
      @imports[bits[-1]] = type_from_dotted(strcls)
    end

    def type(sym)
      sym = sym.to_s
      type = @imports[sym]
      
      return type if type
      return @parent.type(sym) if @parent
      return type_from_dotted(sym)
    end
  end
    
  class TypedVariable
    attr_accessor :type
    attr_accessor :name
    attr_accessor :index

    def initialize(name, type, index = 0)
      @name = name
      @type = type || java.lang.Object.java_class
      @index = index
    end

    def learn(type)
      # TODO ensure new type is compatible with previous type or raise
      @type ||= type || java.lang.Object.java_class
    end
  end
  
  class FileBuilder
    include Util
    include TypeNamespace
    
    attr_accessor :file_name
    attr_accessor :class_builders
    attr_accessor :imports
    attr_accessor :package
    
    def initialize(file_name)
      @file_name = file_name
      @class_builders = {}
      @imports = {}
      @package = []
      
      init_imports
    end
    
    def init_imports
      # set up a few useful imports
      @imports[:int.to_s] = Java::int.java_class
      @imports[:string.to_s] = Java::java.lang.String.java_class
      @imports[:object.to_s] = Java::java.lang.Object.java_class
    end
    
    def public_class(class_name, superclass = java.lang.Object, *interfaces)
      class_name = @package.empty? ? class_name : "#{@package.join('/')}/#{class_name}"
      @class_builders[class_name] ||= ClassBuilder.new(self, class_name, @file_name, superclass, *interfaces)
      
      if block_given?
        yield @class_builders[class_name]
      else
        return @class_builders[class_name]
      end
    end
    
    def generate
      @class_builders.each do |class_name, class_builder|
        class_file = "#{class_name}.class"
        
        yield class_file, class_builder
      end
    end
    
    def line(line)
      # No tracking of lines at the file level, so we ignore
    end
    
    def package(name)
      name = name.dup
      # flip case of first char (obviously not unicode aware...)
      name[0] += 32
      @package.push(name)
      yield
      @package.pop
    end
    
    def method?
      false
    end
  end
  
  class ClassBuilder
    include Util
    include TypeNamespace

    begin
      import "jruby.objectweb.asm.Opcodes"
      import "jruby.objectweb.asm.ClassWriter"
    rescue
      import "org.objectweb.asm.Opcodes"
      import "org.objectweb.asm.ClassWriter"
    end
    
    import java.lang.Object
    import java.lang.Void
    include Signature
    
    attr_accessor :class_name
    attr_accessor :superclass
    attr_accessor :constructor
    attr_accessor :instance_methods
    attr_accessor :static_methods
    attr_accessor :imports
    attr_accessor :fields

    def initialize(file_builder, class_name, file_name, superclass = Object, *interfaces)
      @parent = file_builder
      @class_name = class_name
      @superclass = superclass 
      
      @class_writer = ClassWriter.new(ClassWriter::COMPUTE_MAXS)
      
      interface_paths = []
      interfaces.each {|interface| interface_paths << path(interface)}
      @class_writer.visit(Opcodes::V1_4, Opcodes::ACC_PUBLIC | Opcodes::ACC_SUPER, class_name, nil, path(superclass), interface_paths.to_java(:string))
      @class_writer.visit_source(file_name, nil)
      
      @constructor = nil
      @instance_methods = {}
      @static_methods = {}
      
      @imports = {}
      
      @fields = {}
    end
    
    def self.build(class_name, file_name, superclass = java.lang.Object, *interfaces, &block)
      fb = FileBuilder.new(file_name)
      cb = fb.public_class(class_name, superclass, *interfaces)
      cb.instance_eval(&block)
      cb.generate
    end
    
    def generate
      if @constructor
        @constructor.generate_constructor(@superclass)
      else
        method = MethodBuilder.new(self, Opcodes::ACC_PUBLIC, "<init>", [])
        method.start
        method.aload 0
        method.invokespecial @superclass, "<init>", Void::TYPE
        method.returnvoid
        method.stop
      end
      
      # lazily generate the actual methods
      @instance_methods.each do |name, deferred_method_builder|
        deferred_method_builder.generate
      end
      @static_methods.each do |name, deferred_method_builder|
        deferred_method_builder.generate
      end
      
      # generate fields
      @fields.each do |name, field|
        @class_writer.visit_field(Opcodes::ACC_PROTECTED, field.name, class_id(field.type), nil, nil)
      end
      
      String.from_java_bytes(@class_writer.to_byte_array)
    end
    
    class DeferredMethodBuilder
      attr_accessor :signature
      
      def initialize(name, method_builder, signature, block)
        @name = name
        @method_builder = method_builder
        @signature = signature
        @block = block
      end
      
      def generate
        @method_builder.start
        @block.call(@method_builder)
        @method_builder.stop
      end
      
      def generate_constructor(superclass)
        @method_builder.start
        
        # FIXME: this could be a lot nicer
        if (@name == "<init>")
          @method_builder.aload(0)
          @method_builder.invokespecial superclass, "<init>", Void::TYPE
        end
        
        @block.call(@method_builder)
        @method_builder.stop
      end
    end
    
    # New version does not instance_eval, to allow for easier embedding
    def method(name, *signature, &block)
      if @constructor && name == "<init>"
        raise "Overloading not yet supported"
      end

      mb = MethodBuilder.new(self, Opcodes::ACC_PUBLIC, name, signature)
      deferred_builder = DeferredMethodBuilder.new(name, mb, signature, block)
      if name == "<init>"
        @constructor = deferred_builder
      else
        @instance_methods[name] = deferred_builder
      end

      mb
    end
    
    # New version does not instance_eval, to allow for easier embedding
    def static_method(name, *signature, &block)
      mb = MethodBuilder.new(self, Opcodes::ACC_PUBLIC | Opcodes::ACC_STATIC, name, signature)
#      deferred_builder = DeferredMethodBuilder.new(name, mb, signature, block)
#      @static_methods[name] = deferred_builder

      if block
        mb.start
        block.yield(mb)
        mb.stop
      else
        mb
      end
    end
    
    # name for signature generation using the class being generated
    def name
      @class_name
    end
    
    # never generating an array
    def array?
      false
    end
    
    # never generating a primitive
    def primitive?
      false
    end
    
    def this
      self
    end
    
    def new_method(modifiers, name, signature)
      @class_writer.visit_method(modifiers, name, sig(*signature), nil, nil)
    end
    
    def line(num)
      # class bodies don't track line numbers in Java, so we ignore
    end
    
    def static_signature(name, arg_types)
      if @static_methods[name]
        @static_methods[name].signature
      else
        find_super_static_signature(name, arg_types)
      end
    end
    
    def instance_signature(name, arg_types)
      # TODO: allow overloading?
      if @instance_methods[name]
        @instance_methods[name].signature
      else
        find_super_instance_signature(name, arg_types)
      end
    end
    
    def find_super_instance_signature(name, arg_types)
      # TODO: implement by searching parent
      nil
    end
    
    def find_super_static_signature(name, arg_types)
      # TODO: implement by searching parent
      nil
    end
    
    def field(name, type = nil)
      if type
        # declaring
        if @fields[name]
          # TODO ensure new type fits existing inferred type
          field = @fields[name]
          field.learn(type) if type
        else
          field = TypedVariable.new(name, type)
          @fields[name] = field
        end
      else
        field = @fields[name]
        
        raise "Field accessed before initialized" unless field
      end
      
      field
    end
    
    def field_type(name)
      @fields[name].type
    end
    
    def method?
      false
    end
  end
  
  class MethodBuilder
    begin
      import "jruby.objectweb.asm.Opcodes"
    rescue
      import "org.objectweb.asm.Opcodes"
    end
    
    include Compiler::Bytecode
    
    attr_reader :method_visitor
    attr_reader :static
    
    def initialize(class_builder, modifiers, name, signature)
      @class_builder = class_builder
      @modifiers = modifiers
      @name = name
      @signature = signature
      
      @method_visitor = class_builder.new_method(modifiers, name, signature)
      
      @locals = {}
      
      @static = (modifiers & Opcodes::ACC_STATIC) != 0
      
      @locals['this'] = TypedVariable.new("this", @class_builder, 0) unless @static
    end
    
    def self.build(class_builder, modifiers, name, signature, &block)
      mb = MethodBuilder.new(class_builder, modifiers, name, signature)
      mb.start
      mb.instance_eval(&block)
      mb.stop
    end
    
    def self.build2(class_builder, modifiers, name, signature, &block)
      mb = MethodBuilder.new(class_builder, modifiers, name, signature)
      mb.start
      block.call(mb)
      mb.stop
    end
    
    def generate(&block)
      start
      block.call(self)
      stop
    end
    
    def this
      @class_builder
    end
    
    def local(name, type = nil)
      if name == "this" && @static
        raise "'this' attempted to load from static method"
      end
      
      if @locals[name]
        # TODO ensure new type fits existing inferred type
        local = @locals[name]
        local.learn(type) if type
      else
        local = TypedVariable.new(name, type, @locals.size)
        @locals[name] = local
      end
      local.index
    end
    
    def local_type(name)
      @locals[name].type
    end
    
    def field(*args)
      @class_builder.field(*args)
    end
    
    def field_type(name)
      @class_builder.field(name).type
    end
    
    def getfield(name)
      field = @class_builder.field(name)
      aload(0)
      super(@class_builder, field.name, [field.type])
    end
    
    def putfield(name)
      field = @class_builder.field(name)
      aload(0)
      swap
      super(@class_builder, field.name, [field.type])
    end
    
    def instance_signature(name, arg_types)
      @class_builder.instance_signature(name, arg_types) || @class_builder.static_signature(name, arg_types)
    end
    
    def static_signature(name, arg_types)
      @class_builder.static_signature(name, arg_types)
    end
    
    def type(sym)
      @class_builder.type(sym)
    end
    
    def method?
      true
    end
  end
end