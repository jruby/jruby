module Kernel
  module_function
  def require_relative(relative_feature)
    if relative_feature.respond_to? :to_path
      relative_feature = relative_feature.to_path
    else
      relative_feature = relative_feature
    end
    unless relative_feature.respond_to? :to_str
      raise TypeError, "cannot convert #{relative_feature.class} into String"
    end
    relative_feature = relative_feature.to_str
    
    c = caller.first
    e = c.rindex(/:\d+:in /)
    file = $`
    if /\A\((.*)\)/ =~ file # eval, etc.
      raise LoadError, "cannot infer basepath"
    end
    absolute_feature = File.join(File.dirname(File.realpath(file)), relative_feature)
    require absolute_feature
  end

  def exec(*args)
    _exec_internal(*JRuby::ProcessUtil.exec_args(args))
  end
  
  def spawn(*args)
    Process.spawn(*args)
  end
end
