require 'java'
require 'rdoc/ri/ri_driver'
require 'rdoc/ri/ri_reader'

class RiDriver
  def report_java_method_stuff(java_methods)
    method = @ri_reader.get_java_method(java_methods)
    @display.display_method_info(method)
  end

  def report_java_class_stuff(cls)
    klass = @ri_reader.get_java_class(cls)
    @display.display_class_info(klass, @ri_reader)
  end

  def get_java_info_for(arg)
    cls_name = arg

    if arg['#']
      cls_name, mth_name = arg.split '#'
      static = -1
      begin
        cls = java.lang.Class.for_name(cls_name)
      rescue Exception
        raise RiError.new("Nothing known about #{arg}")
      end
    elsif arg['::']
      cls_name, mth_name = arg.split '::'
      static = 1
      begin
        cls = java.lang.Class.for_name(cls_name)
      rescue Exception
        raise RiError.new("Nothing known about #{arg}")
      end
    else
      static = 0
      begin
        cls = java.lang.Class.for_name(cls_name)
      rescue Exception
        begin
          splitted = arg.split('.')
          cls_name = splitted[0..-2].join('.')
          mth_name = splitted[-1]
          cls = java.lang.Class.for_name(cls_name)
        end
      end
    end

    if mth_name
      # print method info
      if static == -1 # instance
        methods = cls.methods.select do |m|
          m.name == mth_name &&
            !java.lang.reflect.Modifier.is_static(m.modifiers)
        end
      elsif
        static == 1 # static
        methods = cls.methods.select do |m|
          m.name == mth_name &&
            java.lang.reflect.Modifier.is_static(m.modifiers)
        end
      else # one or other
        last = nil
        methods = cls.methods.select do |m|
          # filter by name
          next false unless m.name == mth_name

          # track whether we've seen both static and instance
          static = java.lang.reflect.Modifier.is_static(m.modifiers) ? 1 : -1
          if last && last != static
            raise RiError.new("Ambiguous: try using \# or :: for #{arg}")
          end
          last = static

          true
        end
      end
      if !methods || methods.empty?
        raise RiError.new("Nothing known about #{arg}")
      end

      report_java_method_stuff(methods)
    else
      report_java_class_stuff(cls)
    end
  end
end

module RI
  class RiReader
    def get_nice_java_class_name(cls)
      if cls.primitive?
        cls.name
      elsif cls.array?
        "#{get_nice_java_class_name(cls.component_type)}[]"
      else
        cls.name
      end
    end

    def get_java_method(java_methods)
      modifier = java.lang.reflect.Modifier
      desc = RI::MethodDescription.new
      is_static = modifier.is_static(java_methods[0].modifiers)
      desc.is_class_method = is_static
      desc.visibility = "public"
      desc.name = java_methods[0].name
      desc.full_name =
        "#{java_methods[0].declaring_class.name}#{is_static ? '.' : '#'}#{desc.name}"
      desc.params = ""
      java_methods.each do |java_method|
        param_strs = java_method.parameter_types.map {|cls| get_nice_java_class_name(cls)}
        desc.params <<
          "#{java_method.name}(#{param_strs.join(',')}) => #{get_nice_java_class_name(java_method.return_type)}\n"
      end

      desc
    end

    def get_java_class(class_entry)
      desc = RI::ClassDescription.new

      desc.full_name = class_entry.name
      desc.superclass = class_entry.superclass.name if class_entry.superclass

      # TODO interfaces?
      desc.includes = []

      # TODO constants
      desc.constants = []

      # TODO bean attributes or something?
      desc.attributes = []

      modifier = java.lang.reflect.Modifier
      methods = class_entry.methods
      cls_methods = {}
      methods.select {|m| modifier.is_static(m.modifiers)}.each {|m| cls_methods[m.name] = m}
      inst_methods = {}
      methods.select {|m| !modifier.is_static(m.modifiers)}.each {|m| inst_methods[m.name] = m}

      desc.class_methods = []
      cls_methods.keys.each do |name|
        desc.class_methods << RI::MethodSummary.new(name)
      end

      desc.instance_methods = []
      inst_methods.keys.each do |name|
        desc.instance_methods << RI::MethodSummary.new(name)
      end

      desc
    end
  end
end
