require 'benchmark'

amount = 100000
script = <<EOF
1 + 1
EOF

bnd = binding

Benchmark.bmbm do |bm|
  bm.report("Control") { amount.times { 1 + 1 } }
  bm.report("Binding creation") { amount.times { binding } }
  bm.report("Implicit binding") { amount.times { eval script } }
  bm.report("Explicit binding") { amount.times { eval script, bnd } }
end
