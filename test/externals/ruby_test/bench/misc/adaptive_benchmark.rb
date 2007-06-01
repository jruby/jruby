# This class was taken entirely from Mauricio Fernandez's blog at
# http://eigenclass.org/hiki.rb?adaptative+benchmark
# 
class AdaptiveBenchmark
   def initialize(len, runs)
      @field_len = len
      @min_runs = runs
   end
   
   def self.bm(len = 10, min_runs = 10) 
      puts "#{" " * len}        \tstddev  \truns\tinterval\tconfidence"
      yield new(len, min_runs)
   end

   REPORT_OPT = {:precision => 0.1, :confidence => 0.95}
   
   def report(name, options = {})
      old_sync, $stdout.sync = $stdout.sync, true
      opt = REPORT_OPT.clone.update(options)
      sample_avg = sample_variance = 0

      tms_to_total = lambda{|tms| tms.utime + tms.stime }
    
      take_sample = lambda do |i|
         GC.start
         t0 = tms_to_total[Process.times]
         yield
         exec_time = tms_to_total[Process.times] - t0
         new_sample_avg = sample_avg + (exec_time - sample_avg) / i
         if i == 1
            sample_avg = new_sample_avg
            next
         end
         sample_variance = (1 - 1.0/i) * sample_variance + (i+1) * (new_sample_avg - sample_avg) ** 2
         sample_avg = new_sample_avg
      end
    
      (1..@min_runs).each{|i| take_sample[i] }

      population_variance = 1.0 * @min_runs / (@min_runs - 1) * sample_variance
      a = opt[:precision] * sample_avg
      num_runs = (sample_variance / (a ** 2 * (1 - opt[:confidence]))).ceil
      total_runs = @min_runs+num_runs
      (@min_runs+1..total_runs+1).each{ |i| take_sample[i] }

      population_variance = 1.0 * total_runs / (total_runs - 1) * sample_variance
      puts "%-#{@field_len}s %8.6f\t%8.6f\t%-6d\t%8.6f\t%3d%%" % [name, sample_avg, Math.sqrt(population_variance), total_runs, a, opt[:confidence] * 100]
   ensure
      $stdout.sync = old_sync
   end
end