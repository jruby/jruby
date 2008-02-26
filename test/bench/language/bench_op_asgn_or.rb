require 'benchmark'

def empty
end

def unexisting_var
  # aa-dv
  aa ||= 1; ab ||= 1; ac ||= 1; ad ||= 1; ae ||= 1; af ||= 1; ag ||= 1; ah ||= 1; ai ||= 1; aj ||= 1
  aa ||= 1; ab ||= 1; ac ||= 1; ad ||= 1; ae ||= 1; af ||= 1; ag ||= 1; ah ||= 1; ai ||= 1; aj ||= 1
  aa ||= 1; ab ||= 1; ac ||= 1; ad ||= 1; ae ||= 1; af ||= 1; ag ||= 1; ah ||= 1; ai ||= 1; aj ||= 1
  aa ||= 1; ab ||= 1; ac ||= 1; ad ||= 1; ae ||= 1; af ||= 1; ag ||= 1; ah ||= 1; ai ||= 1; aj ||= 1
  aa ||= 1; ab ||= 1; ac ||= 1; ad ||= 1; ae ||= 1; af ||= 1; ag ||= 1; ah ||= 1; ai ||= 1; aj ||= 1
end

def existing_var
  aa=1
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
end


def bench_op_asgn_or(bm)
bm.report "control: empty method" do
100_000.times { empty() }
end

bm.report "existing var" do
100_000.times { existing_var() }
end

bm.report "nonnexisting var:" do
100_000.times { unexisting_var() } 
end
end

if $0 == __FILE__
  (ARGV[0] || 10).to_i.times { Benchmark.bm(40) {|bm| bench_op_asgn_or(bm)} }
end