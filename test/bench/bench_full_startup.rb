require 'benchmark'

Benchmark.bm(50) do |bm|
  5.times do
    bm.report('launching JRuby client VM, indexed methods') do
      system "jruby -O -J-Djruby.indexed.methods=true -e \"require 'net/http'; require 'irb'\""
    end
  end
  5.times do
    bm.report('launching JRuby server VM, indexed methods') do
      system "jruby -J-server -O -J-Djruby.indexed.methods=true -e \"require 'net/http'; require 'irb'\""
    end
  end
  5.times do
    bm.report('launching JRuby client VM, individual methods') do
      system "jruby -O -e \"require 'net/http'; require 'irb'\""
    end
  end
  5.times do
    bm.report('launching JRuby server VM, individual methods') do
      system "jruby -J-server -O -e \"require 'net/http'; require 'irb'\""
    end
  end
end
