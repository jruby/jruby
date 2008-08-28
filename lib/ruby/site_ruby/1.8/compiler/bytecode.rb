require 'compiler/signature'

module Compiler
  # Bytecode is a simple adapter around an ASM MethodVisitor that makes it look like
  # JVM assembly code. Included classes must just provide a method_visitor accessor
  module Bytecode
    include Signature

    begin
      import "jruby.objectweb.asm.Opcodes"
      import "jruby.objectweb.asm.Label"
    rescue Exception
      import "org.objectweb.asm.Opcodes"
      import "org.objectweb.asm.Label"
    end
  
    import java.lang.Object
    import java.lang.System
    import java.io.PrintStream
    import java.lang.Void
    
    b = binding
    Opcodes.constants.each do |const_name|
      const_down = const_name.downcase
      
      case const_name
      when "ALOAD", "ASTORE",
          "ISTORE", "ILOAD",
          "LSTORE", "LLOAD",
          "FSTORE", "FLOAD",
          "DSTORE", "DLOAD",
          "RET"
        # variable instructions
        eval "
            def #{const_down}(var)
              method_visitor.visit_var_insn(Opcodes::#{const_name}, var)
            end
          ", b, __FILE__, __LINE__
          
      when "LDC"
        # constant loading is tricky because overloaded invocation is pretty bad in JRuby
        def ldc_int(value); method_visitor.visit_ldc_insn(java.lang.Integer.new(value)); end
        def ldc_long(value); method_visitor.visit_ldc_insn(java.lang.Long.new(value)); end
        def ldc_float(value); method_visitor.visit_ldc_insn(java.lang.Float.new(value)); end
        def ldc_double(value); method_visitor.visit_ldc_insn(java.lang.Double.new(value)); end
        eval "
            def #{const_down}(value)
              value = value.to_s if Symbol === value
              method_visitor.visit_ldc_insn(value)
            end
          ", b, __FILE__, __LINE__

      when "BIPUSH", "SIPUSH"
        eval "
            def #{const_down}(value)
              method_visitor.visit_int_insn(Opcodes::#{const_name}, value)
            end
        "
          
      when "INVOKESTATIC", "INVOKEVIRTUAL", "INVOKEINTERFACE", "INVOKESPECIAL"
        # method instructions
        eval "
            def #{const_down}(type, name, call_sig)
              method_visitor.visit_method_insn(Opcodes::#{const_name}, path(type), name.to_s, sig(*call_sig))
            end
          ", b, __FILE__, __LINE__
          
      when "RETURN"
        # special case for void return, since return is a reserved word
        def returnvoid()
          method_visitor.visit_insn(Opcodes::RETURN)
        end
        
      when "DUP", "SWAP", "POP", "POP2", "DUP_X1", "DUP_X2", "DUP2", "DUP2_X1", "DUP2_X2",
          "NOP",
          "ARRAYLENGTH",
          "ARETURN", "ATHROW", "ACONST_NULL", "AALOAD", "AASTORE",
          "BALOAD", "BASTORE",
          "CALOAD", "CASTORE",
          "SALOAD", "SASTORE",
          "ICONST_M1", "ICONST_0", "ICONST_1", "ICONST_2", "ICONST_3", "ICONST_4", "ICONST_5", "IRETURN", "IALOAD",
          "IADD", "IINC", "ISUB", "IDIV", "IMUL", "INEG", "IAND", "IOR", "IXOR", "IASTORE",
          "IUSHR", "ISHL", "ISHR", "I2L", "I2S", "I2F", "I2D", "I2B", "IREM", "I2C",
          "LCONST_0", "LCONST_1", "LRETURN", "LALOAD", "LASTORE", "LCMP", "LSHL", "LSHR", "LREM", "LUSHR",
          "LADD", "LINC", "LSUB", "LDIV", "LMUL", "LNEG", "LAND", "LOR", "LXOR", "L2I", "L2F", "L2D",
          "FCONST_0", "FCONST_1", "FCONST_2", "FRETURN", "FALOAD", "F2D", "F2I", "FASTORE",
          "FADD", "FSUB", "FDIV", "FMUL", "FNEG", "FREM", "FCMPG", "F2L", "FCMPL",
          "DCONST_0", "DCONST_1", "DRETURN", "DALOAD", "DASTORE", "D2I", "D2F", "D2L",
          "DADD", "DINC", "DSUB", "DDIV", "DMUL", "DNEG", "DCMPL", "DCMPG", "DREM",
          "MONITORENTER", "MONITOREXIT"
        # bare instructions
        eval "
            def #{const_down}
              method_visitor.visit_insn(Opcodes::#{const_name})
            end
          ", b, __FILE__, __LINE__
          
      when "NEW", "ANEWARRAY", "NEWARRAY", "INSTANCEOF", "CHECKCAST"
        # type instructions
        eval "
            def #{const_down}(type)
              method_visitor.visit_type_insn(Opcodes::#{const_name}, path(type))
            end
          ", binding, __FILE__, __LINE__
          
      when "GETFIELD", "PUTFIELD", "GETSTATIC", "PUTSTATIC"
        # field instructions
        eval "
            def #{const_down}(type, name, field_sig)
              method_visitor.visit_field_insn(Opcodes::#{const_name}, path(type), name.to_s, ci(*field_sig))
            end
          ", b, __FILE__, __LINE__
          
      when "GOTO", "IFEQ", "IFNE", "IF_ACMPEQ", "IF_ACMPNE", "IF_ICMPEQ", "IF_ICMPNE", "IF_ICMPLT",
          "IF_ICMPGT", "IF_ICMPLE", "IF_ICMPGE", "IFNULL", "IFNONNULL", "JSR",
          "IFLE", "IFGE", "IFLT", "IFGT"
        # jump instructions
        eval "
            def #{const_down}(target)
              method_visitor.visit_jump_insn(Opcodes::#{const_name}, target.label)
            end
          ", b, __FILE__, __LINE__
          
      when "MULTIANEWARRAY"
        # multi-dim array
        eval "
            def #{const_down}(type, dims)
              method_visitor.visit_multi_anew_array_insn(ci(type), dims)
            end
          ", b, __FILE__, __LINE__
          
      when "LOOKUPSWITCH"
        def lookupswitch(default, ints, cases)
          method_visitor.visit_lookup_switch_insn(default, ints, cases)
        end
        
      when "TABLESWITCH"
        def tableswitch(min, max, default, cases)
          method_visitor.visit_table_switch_insn(min, max, default, cases)
        end

      when "F_FULL", "ACC_ENUM", "ACC_SYNTHETIC", "ACC_INTERFACE", "ACC_PUBLIC",
          "ACC_PRIVATE", "ACC_PROTECTED", "ACC_DEPRECATED", "ACC_BRIDGE",
          "ACC_VARARGS", "ACC_SUPER", "F_CHOP", "F_APPEND", "FLOAT", "F_SAME",
          "T_LONG", "INTEGER", "T_BYTE", "ACC_STATIC", "ACC_SYNCHRONIZED",
          "T_BOOLEAN", "ACC_ANNOTATION", "ACC_ABSTRACT", "LONG", "ACC_TRANSIENT",
          "T_DOUBLE", "DOUBLE", "ACC_STRICT", "NULL", "T_FLOAT", "ACC_FINAL",
          "F_SAME1", "ACC_NATIVE", "F_NEW", "T_CHAR", "T_INT", "ACC_VOLATILE",
          "V1_6", "V1_5", "V1_4", "V1_3", "V1_2", "V1_1", "UNINITIALIZED_THIS",
          "TOP", "T_SHORT"
        # non-instructions

      else
        raise "Unknown opcode: " + const_name
        
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
    
    class SmartLabel
      attr_reader :label
      
      def initialize(method_visitor)
        @method_visitor = method_visitor
        @label = Label.new
      end
      
      def set!
        @method_visitor.visit_label(@label)
      end
    end
    
    def label
      return SmartLabel.new(method_visitor)
    end
    
    def aprintln
      dup
      getstatic System, "out", PrintStream
      swap
      invokevirtual PrintStream, "println", [Void::TYPE, Object]
    end
    
    def swap2
      dup2_x2
      pop2
    end
    
    def line(num)
      method_visitor.visit_line_number num, Label.new
    end

    def push_int(num)
      if (num <= Java::java.lang.Byte::MAX_VALUE && num >= Java::java.lang.Byte::MIN_VALUE)
        case num
        when -1
          iconst_m1
        when 0
          iconst_0
        when 1
          iconst_1
        when 2
          iconst_2
        when 3
          iconst_3
        when 4
          iconst_4
        when 5
          iconst_5
        else
          bipush(num)
        end
      elsif (num <= Java::java.lang.Short::MAX_VALUE && num >= Java::java.lang.Short::MIN_VALUE)
        sipush(num)
      else
        ldc(num)
      end
    end
  end
end