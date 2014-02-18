time_budget = nil
reference = false
verbose = false

args = ARGV.dup

while not args.empty?
  arg = args.shift

  case arg
  when "--reference"
    reference = true
  when "-s"
    time_budget = args.shift.to_i
  when "-m"
    time_budget = args.shift.to_i * 60
  when "-h"
    time_budget = args.shift.to_i * 60 * 60
  when "-v"
    verbose = true
  when "--help", "-help", "-h"
    puts "Note: use a system Ruby to run this script, not the development JRuby, as that seems to sometimes get in a twist"
    puts
    puts "Set a reference point, running for 5 minutes:"
    puts "    GRAAL_DIR=... ruby benchmark-compare.rb --reference -m 5"
    puts
    puts "Compare against that reference point:"
    puts "    GRAAL_DIR=... ruby benchmark-compare.rb"
    puts
    puts "  -s n  run for n seconds (default 60)"
    puts "  -m n  run for n minutes"
    puts "  -h n  run for n hours"
    puts
    puts "  -v    show all output"
    exit
  else
    puts "unknown argument " + arg
    exit
  end
end

if reference
  if time_budget.nil?
    time_budget = 60
  end
else
  reference_scores = {}

  if not time_budget.nil?
    puts "can't specify budget unless --reference"
    exit
  end

  File.open("reference.data").each do |line|
    key, value = line.split
    if key == "-s"
      time_budget = value.to_i
    else
      reference_scores[key] = value.to_f
    end
  end
end

if ENV["GRAAL_DIR"].nil? or not Dir.exist? File.expand_path(ENV["GRAAL_DIR"])
  puts "warning: couldn't find $GRAAL_DIR - set this to the path of graalvm-jdk1.8.0"
end

benchmarks = [
  "binary-trees",
  "fannkuch-redux",
  "mandelbrot",
  "n-body",
  "pidigits",
  "spectral-norm",
  "neural-net"
]

time_budget_per_run = time_budget / benchmarks.length
puts time_budget_per_run.to_s + "s for each benchmark"

scores = {}

benchmarks.each do |benchmark|
  execute = "JAVACMD=$GRAAL_DIR/bin/java ../../bin/jruby -J-server -J-d64 -X+T -Xtruffle.printRuntime=true"
  output = `#{execute} harness.rb -s #{time_budget_per_run} #{benchmark}.rb`
  score_match = /[a-z\-]+: (\d+\.\d+)/.match(output)
  if score_match.nil?
    score = 0
    puts benchmark + " error"
    puts output
  else
    if verbose
      puts output
    end
    
    score = score_match[1].to_f
    puts benchmark + " " + score.to_s
  end
  scores[benchmark] = score
end

if reference
  File.open("reference.data", "w") do |file|
    file.write("-s #{time_budget}\n")

    benchmarks.each do |benchmark|
      file.write("#{benchmark} #{scores[benchmark]}\n")
    end
  end
else
  puts "-------"
  benchmarks.each do |benchmark|
    increase = scores[benchmark] - reference_scores[benchmark]
    increase_percentage = increase / reference_scores[benchmark] * 100
    puts "#{benchmark.ljust(15)} #{increase_percentage.round(2).to_s.rjust(6)}%"
  end
end
