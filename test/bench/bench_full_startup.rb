require 'benchmark'

Benchmark.bm(30) do |bm|
  5.times do
    bm.report('launching JRuby client VM') do
      system "jruby -O -e \"require 'net/http'; require 'irb'\""
    end
  end
  5.times do
    bm.report('launching JRuby server VM') do
      system "jruby -J-server -O -e \"require 'net/http'; require 'irb'\""
    end
  end
end
