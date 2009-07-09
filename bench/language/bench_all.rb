require 'benchmark'

bench_list = %w[
  bench_alias
  bench_and
  bench_args_cat
  bench_args_push
  bench_array
  bench_attr_assign
  bench_backref
  bench_begin
  bench_bignum
  bench_break
  bench_case
  bench_class_definition
  bench_classvars
  bench_colon
  bench_const_lookup
  bench_def_method
  bench_defined
  bench_dregexp
  bench_dstr
  bench_dsymbol
  bench_false
  bench_fixnum
  bench_flip
  bench_float
  bench_for_loop
  bench_globals
  bench_ivar_access
  bench_literal_hash
  bench_lvar
  bench_method_dispatch
  bench_method_return
  bench_op_asgn_or
  bench_op_assign
  bench_op_element_asgn
  bench_range_literal
  bench_symbol
  bench_true
  bench_yield
]

bench_list.each do |bench_name|
  load File.join(File.dirname(__FILE__), bench_name + '.rb')
end

Benchmark.bm(50) do |bm|
  bench_list.each do |bench_name|
    bm.report(" #{bench_name.upcase}") {}
    ARGV[0].to_i.times do |i|
      send bench_name.intern, bm
      bm.report(" ----------------") {}
    end
  end
end

# if (ARGV[0] && ARGV[0].to_i > 0)
#   ARGV[0].to_i.times do |i|
#     puts unless i == 0
#     puts "Iteration: #{i + 1}"
#     puts "------------------------"
#     Benchmark.bm(50) do |bm|
#       bench_list.each do |bench_name|
#         bm.report(" #{bench_name.upcase}") {}
#         send bench_name.intern, bm
#       end
#     end
#   end
# else
#   Benchmark.bmbm(50) do |bm|
#     bench_list.each do |bench_name|
#       bm.report(" #{bench_name.upcase}") {}
#       send bench_name.intern, bm
#     end
#   end
# end
