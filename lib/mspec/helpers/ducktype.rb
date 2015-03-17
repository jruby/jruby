class Object
  def responds_to(sym)
    singleton_class.class_eval <<-END
      def respond_to?(sym, include_private=false)
        sym.to_sym == #{sym.to_sym.inspect} ? true : super
      end
    END
  end

  def does_not_respond_to(sym)
    singleton_class.class_eval <<-END
      def respond_to?(sym, include_private=false)
        sym.to_sym == #{sym.to_sym.inspect} ? false : super
      end
    END
  end

  def undefine(sym)
    singleton_class.class_eval <<-END
      undef_method #{sym.to_sym.inspect}
    END
  end

  def fake!(sym, value=nil)
    responds_to sym

    singleton_class.class_eval <<-END
      def method_missing(sym, *args)
        return #{value.inspect} if sym.to_sym == #{sym.to_sym.inspect}
      end
    END
  end
end
