require 'benchmark'

amount = 100000
script = <<EOF
a = 1 + 1
EOF

long_script = script * 100

bnd = binding

(ARGV[0] || 1).to_i.times do
  Benchmark.bm(40) do |bm|
    bm.report("Control") { amount.times { 1 + 1 } }
    bm.report("Binding creation") { amount.times { binding } }
    bm.report("Implicit binding (short)") { amount.times { eval script } }
    bm.report("Explicit binding (short)") { amount.times { eval script, bnd } }
    bm.report("Implicit binding (long, * 0.1 loops)") { (amount/10).times { eval long_script } }
    bm.report("Explicit binding (long, * 0.1 loops)") { (amount/10).times { eval long_script, bnd } }
  end
end
