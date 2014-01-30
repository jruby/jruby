require 'timeout'

describe "A fiber" do
  describe "that is running while its resuming thread receives an exception" do
    it "receives that exception" do
      fiber_exceptions = []
      thread_exceptions = []
      100.times do
        fiber_running = false
        t = Thread.new do
          begin
            f = Fiber.new do
              fiber_running = true
              begin
                sleep 0.2
              rescue => e
                fiber_exceptions << e
              end
            end
            f.resume
          rescue => e
            thread_exceptions << e
          end
        end
        Thread.pass until fiber_running
        t.raise StandardError.new
        t.join
      end

      fiber_exceptions.size.should == 100
      fiber_exceptions.each do |ex|
        ex.should be_kind_of(StandardError)
      end

      thread_exceptions.should be_empty
    end
  end

  describe "that is paused in yield while it receives an exception" do
    it "raises that exception in its parent thread" do
      fiber_exceptions = []
      thread_exceptions = []

      100.times do |i|
        t = Thread.new do
          begin
            f = Fiber.new do
              begin
                Timeout.timeout(0.01, StandardError) do
                  Fiber.yield
                  end
              rescue => e
                fiber_exceptions << e
              end
            end
            f.resume
            sleep
          rescue Exception => e
            thread_exceptions << e
          end
        end
        t.join rescue nil
      end

      thread_exceptions.size.should == 100
      thread_exceptions.each do |ex|
        ex.should be_kind_of(StandardError)
      end

      fiber_exceptions.should be_empty
    end
  end
end