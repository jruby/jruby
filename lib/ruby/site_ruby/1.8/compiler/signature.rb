require 'java'

module Compiler
  module JavaTypes
    import java.lang.Object
    import java.lang.Byte
    import java.lang.Boolean
    import java.lang.Short
    import java.lang.Character
    import java.lang.Integer
    import java.lang.Long
    import java.lang.Float
    import java.lang.Double
    import java.lang.Void
  end
  module Signature
    def classname(path)
      path.gsub('/', '.')
    end
    module_function :classname
    
    def path(cls)
      return cls if Symbol === cls
      cls = cls.java_class if Class === cls
      cls.name.gsub('.', '/')
    end
    module_function :path
    
    def class_id(cls)
      cls = cls.java_class if Class === cls
      
      unless cls
        return "V"
      end
      
      if Module === cls
        return "L#{cls.java_class};"
      end
      
      if cls.array?
        cls = cls.component_type
        if cls.primitive?
          case cls
          when JavaTypes::Byte::TYPE
            return "[B"
          when JavaTypes::Boolean::TYPE
            return "[Z"
          when JavaTypes::Short::TYPE
            return "[S"
          when JavaTypes::Character::TYPE
            return "[C"
          when JavaTypes::Integer::TYPE
            return "[I"
          when JavaTypes::Long::TYPE
            return "[J"
          when JavaTypes::Float::TYPE
            return "[F"
          when JavaTypes::Double::TYPE
            return "[D"
          else
            raise "Unknown type in compiler: " + cls.name
          end
        else
          return "[#{class_id(cls)}"
        end
      else
        if cls.primitive?
          case cls
          when JavaTypes::Byte::TYPE
            return "B"
          when JavaTypes::Boolean::TYPE
            return "Z"
          when JavaTypes::Short::TYPE
            return "S"
          when JavaTypes::Character::TYPE
            return "C"
          when JavaTypes::Integer::TYPE
            return "I"
          when JavaTypes::Long::TYPE
            return "J"
          when JavaTypes::Float::TYPE
            return "F"
          when JavaTypes::Double::TYPE
            return "D"
          when JavaTypes::Void::TYPE
            return "V"
          else
            raise "Unknown type in compiler: " + cls.name
          end
        else
          return "L#{path(cls)};"
        end
      end
    end
    alias ci class_id
    module_function :class_id, :ci
    
    def signature(*sig_classes)
      if sig_classes.size == 0
        return "()V"
      end
      
      return_class = sig_classes.shift
      sig_string = "("
      sig_classes.each {|arg_class| sig_string << class_id(arg_class)}
      sig_string << ")#{class_id(return_class)}"
    end
    alias sig signature
    module_function :signature, :sig
  end
end