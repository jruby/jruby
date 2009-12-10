require 'benchmark'

BUFSIZE = 16 * 1024
(ARGV[0] || 1).to_i.times do
  Benchmark.bmbm(40) do |bm|
    bm.report("100 String#slice!(0)") do
      100.times do
        str = 0.chr * BUFSIZE
        BUFSIZE.times { str.slice!(0) }
      end
    end

    bm.report("100 String#slice!(0, 1)") do
      100.times do
        str = 0.chr * BUFSIZE
        BUFSIZE.times { str.slice!(0, 1) }
      end
    end

    bm.report("100 String#slice!(str.length-1, 1)") do
      100.times do
        str = 0.chr * BUFSIZE
        BUFSIZE.times { str.slice!(str.length-1, 1) }
      end
    end

    bm.report("100 String#slice!(0..1)") do
      100.times do
        str = 0.chr * BUFSIZE
        BUFSIZE.times { str.slice!(0, 1) }
      end
    end

    bm.report("100 String#slice!(str.length-1..-1)") do
      100.times do
        str = 0.chr * BUFSIZE
        BUFSIZE.times { str.slice!(str.length-1..-1) }
      end
    end
  end
end