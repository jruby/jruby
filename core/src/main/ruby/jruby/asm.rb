
module JRuby
  # Internal module which acts as a proxy for ASM class names.
  # Auto loaded on demand (@see jruby.rb) when JRuby::ASM is accessed.
  # @private
  module ASM

    begin
      Opcodes = org.jruby.org.objectweb.asm.Opcodes
      ClassReader = org.jruby.org.objectweb.asm.ClassReader
      ClassWriter = org.jruby.org.objectweb.asm.ClassWriter
      TraceClassVisitor = org.jruby.org.objectweb.asm.util.TraceClassVisitor
    rescue
      Opcodes = org.objectweb.asm.Opcodes
      ClassReader = org.objectweb.asm.ClassReader
      ClassWriter = org.objectweb.asm.ClassWriter
      TraceClassVisitor = org.objectweb.asm.util.TraceClassVisitor
    end

  end
end
