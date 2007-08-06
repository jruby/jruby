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


puts "Empty control:"
5.times do 
  puts Benchmark.measure { 100_000.times { empty() } }
end

puts "Existing var:"
5.times do 
  puts Benchmark.measure { 100_000.times { existing_var() } }
end

puts "Unexisting var:"
5.times do 
  puts Benchmark.measure { 100_000.times { unexisting_var() } }
end
