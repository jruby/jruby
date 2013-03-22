module Kernel
  module_function
  def require_relative(relative_feature)
    if relative_feature.respond_to? :to_path
      relative_feature = relative_feature.to_path
    else
      relative_feature = relative_feature
    end

    relative_feature = JRuby::Type.convert_to_str(relative_feature)
    
    c = caller.first
    e = c.rindex(/:\d+:in /) 
    file = $` # just the filename
    if /\A\((.*)\)/ =~ file # eval, etc.
      raise LoadError, "cannot infer basepath"
    end
    absolute_feature = File.expand_path(relative_feature, File.dirname(file))
    require absolute_feature
  end

  def exec(*args)
    _exec_internal(*JRuby::ProcessUtil.exec_args(args))
  end
  
  def spawn(*args)
    Process.spawn(*args)
  end
end
