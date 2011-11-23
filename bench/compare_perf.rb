#revisions = [['080c52', 'IR: pre major-refactoring + end of big bug fix pass + JGraphT use', "jruby.ir"],
#             ['45417e', 'IR: last IR mod before India trip + end of big refactoring + performance fix pass'],
#             ['3561ca', 'mainline fixes (no IR mods)'],
#             ['8ba592', 'IR: first botched tmp var fix'],
#             ['2a940e', 'additional mainline fixes (no IR mods)'],
#             ['7daf19', 'IR: removed className from IR interp'],
#             ['fa3ecc', 'IR: fixed tmp var compacting'],
#             ['455d41', 'IR: conservative live var/dead code'],
#             ['88a91a', 'IR: several tweaks'],
#             ['HEAD', 'IR: more tweaks']]

revisions = [['8c5c8b', 'IR: Virtual call interp loop'],
             ['59ebe1', 'IR: Switch table interp loop'],
             ['e7c82c', 'IR: Eliminate Operand.store'],
             ['af3d1d', 'IR: Push dynamic scope as a param down into operands']]

bms = ["bench/bench_sieve.rb",
       "-S gem list > /dev/null",
       "bench/bench_fib_recursive.rb",
       "bench/language/bench_method_dispatch.rb 1",
       "bench/bm1.rb 3"]

jruby_runs = [["JIT", "-X+C"],
              ["AST", "-X-C"],
              ["IR", "-X-CIR"],
              ["IR w/ dead code pass", "-X-CIR -J-Djruby.ir.pass.dead_code=true"]]

revisions.each { |r|
  puts "##### Checking out revision #{r[0]}: #{r[1]} #####"
  cmd_prefix = "time jruby --server -J-Xms512m -J-Xmx2048m"
  system("cd ..; git checkout #{r[0]} > /dev/null; ant clean > /dev/null; ant > /dev/null")
  bms.each { |bm|
    puts "----- BM: #{bm} -----"
    jruby_runs.each { |run, flag|
      puts run
      system("#{cmd_prefix} #{flag} #{bm}")
      puts "---"
    }
  }
  puts "\n\n"
}
