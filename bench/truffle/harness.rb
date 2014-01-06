$:.unshift File.expand_path(File.dirname(__FILE__))

require "lib/deterministic-random"

sample_time = 30
benchmark = nil
show_warmup = false

args = ARGV.dup

while not args.empty?
  arg = args.shift

  if arg.start_with? "-"
    case arg
    when "-s"
      sample_time = args.shift.to_i
    when "--show-warmup"
      show_warmup = true
    when "--help", "-help", "-h"
      puts "benchmark.rb benchmark-name [-s n]"
      puts "  -s n  sample for n seconds (default 30)"
      exit
    else
      puts "unknown argument " + arg
      exit
    end
  else
    if benchmark != nil
      puts "you can only specify one benchmark"
      exit
    else
      benchmark = arg
    end
  end
end

load benchmark

if defined? before_warmup
  before_warmup
end

if show_warmup
  puts "warmup started"
  start = Time.now
end

warmup

if show_warmup
  elapsed = (Time.now - start) * 1000
  puts "warmup (" + name + ") finished = " + elapsed.to_s + " ms"
end

if defined? before_sample
  before_sample
end

iterations = 0

start = Time.now

while true
  if not sample
    puts "result not correct"
  end

  iterations += 1

  elapsed = Time.now - start

  if elapsed >= sample_time
    break
  end
end

score = iterations / elapsed * 1000

puts name + ": " + score.to_s
