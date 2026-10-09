require 'open3'
require 'rbconfig'

describe 'Ruby thread backing and startup' do
  # The backing thread factory is selected once per JVM, so test each option in
  # a fresh process, including the default without an explicit property.
  [nil, false, true].each do |virtual|
    ['-X-C', '-X+C'].each do |mode|
      it "initializes threads before running them (virtual=#{virtual.inspect}, #{mode})" do
        script = <<~RUBY
          require 'java'
          expected_virtual = #{virtual == true}
          group = ThreadGroup.new
          group.add(Thread.current)
          50.times do
            thread = Thread.handle_interrupt(RuntimeError => :never) do
              Thread.new do
                current = Thread.current
                native = java.lang.Thread.current_thread
                raise 'wrong backing thread' unless native.virtual? == expected_virtual
                raise 'not a daemon' unless native.daemon?
                raise 'missing name' unless native.name.start_with?('Ruby-')
                raise 'missing group' unless current.group.equal?(group)
                raise 'missing registration' unless Thread.list.include?(current)
                :finished
              end
            end
            raise 'wrong result' unless thread.value == :finished
            raise 'not terminated' unless thread.status == false
            raise 'group not cleaned up' if group.list.include?(thread)
          end
          Thread.current.priority = 2
          priority = Thread.new do
            raise 'priority not inherited' unless Thread.current.priority == 2
            Thread.current.priority = -2
            raise 'priority not retained' unless Thread.current.priority == -2
            Thread.new { Thread.current.priority }.value
          end.value
          raise 'child priority not inherited' unless priority == -2
          ready = Queue.new
          release = Queue.new
          masked = Thread.handle_interrupt(RuntimeError => :never) do
            Thread.new do
              ready << true
              release.pop
              :masked
            end
          end
          ready.pop
          masked.raise(RuntimeError, 'deferred')
          release << true
          raise 'interrupt mask not inherited' unless masked.value == :masked
          puts 'ok'
        RUBY
        options = [mode]
        options << "-Xthread.vthread=#{virtual}" unless virtual.nil?
        stdout, stderr, status = Open3.capture3({'JRUBY_OPTS' => nil}, RbConfig.ruby, *options, '-e', script)
        expect(status.success?).to eq(true), "#{stdout}\n#{stderr}"
        expect(stdout).to eq("ok\n")
        expect(stderr).to eq('')
      end
    end
  end
end
