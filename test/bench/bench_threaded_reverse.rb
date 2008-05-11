require 'thread'

class ThreadScalability
  def initialize()
    @mutex = Mutex.new
    @cond_var = ConditionVariable.new
    @num_ready_threads = 0
    @num_threads = 0
    @array_size = 120
    @array = []
    value = "0123456789" * 10000
    @array_size.times {|count|
      @array[count] = value.clone
    }
  end

  def reverse_string(s)
    len = s.length
    (len/2).times { |i|
      tmp = s[i]
      s[i] = s[len -1 - i]
      s[len -1 -i] = tmp
    }
    return s
  end

  def do_test()
    concurrency = @num_threads
    num_ops_per_thread = @array_size/@num_threads
    threads = []
    puts "concurrency is - #{concurrency}"
    concurrency.times { |i|
      threads << Thread.new {
        puts "started thread #{i}"
        
        # make sure all the threads start at the same time.
        @mutex.synchronize {
          @num_ready_threads += 1
          @cond_var.wait(@mutex) while @num_ready_threads < @num_threads
          @cond_var.signal if @num_ready_threads == @num_threads
        }
        
        puts "Thread #{i} running"

        #now do the work.
        reverse_strings(i*num_ops_per_thread, (i+1)*num_ops_per_thread - 1)

        puts "Thread #{i} done"
      }
    }
    threads.each {|t|
      t.join
    }
  end

  #to reverse a range of String elements in @array.
  def reverse_strings(beginIndex, endIndex)
    while beginIndex <= endIndex
      puts "another 10 in #{Thread.current.inspect}" if beginIndex % 10 == 0
      reverse_string(@array[beginIndex])
      beginIndex += 1
    end
  end

  def run(argument)
    count = argument.to_i
    @num_threads = count

    t = Time.now
    do_test()
    puts "Time: #{Time.now - t}"
  end

end

5.times {
  ThreadScalability.new.run(ARGV[0].to_i)
}
