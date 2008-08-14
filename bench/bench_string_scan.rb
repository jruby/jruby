require 'benchmark'

Benchmark.bmbm do |bm|
  bm.report("control") { str = '123456789'; 1_000_000.times { str; /.../ } }
  bm.report("String\#scan success") do
    str = '123456789'; 1_000_000.times { str.scan(/.../) }
  end
  bm.report("String\#scan fail") do
    str = '123456789'; 1_000_000.times { str.scan(/111/) }
  end
  bm.report("String\#scan success, large string (0.01 scale)") do
    str = '123456789' * 100; 10_000.times { str.scan(/.../) }
  end
  bm.report("String\#scan fail, large string (0.01 scale)") do
    str = '123456789' * 100; 10_000.times { str.scan(/111/) }
  end
end