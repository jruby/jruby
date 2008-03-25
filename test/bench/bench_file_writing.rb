require 'benchmark'

str = "abc"*10
arr = [str, str,str,str,str,str,str,str,str,str,str,str,str,str,str,str]
outstr = arr.to_s

(ARGV[0] || 10).to_i.times do
  @log = open("one_out", (File::WRONLY | File::APPEND | File::CREAT))
  @log.sync = true

  Benchmark.bm(30) do |bm|
    bm.report("100k io.write") { 100_000.times { @log.write(outstr) }}
  end

  @log.close

  File.unlink("one_out")
end

