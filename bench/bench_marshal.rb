require 'benchmark'

(ARGV[0] || 1).to_i.times do
  Benchmark.bm(40) do |bm|
    short_s = 'short string'
    long_s = short_s * 1000
    short_a = [1,2,3,4,5,6,7,8,9,10]
    long_a = short_a * 1000
    short_h = {}; 5.times {|i| short_h[i] = i}
    long_h = {}; 5000.times {|i| long_h[i] = i}
    short_s_dump = Marshal.dump(short_s)
    long_s_dump = Marshal.dump(long_s)
    short_a_dump = Marshal.dump(short_a)
    long_a_dump = Marshal.dump(long_a)
    short_h_dump = Marshal.dump(short_h)
    long_h_dump = Marshal.dump(long_h)

    bm.report("control") { 100_000.times { short_s } }
    bm.report("dump short string") { 100_000.times { Marshal.dump(short_s) } }
    bm.report("dump long string") { 100_000.times { Marshal.dump(long_s) } }
    bm.report("dump short array") { 100_000.times { Marshal.dump(short_a) } }
    bm.report("dump long array (0.01 * iters)") { 1_000.times { Marshal.dump(long_a) } }
    bm.report("dump short hash") { 100_000.times { Marshal.dump(short_h) } }
    bm.report("dump long hash (0.01 * iters)") { 1_000.times { Marshal.dump(long_h) } }
    bm.report("load short string") { 100_000.times { Marshal.load(short_s_dump) } }
    bm.report("load long string") { 100_000.times { Marshal.load(long_s_dump) } }
    bm.report("load short array") { 100_000.times { Marshal.load(short_a_dump) } }
    bm.report("load long array (0.01 * iters)") { 1_000.times { Marshal.load(long_a_dump) } }
    bm.report("load short hash") { 100_000.times { Marshal.load(short_h_dump) } }
    bm.report("load long hash (0.01 * iters)") { 1_000.times { Marshal.load(long_h_dump) } }
  end
end
