=begin
For a given benchmark BENCH,
Find a GOODCOMMIT (known good perf) and a BADCOMMIT (often just HEAD).
First measure the good perf:
$ git checkout GOODCOMMIT
$ jt build
$ jt bench reference BENCH
$ git checkout -

To have an idea how slower the bad part is:
$ jt build
$ jt bench compare BENCH

Then choose a THRESHOLD (between the number from the compare and 100%) and
$ git bisect start BADCOMMIT GOODCOMMIT
$ git bisect run ruby tool/truffle/bisect.rb BENCH THRESHOLD
=end

def jt(cmd)
  puts "jt #{cmd}"
  cmd = "ruby tool/jt.rb #{cmd}"
  output = `#{cmd}`
  raise "#{cmd} failed: #{$?}" unless $?.success?
  output
end

if ARGV.size < 2
  abort "Usage: #{$0} BENCH THRESHOLD\n" \
  "  The commit is considered good if relative performance is above the threshold (in %)"
end

bench = String(ARGV[0])
threshold = Integer(ARGV[1])

begin
  jt 'rebuild'
rescue
  puts "rebuild failed, skipping revision"
  exit 125
end

output = jt("bench compare #{bench}")

/^#{Regexp.escape bench} (\d+)\./ =~ output
raise "Unexpected output:\n#{output}" unless $1
percents = Integer($1)
puts percents

if percents < threshold
  puts "bad"
  exit 1
else
  puts "good"
  exit 0
end
