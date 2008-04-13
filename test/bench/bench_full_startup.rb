require 'benchmark'

Benchmark.bm(50) do |bm|
  5.times do
    bm.report('launching JRuby client VM') do
      system "jruby -J-client -e \"require 'net/http'; require 'irb'\""
    end
  end
  5.times do
    bm.report('launching JRuby server VM') do
      system "jruby -J-server -e \"require 'net/http'; require 'irb'\""
    end
  end
  5.times do
    bm.report('launching JRuby client VM, no verify') do
      system "jruby -J-client -J-Xverify:none -e \"require 'net/http'; require 'irb'\""
    end
  end
  5.times do
    bm.report('launching JRuby server VM, no verify') do
      system "jruby -J-server -J-Xverify:none -e \"require 'net/http'; require 'irb'\""
    end
  end
end
