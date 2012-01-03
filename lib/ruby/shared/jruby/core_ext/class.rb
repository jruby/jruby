require 'java'
require 'jruby'

class Class
  JClass = java.lang.Class
  
  # Get an array of all known subclasses of this class. If recursive == true,
  # include all descendants.
  def subclasses(recursive = false)
    JRuby.reference0(self).subclasses(recursive).to_a.freeze
  end
  
  # Generate a native Java class for this Ruby class. If dump_dir is specified,
  # dump the JVM bytecode there for inspection. If child_loader is false, do not
  # generate the class into its own classloader (use the parent's loader).
  # 
  # :call-seq:
  #   become_java!
  #   become_java!(dump_dir)
  #   become_java!(dump_dir, child_loader)
  #   become_java!(child_loader)
  #   become_java!(child_loader, dump_dir)
  def become_java!(*args)
    self_r = JRuby.reference0(self)
    
    if args.size > 0
      dump_dir = nil
      child_loader = true
      if args[0].kind_of? String
        dump_dir = args[0].to_s
        if args.size > 1
          child_loader = args[1]
        end
      else
        child_loader = args[0]
        if args.size > 1
          dump_dir = args[1].to_s
        end
      end
      
      self_r.reify_with_ancestors(dump_dir, child_loader)
    else
      self_r.reify_with_ancestors
    end
    
    self_r.reified_class
  end
  
  # Get the native or reified (a la become_java!) class for this Ruby class.
  def java_class
    self_r = JRuby.reference0(self)
    current = self_r
    while current
      reified = current.reified_class
      return reified if reified
      current = JRuby.reference0(current.super_class)
    end
    
    nil
  end
  
  def _anno_class(cls)
    if cls.kind_of? JClass
      cls
    elsif cls.respond_to? :java_class
      cls.java_class.to_java :object
    else
      raise TypeError, "expected a Java class, got #{cls}"
    end
  end
  private :_anno_class
  
  # Add annotations to the named method. Annotations are specified as a Hash
  # from the annotation classes to Hashes of name/value pairs for their parameters.
  def add_method_annotation(name, annotations = {})
    name = name.to_s
    self_r = JRuby.reference0(self)
    
    for cls, params in annotations
      params ||= {}
      self_r.add_method_annotation(name, _anno_class(cls), params)
    end
    
    nil
  end
  
  # Add annotations to the parameters of the named method. Annotations are
  # specified as a parameter-list-length Array of Hashes from annotation classes
  # to Hashes of name/value pairs for their parameters.
  def add_parameter_annotation(name, annotations = [])
    name = name.to_s
    self_r = JRuby.reference0(self)
    
    annotations.each_with_index do |param_annos, i|
      for cls, params in param_annos
        params ||= {}
        self_r.add_parameter_annotation(name, i, _anno_class(cls))
      end
    end
    
    nil
  end
  
  # Add annotations to this class. Annotations are specified as a Hash of
  # annotation classes to Hashes of name/value pairs for their parameters.
  def add_class_annotations(annotations = {})
    self_r = JRuby.reference0(self)
    
    for cls, params in annotations
      params ||= {}
      
      self_r.add_class_annotation(_anno_class(cls), params)
    end
    
    nil
  end
  
  # Add a Java signaturefor the named method. The signature is specified as
  # an array of Java classes.
  def add_method_signature(name, classes)
    name = name.to_s
    self_r = JRuby.reference0(self)
    types = []
    
    classes.each {|cls| types << _anno_class(cls)}
    
    self_r.add_method_signature(name, types.to_java(JClass))
    
    nil
  end
end