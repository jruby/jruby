scheduler = Class.new do
  attr_reader :writes
  def initialize = @writes = []
  def fiber(&block) = Fiber.new(blocking: false, &block).tap(&:resume)
  def io_wait(_io, events, _timeout) = events
  def kernel_sleep(_duration = nil) = true
  def block(*) = true
  def unblock(*) = nil
  def close = nil
  def fiber_interrupt(fiber, exception) = fiber.raise(exception)
  def io_write(io, buffer, _length, offset)
    data = buffer.get_string(offset)
    @writes << data
    Fiber.blocking { io.syswrite(data) }
  end
end.new

Thread.new do
  Fiber.set_scheduler(scheduler)
  Fiber.schedule do
    STDOUT.write("a\n")
    STDOUT.write("b\n")
    STDOUT.flush
  end
end.join
STDOUT.sync = true
p scheduler.writes
