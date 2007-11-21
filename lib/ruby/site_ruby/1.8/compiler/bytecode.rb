require 'compiler/signature'

module Compiler
  include Java
  
  # Bytecode is a simple adapter around an ASM MethodVisitor that makes it look like
  # JVM assembly code. Included classes must just provide a method_visitor accessor
  module Bytecode
    include Signature
    
    import "jruby.objectweb.asm.Opcodes"
  
    import java.lang.Object
    import java.lang.System
    import java.io.PrintStream
    import java.lang.Void
    
    Opcodes.constants.each do |const_name|
      const_down = const_name.downcase
      case const_name
      when "ALOAD", "ILOAD", "ASTORE"
        # variable instructions
        eval "def #{const_down}(var); method_visitor.visit_var_insn(Opcodes::#{const_name}, var); end"
      when "LDC"
        # constant loading is tricky because overloaded invocation is pretty bad in JRuby
        def ldc_int(value); method_visitor.visit_ldc_insn(java.lang.Integer.new(value)); end
        eval "def #{const_down}(value); method_visitor.visit_ldc_insn(value); end"
      when "INVOKESTATIC", "INVOKEVIRTUAL", "INVOKEINTERFACE", "INVOKESPECIAL"
        # method instructions
        eval "def #{const_down}(type, name, call_sig); method_visitor.visit_method_insn(Opcodes::#{const_name}, path(type), name.to_s, sig(*call_sig)); end"
      when "RETURN"
        # special case for void return, since return is a reserved word
        def returnvoid(); method_visitor.visit_insn(Opcodes::RETURN); end
      when "ARETURN", "DUP", "SWAP", "POP", "POP2", "ICONST_0", "ICONST_1", "ICONST_2",
          "ICONST_3", "LCONST_0", "ISUB", "ACONST_NULL", "NOP", "AALOAD", "IALOAD",
          "BALOAD", "BASTORE", "DUP_X1", "DUP_X2", "DUP2", "DUP2_X1", "DUP2_X2",
          "ATHROW", "ARRAYLENGTH", "IADD", "IINC"
        # bare instructions
        eval "def #{const_down}; method_visitor.visit_insn(Opcodes::#{const_name}); end"
      when "NEW", "ANEWARRAY", "NEWARRAY", "INSTANCEOF", "CHECKCAST"
        # type instructions
        eval("def #{const_down}(type); method_visitor.visit_type_insn(Opcodes::#{const_name}, path(type)); end")
      when "GETFIELD", "PUTFIELD", "GETSTATIC", "PUTSTATIC"
        # field instructions
        eval("def #{const_down}(type, name, field_sig); method_visitor.visit_field_insn(Opcodes::#{const_name}, path(type), name.to_s, ci(*field_sig)); end")
      when "GOTO", "IFEQ", "IFNE", "IF_ACMPEQ", "IF_ACMPNE", "IF_ICMPEQ", "IF_ICMPNE", "IF_ICMPLT",
          "IF_ICMPGT", "IF_ICMPLE", "IF_ICMPGE", "IFNULL", "IFNONNULL"
        # jump instructions
        eval("def #{const_down}(target); method_visitor.visit_jump_insn(Opcodes::#{const_name}, target); end")
      when "LOOKUPSWITCH"
        def lookupswitch(default, ints, cases); method_visitor.visit_lookup_switch_insn(default, ints, cases); end
      when "TABLESWITCH"
        def tableswitch(min, max, default, cases); method_visitor.visit_table_switch_insn(min, max, default, cases); end
      end
    end
    
    def start
      method_visitor.visit_code
    end
    
    def stop
      method_visitor.visit_maxs(1,1)
      method_visitor.visit_end
    end
    
    def trycatch(from, to, target, type) 
      method_visitor.visit_try_catch_block(from, to, target, type) 
    end
    
    def label(lbl)
      method_visitor.visit_label(lbl)
    end
    
    def aprintln
      dup
      getstatic path(System), "out", PrintStream
      swap
      invokevirtual path(PrintStream), "println", [Void::TYPE, Object]
    end
    
    def swap2
      dup2_x2
      pop
    end
  end
end