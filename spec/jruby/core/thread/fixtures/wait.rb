require 'java'
require 'rbconfig'
require 'rspec/autorun'

virtual = ARGV.shift == 'true'

RSpec.describe "Process waits with thread.vthread=#{virtual}" do
  [:waitpid2, :detach].each do |method|
    it "preserves #{method} status without occupying the only virtual-thread carrier" do
      input, output = IO.pipe
      pid = Process.spawn(RbConfig.ruby, '--disable-gems', '-e', 'STDIN.read; exit 7',
                          in: input, out: File::NULL, err: File::NULL)
      input.close
      ready = Queue.new
      waiter = if method == :detach
        Process.detach(pid)
      else
        Thread.new do
          ready << true
          result = Process.waitpid2(pid)
          expect(Process.last_status).to equal(result.last)
          result
        end
      end
      ready.pop if method == :waitpid2
      expect(waiter.to_java(java.lang.Thread).virtual?).to eq(virtual)

      # The child cannot exit until another Ruby thread runs to close its input.
      expect(Thread.new { output.close; :closed }.value).to eq(:closed)
      result = waiter.value
      if method == :waitpid2
        expect(result.first).to eq(pid)
        status = result.last
      else
        status = result
        expect(waiter.pid).to eq(pid)
      end
      expect(status.pid).to eq(pid)
      expect(status.exitstatus).to eq(7)
      expect(Process.last_status).to eq(nil)

      expect do
        Thread.new do
          Thread.current.report_on_exception = false
          Process.waitpid(pid)
        end.value
      end.to raise_error(Errno::ECHILD)
    ensure
      input&.close unless input&.closed?
      output&.close unless output&.closed?
      waiter&.join
    end
  end
end
