# scripting.rb
# March 30, 2007
#

require 'jruby'

module Scriptable
  alias :pre_scripting_method_missing :method_missing

  def method_missing(sym, *args, &b)
    @engines ||= {}
    @engine_manager ||= javax.script.ScriptEngineManager.new(JRuby.runtime.getJRubyClassLoader)
    
    engine = @engines[sym] ||= @engine_manager.getEngineByName(sym.to_s)
    if engine
      if args.length < 1
        raise ArgumentError.new("expected script for argument zero")
      end

      if args.length >= 1
        script = args[0]
      end

      bindings = engine.createBindings

      local_variablator =<<-'LOCAL'
      _local_values = {}
      local_variables.each do |_local_variable|
        _local_values[_local_variable] = eval(_local_variable)
      end
      _local_values
      LOCAL

      eval(local_variablator, Binding.of_caller).each_pair {|k,v| bindings.put(k,v) if v}
      
      engine.eval(script, bindings)
    else
      pre_scripting_method_missing(sym, *args, &b)
    end
  end
  
  def eval_script(sym, *args)
    send sym, *args
  end
end