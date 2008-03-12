require 'compiler/bytecode'
require 'compiler/signature'
require 'fileutils'

module Compiler
  module Util
    def type_from_dotted(dotted_name)
      JavaUtilities.get_proxy_class(dotted_name).java_class
    end
  end
  
  class FileBuilder
    include Util
    
    def initialize(file_name)
      @file_name = file_name
      @class_builders = {}
      @imports = {}
      @package = []
      
      init_imports
    end
    
    def init_imports
      # set up a few useful imports
      @imports[:int] = Java::int.java_class
      @imports[:string] = Java::java.lang.String.java_class
      @imports[:object] = Java::java.lang.Object.java_class
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
        if class_name.rindex('/')
          dir = class_name[0..class_name.rindex('/')]
          FileUtils.mkdir_p(dir)
        end
        class_file = "#{class_name}.class"
        puts "Writing class #{class_name.gsub('/', '.')}"
        File.open(class_file, 'w') {|file| file.write(class_builder.generate)}
      end
    end
    
    def line(line)
      # No tracking of lines at the file level, so we ignore
    end
    
    def import(strcls)
      # TODO: very simple
      bits = strcls.split(".")
      @imports[bits[-1].to_sym] = type_from_dotted(strcls)
    end
    
    def type(sym)
      @imports[sym] || type_from_dotted(sym)
    end
    
    def package(name)
      name = name.dup
      # flip case of first char (obviously not unicode aware...)
      name[0] += 32
      @package.push(name)
      yield
      @package.pop
    end
  end
  
  class ClassBuilder
    include Util
    
    import "jruby.objectweb.asm.Opcodes"
    import "jruby.objectweb.asm.ClassWriter"
    include Opcodes
    import java.lang.Object
    import java.lang.Void
    include Signature

    def initialize(file_builder, class_name, file_name, superclass = Object, *interfaces)
      @file_builder = file_builder
      @class_name = class_name
      @superclass = superclass 
      
      @class_writer = ClassWriter.new(ClassWriter::COMPUTE_MAXS)
      
      interface_paths = []
      interfaces.each {|interface| interface_paths << path(interface)}
      @class_writer.visit(V1_4, ACC_PUBLIC | ACC_SUPER, class_name, nil, path(superclass), interface_paths.to_java(:string))
      @class_writer.visit_source(file_name, nil)
      
      @constructor = false
      
      @method_builders = {}
      @method_signatures = {}
      
      @imports = {}
    end
    
    def self.build(class_name, file_name, superclass = java.lang.Object, *interfaces, &block)
      fb = FileBuilder.new(file_name)
      cb = fb.public_class(class_name, superclass, *interfaces)
      cb.instance_eval &block
      cb.generate
    end
    
    def generate
      unless @constructor
        method2("<init>") do |method|
          method.aload 0
          method.invokespecial @superclass, "<init>", Void::TYPE
          method.returnvoid
        end
      end
      
      # lazily generate the actual methods
      @method_builders.each do |builder, block|
        builder.start
        block.call(builder)
        builder.stop
      end
      
      String.from_java_bytes(@class_writer.to_byte_array)
    end
    
    def field(name, type)
      @class_writer.visitField(ACC_PUBLIC, name.to_s, ci(type), nil, nil)
    end
    
    def constructor(*signature, &block)
      signature.unshift Void::TYPE
      
      MethodBuilder.build(self, ACC_PUBLIC, "<init>", signature, &block)
    end
    
    def method(name, *signature, &block)
      @method_signatures[name] = signature
      MethodBuilder.build(self, ACC_PUBLIC, name.to_s, signature, &block)
    end
    
    # New version does not instance_eval, to allow for easier embedding
    def method2(name, *signature, &block)
      @method_signatures[name] = signature
      mb = MethodBuilder.new(self, ACC_PUBLIC, name, signature)
      @method_builders[mb] = block
    end
    
    def static_method(name, *signature, &block)
      @method_signatures[name] = signature
      MethodBuilder.build(self, ACC_PUBLIC | ACC_STATIC, name.to_s, signature, &block)
    end
    
    # New version does not instance_eval, to allow for easier embedding
    def static_method2(name, *signature, &block)
      @method_signatures[name] = signature
      mb = MethodBuilder.new(self, ACC_PUBLIC | ACC_STATIC, name, signature)
      @method_builders[mb] = block
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
    
    def signature(name, arg_types)
      # TODO: allow overloading?
      @method_signatures[name] || find_super_signature(name, arg_types)
    end
    
    def find_super_signature(name, arg_types)
      # TODO: implement by searching parent
    end
    
    def import(strcls)
      bits = strcls.split(".")
      @imports[bits[-1].intern] = type_from_dotted(strcls)
    end
    
    def type(sym)
      @imports[sym] || @file_builder.type(sym)
    end
  end
  
  class MethodBuilder
    import "jruby.objectweb.asm.Opcodes"
    include Opcodes
    include Compiler::Bytecode
    
    
    # placeholder for now
    class InferredType
      attr_accessor :type
      
      def initialize(type)
       @type = type || java.lang.Object
      end
      
      def learn(type)
        # TODO ensure new type is compatible with previous type or raise
        @type ||= type || java.lang.Object
      end
    end
    
    attr_reader :method_visitor
    
    def initialize(class_builder, modifiers, name, signature)
      @class_builder = class_builder
      @modifiers = modifiers
      @name = name
      @signature = signature
      
      @method_visitor = class_builder.new_method(modifiers, name, signature)
      
      @locals = []
      @local_types = []
      
      @static = (modifiers & ACC_STATIC) != 0
      
      @locals << "this" unless @static
      @local_types << @class_builder unless @static
    end
    
    def self.build(class_builder, modifiers, name, signature, &block)
      mb = MethodBuilder.new(class_builder, modifiers, name, signature)
      mb.start
      mb.instance_eval &block
      mb.stop
    end
    
    def self.build2(class_builder, modifiers, name, signature, &block)
      mb = MethodBuilder.new(class_builder, modifiers, name, signature)
      mb.start
      block.call(mb)
      mb.stop
    end
    
    def this
      @class_builder
    end
    
    def local(name, type = nil)
      if name == "this" && @static
        raise "'this' attempted to load from static method"
      end
      
      if @locals.index(name)
        # TODO ensure new type fits existing inferred type
        @local_types[@locals.index(name)].learn(type) if type
      else
        @locals << name
        @local_types << InferredType.new(type)
      end
      @locals.index(name)
    end
    
    def local_type(name)
      @local_types[@locals.index(name)].type
    end
    
    def method_signature(name, arg_types)
      @class_builder.signature(name, arg_types)
    end
    
    def type(sym)
      @class_builder.type(sym)
    end
  end
end