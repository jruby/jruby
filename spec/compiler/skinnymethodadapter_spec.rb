describe "SkinnyMethodAdapter" do

  before(:all) do
    require 'java'; require 'jruby'
  end

  let(:instance_methods) { org.jruby.compiler.impl.SkinnyMethodAdapter.instance_methods.map(&:to_s) }
  let(:insn_opcodes) do
    JRuby::ASM::Opcodes.constants.map(&:to_s).select do |c|
      case c

      when /ACC_/, # access modifiers
           /V1_/, # version identifiers
           /V[0-9]+/, # version identifiers
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

      when "V_PREVIEW_EXPERIMENTAL"
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
