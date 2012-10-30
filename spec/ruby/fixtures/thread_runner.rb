class ThreadRunner
  attr_reader :status, :thread_status

  TIME_DELTA = 0.1

  def initialize(timeout=0.25, &block)
    @timeout = timeout
    @status = nil

    @thread = Thread.new do
      @status = :started
      yield self
      @status = :finished
    end

    @monitor = Thread.new do
      Thread.pass until @status == :started

      time = 0
      while time < @timeout
        sleep TIME_DELTA

        break unless @thread.alive?

        time += TIME_DELTA
      end

      @thread_status = @thread.status

      if @thread.alive?
        @thread.kill
        @status = :killed
      end

      @thread.join
    end

    @monitor.join
  end
end
