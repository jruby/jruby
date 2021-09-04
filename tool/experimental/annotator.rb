require 'java'

class Method
  attr_accessor :annotations
end

class Class
  def annotate_method(anno_values)
    @annotations ||= {}
    if Hash === anno_values
      @annotations.merge!(anno_values)
    else
      @annotations[anno_values.java_class] = {}
    end
  end

  def method_added(name)
    if @annotations
      add_method_annotation(name.to_s, @annotations)
      @annotations = nil
    end
    super
  end

  def annotate_class(anno_values)
    if Hash === anno_values
      add_class_annotation(anno_values)
    else
      add_class_annotation(anno_values.java_class => {})
    end
  end
end