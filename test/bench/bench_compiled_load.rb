require 'benchmark'
jrubyc = "jruby jrubyc"

def jrubyc(file)
    system "jrubyc -p 'ruby' #{file}"
end

def bench_compiled_load(bm)
  File.open('compiled_load_test_simple.rb', 'w') do |f|
    f.write '1'
  end
  File.open('compiled_load_test_simple_strings.rb', 'w') do |f|
    f.write '"foo"'
  end
  File.open('compiled_load_test_complex_flat.rb', 'w') do |f|
    f.write "
if false
#{"  foo 1\n" * 1000}
#{"  'asdf'\n" * 1000}
end
    "
  end
  File.open('compiled_load_test_complex_tree.rb', 'w') do |f|
    f.write "
if false
#{("def foo; " + ('1; ' * 100) + "end\n") * 10}
#{("def foo; " + ('"asdf"; ' * 100) + "end\n") * 10}
end
    "
  end
  
  File.open('compiled_load_test_many_defs.rb', 'w') do |f|
    f.write "
      def foo; end
      def bar; end
      def oof; end
      def rab; end
  "
  end
  
  jrubyc 'compiled_load_test_simple.rb'
  jrubyc 'compiled_load_test_simple_strings.rb'
  jrubyc 'compiled_load_test_complex_flat.rb'
  jrubyc 'compiled_load_test_complex_tree.rb'
  jrubyc 'compiled_load_test_many_defs.rb'
#  
  $: << '.'
  bm.report("control, 1000.times") { 1000.times {1} }

  bm.report("control, 10000 loads simple") do
    10000.times { load 'compiled_load_test_simple.rb' }
  end
  bm.report("control, 10000 loads simple_strings") do
    10000.times { load 'compiled_load_test_simple_strings.rb' }
  end
  bm.report("control, 100 loads complex flat") do
    100.times { load 'compiled_load_test_complex_flat.rb' }
  end
  bm.report("control, 100 loads complex tree") do
    100.times { load 'compiled_load_test_complex_tree.rb' }
  end
  bm.report("control, 1000 loads many defs") do
    1000.times { load 'compiled_load_test_many_defs.rb' }
  end

  bm.report("10000 loads simple compiled") do
    10000.times { load 'ruby/compiled_load_test_simple.class' }
  end
  bm.report("10000 loads simple strings compiled") do
    10000.times { load 'ruby/compiled_load_test_simple_strings.class' }
  end
  bm.report("100 loads complex flat compiled") do
    100.times { load 'ruby/compiled_load_test_complex_flat.class' }
  end
  bm.report("100 loads complex tree compiled") do
    100.times { load 'ruby/compiled_load_test_complex_tree.class' }
  end
  bm.report("1000 loads many defs compiled") do
    1000.times { load 'ruby/compiled_load_test_many_defs.class' }
  end
end

if __FILE__ == $0
  Benchmark.bmbm {|bm| bench_compiled_load(bm)}
  File.delete("compiled_load_test_simple.rb")
  File.delete("compiled_load_test_simple_strings.rb")
  File.delete("compiled_load_test_complex_flat.rb")
  File.delete("compiled_load_test_complex_tree.rb")
  File.delete("compiled_load_test_many_defs.rb")
  File.delete("ruby/compiled_load_test_simple.class")
  File.delete("ruby/compiled_load_test_simple_strings.class")
  File.delete("ruby/compiled_load_test_complex_flat.class")
  File.delete("ruby/compiled_load_test_complex_tree.class")
  File.delete("ruby/compiled_load_test_many_defs.class")  
end