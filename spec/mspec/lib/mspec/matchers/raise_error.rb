class RaiseErrorMatcher
  def initialize(exception, message, &block)
    @exception = exception
    @message = message
    @block = block
  end

  def matches?(proc)
    @result = proc.call
    return false
  rescue Exception => @actual
    if matching_exception?(@actual)
      return true
    else
      raise @actual
    end
  end

  def matching_exception?(exc)
    return false unless @exception === exc
    if @message then
      case @message
      when String
        return false if @message != exc.message
      when Regexp
        return false if @message !~ exc.message
      end
    end

    # The block has its own expectations and will throw an exception if it fails
    @block[exc] if @block

    return true
  end

  def failure_message
    message = ["Expected #{@exception}#{%[ (#{@message})] if @message}"]

    if @actual then
      message << "but got #{@actual.class}#{%[ (#{@actual.message})] if @actual.message}"
    else
      message << "but no exception was raised (#@result was returned)"
    end

    message
  end

  def negative_failure_message
    message = ["Expected to not get #{@exception}#{%[ (#{@message})] if @message}", ""]
    message[1] = "but got #{@actual.class}#{%[ (#{@actual.message})] if @actual.message}" unless @actual.class == @exception
    message
  end
end

class Object
  def raise_error(exception=Exception, message=nil, &block)
    RaiseErrorMatcher.new(exception, message, &block)
  end
end

# Legacy alias
RaiseExceptionMatcher = RaiseErrorMatcher

class Object
  alias_method :raise_exception, :raise_error
end
