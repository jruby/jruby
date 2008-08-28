require 'compiler/bytecode'
require 'compiler/signature'

module Compiler
  class ClassBuilder
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

    def initialize(class_name, file_name, superclass, *interfaces)
      @class_name = class_name
      @superclass = superclass
      
      @class_writer = ClassWriter.new(ClassWriter::COMPUTE_MAXS)
      
      interface_paths = []
      interfaces.each {|interface| interface_paths << path(interface)}
      @class_writer.visit(Opcodes::V1_4, Opcodes::ACC_PUBLIC | Opcodes::ACC_SUPER, class_name, nil, path(superclass), interface_paths.to_java(:string))
      @class_writer.visit_source(file_name, nil)
    end
    
    def self.build(class_name, file_name, superclass = java.lang.Object, *interfaces, &block)
      cb = ClassBuilder.new(class_name, file_name, superclass, *interfaces)
      cb.instance_eval &block
      cb.generate
    end
    
    def generate
      String.from_java_bytes(@class_writer.to_byte_array)
    end
    
    def field(name, type)
      @class_writer.visitField(Opcodes::ACC_PUBLIC, name.to_s, ci(type), nil, nil)
    end
    
    def constructor(*signature, &block)
      signature.unshift Void::TYPE
      
      MethodBuilder.build(self, Opcodes::ACC_PUBLIC, "<init>", signature, &block)
    end
    
    def method(name, *signature, &block)
      MethodBuilder.build(self, Opcodes::ACC_PUBLIC, name.to_s, signature, &block)
    end
    
    def static_method(name, *signature, &block)
      MethodBuilder.build(self, Opcodes::ACC_PUBLIC | Opcodes::ACC_STATIC, name.to_s, signature, &block)
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
    include Compiler::Bytecode
    
    attr_reader :method_visitor
    
    def initialize(class_builder, modifiers, name, signature)
      @class_builder = class_builder
      @modifiers = modifiers
      @name = name
      @signature = signature
      
      @method_visitor = class_builder.new_method(modifiers, name, signature)
    end
    
    def self.build(class_builder, modifiers, name, signature, &block)
      mb = MethodBuilder.new(class_builder, modifiers, name, signature)
      mb.start
      mb.instance_eval &block
      mb.stop
    end
    
    def this
      @class_builder
    end
  end
end