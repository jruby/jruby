class String
  # remove indentation from the beginning of each line
  # according to the number of blanks in the first line
  def unindent
    match = /^\s+/.match(self)
    return unless match
    self.gsub(/^\s{#{match[0].length}}/, "")
  end
end

module ExceptionHelper
  # try(anException, result) { ... } 
  #   => result if block does not raise anException, else nil
  def try(a, b=true)
    yield
    return nil
  rescue a
    return b
  end
end

class Object
  def should_raise(exc)
    yield
  rescue Object => ex
    exc.should === ex
  end

  def should_include(*other)
    should Spec::Matchers::Include.new(*other)
  end
end
  
# Example courtesy of nicksieger, many thanks!
class Spec::Runner::Context
  def before_context_eval
    @context_eval_module.include ExceptionHelper

    case ENV['SPEC_TARGET']
    when /mri/
      require 'mri_target'
      @context_eval_module.include MRITarget
    when /jruby/
      require 'jruby_target'
      @context_eval_module.include JRubyTarget
    else
      require 'rubinius_target'
      @context_eval_module.include RubiniusTarget
    end
  end
end
