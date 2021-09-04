describe "embedded runtimes" do
  it "should not leak runtimes after tearing them down" do
    num_runtimes = 10
    mbean = java.lang.management.ManagementFactory.getMemoryMXBean
    lambda do
      num_runtimes.times do
        instance = org.jruby.Ruby.newInstance
        instance.evalScriptlet <<-EOS
          # eat up some memory in each runtime
          $arr = 500000.times.map { |i| "foobarbaz\#{i}" }
        EOS
        instance.tearDown(false)
        # Make sure GC can keep up
        while mbean.getObjectPendingFinalizationCount > 0
          sleep 0.2
        end
      end
    end.should_not raise_error
  end
end
