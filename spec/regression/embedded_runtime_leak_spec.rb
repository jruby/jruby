describe "embedded runtimes" do
  it "should not leak runtimes after tearing them down" do
    create_runtimes(10) do |runtime|
      # nothing to do actually - the runtimes will by default build up
      # and OOM if they aren't being garbage collected
    end
  end

  it "should not leak runtimes blocked on IO" do
    create_runtimes(10) do |runtime|
      latch = java.util.concurrent.CountDownLatch.new(1)
      Thread.new do
        latch.countDown
        # we'll get an OOM if the blocking TCPServer#accept call below
        # doesn't get interrupted during runtime teardown
        runtime.evalScriptlet <<-EOS
          require "socket"
          server = TCPServer.new(0)
          begin
            server.accept
          rescue Exception
          end
        EOS
      end
      latch.await # spawned thread has started
      sleep 0.5 # give time for the new runtime to block on accept
    end
  end

  def create_runtimes(num_runtimes)
    mbean = java.lang.management.ManagementFactory.getMemoryMXBean
    expect do
      num_runtimes.times do
        runtime = org.jruby.Ruby.newInstance
        runtime.evalScriptlet <<-EOS
          # eat up some memory in each runtime
          $arr = 500000.times.map { |i| "foobarbaz\#{i}" }
        EOS
        yield runtime if block_given?
        runtime.tearDown(false)
        # Make sure GC can keep up
        while mbean.getObjectPendingFinalizationCount > 0
          sleep 0.2
        end
      end
    end.not_to raise_error
  end
end
