class BeComputedByFunctionMatcher
  def initialize(sym, *args)
    @function = sym
    @args = args
  end

  def matches?(array)
    array.each do |line|
      @value = line.pop
      @arguments = line
      @arguments += @args
      return false unless send(@function, *@arguments) == @value
    end

    return true
  end

  def function_call
    function_call = "#{@function}"
    unless @arguments.empty?
      function_call << "(#{@arguments.map { |x| x.inspect }.join(", ")})"
    end
    function_call
  end

  def failure_message
    ["Expected #{@value.inspect}", "to be computed by #{function_call}"]
  end
end

class Object
  def be_computed_by_function(sym, *args)
    BeComputedByFunctionMatcher.new(sym, *args)
  end
end
