require 'compiler/bytecode'
require 'compiler/signature'
require 'fileutils'

module Compiler
  module Util
    def type_from_dotted(dotted_name)
      JavaUtilities.get_proxy_class(dotted_name).java_class
    end
  end

  module QuickTypes
    def void
      Java::void
    end

    def boolean
      Java::boolean
    end

    def byte
      Java::byte
    end

    def short
      Java::short
    end

    def char
      Java::char
    end

    def int
      Java::int
    end

    def long
      Java::long
    end

    def float
      Java::float
    end

    def double
      Java::double
    end

    def object
      Java::java.lang.Object
    end

    def string
      Java::java.lang.String
    end

    def null
      nil
    end
  end
  
  class FileBuilder
    include Util
    include QuickTypes
    
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

    def self.build(filename, &block)
      fb = new(filename)
      if block_given?
        fb.instance_eval(&block)
      end
      fb
    end
    
    def public_class(class_name, superclass = java.lang.Object, *interfaces, &block)
      class_name = @package.empty? ? class_name : "#{@package.join('/')}/#{class_name}"
      class_builder = ClassBuilder.new(self, class_name, @file_name, superclass, *interfaces)
      @class_builders[class_name] ||= class_builder
      
      if block_given?
        class_builder.instance_eval(&block)
      else
        return class_builder
      end
    end
    
    def generate
      @class_builders.each do |class_name, class_builder|
        class_file = "#{class_name.gsub('.', '/')}.class"
        
        yield class_file, class_builder
      end
    end
    
    def line(line)
      # No tracking of lines at the file level, so we ignore
    end
    
    def package(*names)
      elements = 0
      names.each do |name_maybe_dotted|
        name_maybe_dotted.split(/\./).each do |name|
          elements += 1
          @package.push name
        end
      end
      yield
      elements.times {@package.pop}
    end
    
    def method?
      false
    end
  end
  
  class ClassBuilder
    include Util
    include QuickTypes

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

    def start
    end

    def stop
      # if we haven't seen a constructor, generate a default one
      unless @constructor
        method = MethodBuilder.new(self, Opcodes::ACC_PUBLIC, "<init>", [])
        method.start
        method.aload 0
        method.invokespecial @superclass, "<init>", Void::TYPE
        method.returnvoid
        method.stop
      end
    end
    
    def generate
      String.from_java_bytes(@class_writer.to_byte_array)
    end

    def constructor(*signature, &block)
      method("<init>", *signature, &block)
    end

    %w[public private protected].each do |modifier|
      # instance fields
      eval "
        def #{modifier}_field(name, type)
          field(Opcodes::ACC_#{modifier.upcase}, name, type)
        end
      ", binding, __FILE__, __LINE__
      # static fields
      eval "
        def #{modifier}_static_field(name, type)
          field(Opcodes::ACC_STATIC | Opcodes::ACC_#{modifier.upcase}, name, type)
        end
      ", binding, __FILE__, __LINE__
      # instance methods
      eval "
        def #{modifier}_method(name, *signature, &block)
          method(Opcodes::ACC_#{modifier.upcase}, name, signature, &block)
        end
      ", binding, __FILE__, __LINE__
      # static methods
      eval "
        def #{modifier}_static_method(name, *signature, &block)
          method(Opcodes::ACC_STATIC | Opcodes::ACC_#{modifier.upcase}, name, signature, &block)
        end
      ", binding, __FILE__, __LINE__
      eval "
        def #{modifier}_constructor(*signature, &block)
          method(Opcodes::ACC_#{modifier.upcase}, \"<init>\", [nil, *signature], &block)
        end
      ", binding, __FILE__, __LINE__
    end
    
    def method(flags, name, signature, &block)
      mb = MethodBuilder.new(self, flags, name, signature)

      if block_given?
        mb.start
        mb.instance_eval(&block)
        mb.stop
      end

      mb
    end
    
    def field(flags, name, type)
      @class_writer.visit_field(flags, name, ci(type), nil, nil)
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
  end
  
  class MethodBuilder
    begin
      import "jruby.objectweb.asm.Opcodes"
    rescue
      import "org.objectweb.asm.Opcodes"
    end

    include QuickTypes
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
    
    def local(name)
      if name == "this" && @static
        raise "'this' attempted to load from static method"
      end
      
      if @locals[name]
        local_index = @locals[name]
      else
        local_index = @locals[name] = @locals.size
      end
      local_index
    end
  end
end