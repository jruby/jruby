
describe "A fiber" do
  describe "that is running while its resuming thread receives an exception" do
    it "receives that exception" do
      fiber_exceptions = []
      thread_exceptions = []
      mutex = Mutex.new

      100.times do
        fiber_running = false
        t = Thread.new do
          begin
            f = Fiber.new do
              begin
                fiber_running = true
                sleep 10
              rescue => e
                mutex.synchronize do
                  fiber_exceptions << e
                end
              end
            end
            f.resume
          rescue => e
            mutex.synchronize do
              thread_exceptions << e
            end
          end
        end
        Thread.pass until fiber_running
        t.raise StandardError.new
        t.join
      end

      expect(fiber_exceptions.size).to eq(100)
      fiber_exceptions.each do |ex|
        expect(ex).to be_kind_of(StandardError)
      end

      expect(thread_exceptions).to eq []
    end
  end

  describe "that is paused in yield while it receives an exception" do
    it "raises that exception in its parent thread" do
      require 'timeout'

      fiber_exceptions = []
      thread_exceptions = []
      mutex = Mutex.new

      100.times do |i|
        t = Thread.new do
          begin
            f = Fiber.new do
              begin
                Timeout.timeout(0.01, StandardError) do
                  Fiber.yield
                  end
              rescue => e
                mutex.synchronize do
                  fiber_exceptions << e
                end
              end
            end
            f.resume
            sleep
          rescue Exception => e
            mutex.synchronize do
              thread_exceptions << e
            end
          end
        end
        t.join rescue nil
      end

      expect(thread_exceptions.size).to eq(100)
      thread_exceptions.each do |ex|
        expect(ex).to be_kind_of(StandardError)
      end

      expect(fiber_exceptions).to eq []
    end
  end
end
