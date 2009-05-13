require 'benchmark'
require 'strscan'
 
s = "this " * 20
 
(ARGV[0] || 1).to_i.times do
  Benchmark.bm(30) do |bm|
    bm.report "control" do
      100000.times do
        s || /\w+[ $]/
      end
    end    

    bm.report "with .scan.each" do
      100000.times do
        s.scan(/\w+[ $]/)
      end
    end

    bm.report "with StringScanner" do
      100000.times do
        parts = []
        ss = StringScanner.new(s)
        while part = ss.scan(/\w+[ $]/)
          parts << part
        end
      end
    end
  end
end
