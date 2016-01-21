require 'java'

import org.jruby.compiler.impl.SkinnyMethodAdapter

begin
  import org.objectweb.asm.MethodVisitor
  import org.objectweb.asm.Opcodes
rescue # jarjar renames things, so we try the renamed version
  import "org.jruby.org.objectweb.asm.MethodVisitor"
  import "org.jruby.org.objectweb.asm.Opcodes"
end

describe "SkinnyMethodAdapter" do
  let(:instance_methods) { SkinnyMethodAdapter.instance_methods.map(&:to_s) }
  let(:insn_opcodes) do
    Opcodes.constants.map(&:to_s).select do |c|
      case c

      when /ACC_/, # access modifiers
           /V1_/, # version identifiers
           /T_/, # type identifiers
           /F_/, # framing hints
           /H_/, # method handles
           /ASM/ # ASM version stuff
        false

      when "DOUBLE", "FLOAT", "INTEGER", "LONG", "NULL", "TOP", "UNINITIALIZED_THIS"
        false

      when "GOTO", "RETURN", "INSTANCEOF", "NEW"
        false

      when "INVOKEDYNAMIC_OWNER"
        false

      else
        true
      end
    end.map(&:downcase)
  end

  it "supports all JVM opcodes" do
    expect(instance_methods).to include(*insn_opcodes)
    expect(instance_methods).to include(
      "go_to",
      "voidreturn",
      "instance_of",
      "newobj"
    )
  end
end
