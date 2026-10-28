require('openssl')
describe "Concurrent unpacking of a packed Array" do
  CyclicBarrier = java.util.concurrent.CyclicBarrier

  it "does not corrupt the array contents" do
    threads = 8
    start_barrier = CyclicBarrier.new(threads + 1)
    end_barrier = CyclicBarrier.new(threads + 1)
    contexts = nil
    stop = false

    threads.times do |index|
      Thread.new do
        loop do
          break if stop
          start_barrier.await
          begin
            contexts[index].setup
          rescue TypeError
            nil
          end
          end_barrier.await
        end
      rescue java.util.concurrent.BrokenBarrierException
        # ignore
      end
    end

    poisoned = 20_000.times.count do
      alpn = %w[h2 http/1.1].freeze
      contexts = Array.new(threads) { OpenSSL::SSL::SSLContext.new.tap { |context| context.alpn_protocols = alpn } }
      start_barrier.await
      end_barrier.await
      alpn.any?(&:nil?)
    end

    expect(poisoned).to eq(0)

  ensure
    stop = true
    start_barrier.reset
    end_barrier.reset
    threads.each(&:kill) rescue nil
  end
end