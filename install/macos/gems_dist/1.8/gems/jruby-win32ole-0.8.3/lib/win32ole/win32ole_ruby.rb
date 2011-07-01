require 'win32ole/utils'
require 'jruby'

# Shorthand vocabulary:
#  ti, oti - typeinfo or owner_typeinfo

class WIN32OLE
  CP_ACP = 0
  CP_OEMCP = 1
  CP_MACCP = 2
  CP_THREAD_ACP = 3
  CP_SYMBOL = 42
  CP_UTF7 = 65000
  CP_UTF8 = 65001

  def ole_method(name)
    all_methods(typeinfo_from_ole) do |*args|
      return WIN32OLE_METHOD.new(nil, *args) if name == args[3].name
      nil
    end
  end
  alias :ole_method_help :ole_method

  def ole_methods
    members = []
    all_methods(typeinfo_from_ole) do |*args|
      members << WIN32OLE_METHOD.new(nil, *args)
      nil
    end
    members
  end

  def ole_func_methods
    methods_with_flag(Dispatch::Method)
  end

  def ole_get_methods
    methods_with_flag(Dispatch::Get)
  end

  def ole_put_methods
    methods_with_flag(Dispatch::Put|Dispatch::PutRef)
  end

  def ole_type
    typelib = type_info.containing_type_lib
    docs = typelib.documentation(typelib.index)
    WIN32OLE_TYPE.new typelib, type_info, docs
  end
  alias :ole_obj_help :ole_type

  def ole_typelib
    typelib = type_info.containing_type_lib
    WIN32OLE_TYPELIB.new typelib, nil
  end

  def type_info
    dispatch.type_info
  end

  class << self
    def codepage
      @@codepage ||= CP_ACP
    end

    def codepage=(new_codepage)
      @@codepage = new_codepage
    end

    def connect(id)
      WIN32OLE.new to_progid(id)
    end

    def const_load(ole, a_class=WIN32OLE)
      constants = {}
      ole.type_info.containing_type_lib.type_info.to_a.each do |info|
        info.vars_count.times do |i|
          var_desc = info.get_var_desc(i)
          # TODO: Missing some additional flag checks to limit no. of constants
          if var_desc.constant
            name = first_var_name(info, var_desc)
            name = name[0].chr.upcase + name[1..-1] if name
            if constant?(name)
              a_class.const_set name, RubyWIN32OLE.from_variant(JRuby.runtime, var_desc.constant)
            else # vars which don't start [A-Z]?
              constants[name] = var_desc.constant
            end
          end
        end
      end
      a_class.const_set 'CONSTANTS', constants
      nil
    end

    def to_progid(id)
      id =~ /^{(.*)}/ ? "clsid:#{$1}" : id
    end

    private

    def constant?(name)
      name =~ /^[A-Z]/
    end

    def first_var_name(type_info, var_desc)
      type_info.get_names(var_desc.memid)[0]
    rescue
      nil
    end
  end

  private

  include WIN32OLE::Utils
end
