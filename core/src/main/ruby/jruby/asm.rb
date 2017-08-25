
module JRuby
  # Internal module which acts as a proxy for ASM class names.
  # Auto loaded on demand (@see jruby.rb) when JRuby::ASM is accessed.
  # @private
  module ASM

    begin
      const_set(:TraceClassVisitor, org.jruby.org.objectweb.asm.util.TraceClassVisitor)
      const_set(:ClassReader, org.jruby.org.objectweb.asm.ClassReader)
    rescue
      const_set(:TraceClassVisitor, org.objectweb.asm.util.TraceClassVisitor)
      const_set(:ClassReader, org.objectweb.asm.ClassReader)
    end

  end
end
