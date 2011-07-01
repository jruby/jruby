class WIN32OLERuntimeError < RuntimeError
  def initialize(cause=nil)
    @cause = cause
  end

  def backtrace
    @cause ? @cause.backtrace : super
  end

  def to_s
    @cause ? @cause.to_s : super
  end
end
