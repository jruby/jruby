#######################################################################
# bench_array.rb
#
# Benchmark suite for the Array methods.  Deprecated methods and
# aliases are not benchmarked. You may pass the maximum number of
# iterations as an argument. The default is 200,000.
#######################################################################
require "benchmark"

MAX = ARGV[0].chomp.to_i rescue 200000

Benchmark.bm(35) do |x|
   x.report("Array[]"){
      MAX.times{ Array["a", 1, "b", true, nil] }
   }

   x.report("Array.new(int)"){
      MAX.times{ Array.new(3) }
   }

   x.report("Array.new(int, obj)"){
      MAX.times{ Array.new(3, "hi") }
   }

   x.report("Array.new(array)"){
      MAX.times{ Array.new([1,2,3]) }
   }

   x.report("Array.new(size){ block }"){
      MAX.times{ Array.new(3){|i|} }
   }

   x.report("Array#&"){
      array1 = [1,2,3]
      array2 = [3,4,5]
      MAX.times{ array1 & array2 }
   }

   x.report("Array#* (int)"){
      array = [1,2,3]
      MAX.times{ array * 10 }
   }

   x.report("Array#* (join)"){
      array = [1,2,3]
      MAX.times{ array * "-" }
   }

   x.report("Array#-"){
      array1 = [1,2,3,4,1,2,3]
      array2 = [2,3,4]
      MAX.times{ array1 - array2 }
   }

   x.report("Array#<<"){
      array = [1,2,3]
      MAX.times{ array << "a" }
   }

   x.report("Array#<=>"){
      array1 = ["a", "b", "c"]
      array2 = [1, 2, 3]
      MAX.times{ array1 <=> array2 }
   }

   x.report("Array#=="){
      array1 = [1, 2, 3]
      array2 = [1, 2, "3"]
      MAX.times{ array1 == array2 }
   }

   x.report("Array#[]"){
      array = ["a", 1, "b", true, nil]
      MAX.times{ array[2] }
   }

   x.report("Array#[]="){
      array = [0, 1, 2]
      MAX.times{ array[1] = 5 }
   }

   x.report("Array#|"){
      array1 = [1, 2, 3]
      array2 = [3, 4, 5]
      MAX.times{ array1 | array2 }
   }

   x.report("Array#assoc"){
      array1 = ["a", "b", "c"]
      array = [array1]
      MAX.times{ array.assoc("a") }
   }
   
   x.report("Array#at"){
      array = ["a", 1, "b", true, nil]
      MAX.times{ array.at(2) }
   }

   x.report("Array#clear"){
      array = [1, 2, 3]
      MAX.times{ array.clear }
   }

   x.report("Array#collect"){
      array = [1,2,3,4]
      MAX.times{ array.collect{ |e| } }
   }

   x.report("Array#collect!"){
      array = [1,2,3,4]
      MAX.times{ array.collect!{ |e| } }
   }

   x.report("Array#compact"){
      array = [1, nil, "two", nil, false]
      MAX.times{ array.compact }
   }

   x.report("Array#compact!"){
      array = [1, nil, "two", nil, false]
      MAX.times{ array.compact! }
   }

   x.report("Array#concat"){
      array1 = ["a", "b"]
      array2 = ["c", 1, "d"]
      MAX.times{ array1.concat(array2) }
   }

   x.report("Array#delete(obj)"){
      array = ["a", "b", 1]
      MAX.times{ array.delete("b") }
   }

   x.report("Array#delete(obj){ block }"){
      array = ["a", "b", 1]
      MAX.times{ array.delete("b"){ 1 } }
   }

   x.report("Array#delete_at"){
      array = [1, 2, 3, 4]
      MAX.times{ array.delete_at(2) }
   }

   x.report("Array#delete_if"){
      array = [1, 2, 3, 4]
      MAX.times{ array.delete_if{ |e| e == 3 } }
   }

   x.report("Array#each"){
      array = [1, 2, 3, 4]
      MAX.times{ array.each{ |e| } }
   }

   x.report("Array#each_index"){
      array = [1, 2, 3, 4, 5]
      MAX.times{ array.each_index{ |i| } }
   }

   x.report("Array#empty?"){
      array = []
      MAX.times{ array.empty? }
   }

   x.report("Array#eql?"){
      array1 = ["a", "b", "c"]
      array2 = ["a", "b", "c"]
      MAX.times{ array1.eql?(array2) }
   }

   x.report("Array#fetch(index)"){
      array = [1, 2, 3, 4, 5]      
      MAX.times{ array.fetch(0) }
   }

   x.report("Array#fetch(index, default)"){
      array = [1, 2, 3, 4, 5]      
      MAX.times{ array.fetch(9, "a") }
   }

   x.report("Array#fetch(index){ block }"){
      array = [1, 2, 3, 4, 5]      
      MAX.times{ array.fetch(9){ |i| } }
   }

   x.report("Array#fill(obj)"){
      array = ["a", "b", "c", "d"]
      MAX.times{ array.fill("x") }
   }

   x.report("Array#fill(obj, start)"){
      array = ["a", "b", "c", "d"]
      MAX.times{ array.fill("x", 2) }
   }

   x.report("Array#fill(obj, start, length)"){
      array = ["a", "b", "c", "d"]
      MAX.times{ array.fill("x", 0, 1) }
   }

   x.report("Array#fill(obj, range)"){
      array = ["a", "b", "c", "d"]
      MAX.times{ array.fill("x", 0..1) }
   }

   x.report("Array#fill{ block }"){
      array = ["a", "b", "c", "d"]
      MAX.times{ array.fill{ 7 } }
   }

   x.report("Array#fill(start){ block }"){
      array = ["a", "b", "c", "d"]
      MAX.times{ array.fill(-3){ 7 } }
   }

   x.report("Array#fill(start, length){ block }"){
      array = ["a", "b", "c", "d"]
      MAX.times{ array.fill(0,1){ 7 } }
   }

   x.report("Array#fill(range){ block }"){
      array = ["a", "b", "c", "d"]
      MAX.times{ array.fill(0..1){ 7 } }
   }

   x.report("Array#first"){
      array = [1,2,3]
      MAX.times{ array.first }
   }

   x.report("Array#flatten"){
      array = [[1,2,3],[4,5,6]]
      MAX.times{ array.flatten }
   }

   x.report("Array#flatten!"){
      array = [[1,2,3],[4,5,6]]
      MAX.times{ array.flatten! }
   }

   x.report("Array#include?"){
      array = ["one", 2, "three", nil]
      MAX.times{ array.include?("three") }
   }

   x.report("Array#index"){
      array = [1,2,3,"four"]
      MAX.times{ array.include?("four") }
   }

   # Pathological in Ruby 1.8.3 and earlier. Still bad in Ruby 1.8.4 or later.
   x.report("Array#insert"){
      array = [1,2,3,4]
      MAX.times{ array.insert(2, "a", "b") }
   }

   x.report("Array#join"){
      array = [1,2,3,4]
      MAX.times{ array.join }
   }

   x.report("Array#last"){
      array = [1,2,3,4]
      MAX.times{ array.last }
   }

   x.report("Array#length"){
      array = [1,2,3,4]
      MAX.times{ array.length }
   }

   x.report("Array#nitems"){
      array = [1, nil, "two", nil]
      MAX.times{ array.nitems }
   }

   # TODO: Add more variations of Array#pack later
   x.report("Array#pack"){
      array = ["a", "b", "c"]
      MAX.times{ array.pack("A3A3A3") }
   }

   x.report("Array#pop"){
      array = [1,2,3]
      MAX.times{ array.pop }
   }

   x.report("Array#push"){
      array = [1,2,3]
      MAX.times{ array.push("one","two","three") }
   }

   x.report("Array#rassoc"){
      array = [ [1,"one"], [2,"two"], [3,"three"], [4,"four"] ]
      MAX.times{ array.rassoc("two") }
   }

   x.report("Array#reject"){
      array = [1, 2, 3, 4]
      MAX.times{ array.reject{ |e| } }
   }

   x.report("Array#reject!"){
      array = [1, 2, 3, 4]
      MAX.times{ array.reject!{ |e| } }
   }

   x.report("Array#replace"){
      array = [1,2,3]
      MAX.times{ array.replace([4,5,6]) }
   }

   x.report("Array#reverse"){
      array = [1,2,3,4]
      MAX.times{ array.reverse }
   }

   x.report("Array#reverse!"){
      array = [1,2,3,4]
      MAX.times{ array.reverse! }
   }

   x.report("Array#reverse_each"){
      array = [1,2,3,4]
      MAX.times{ array.reverse_each{ |e| } }
   }

   x.report("Array#rindex"){
      array = [1,2,3,2]
      MAX.times{ array.rindex(2) }
   }

   x.report("Array#shift"){
      array = [1,2,3,4]
      MAX.times{ array.shift }
   }

   x.report("Array#slice(int)"){
      array = [1,2,3,4]
      MAX.times{ array.slice(2) }
   }

   x.report("Array#slice(start, length)"){
      array = [1,2,3,4,5]
      MAX.times{ array.slice(2,2) }
   }

   x.report("Array#slice(range)"){
      array = [1,2,3,4,5]
      MAX.times{ array.slice(2..4) }
   }

   x.report("Array#slice!(int)!"){
      array = [1,2,3,4]
      MAX.times{ array.slice!(2) }
   }

   x.report("Array#slice!(start, length)"){
      array = [1,2,3,4,5]
      MAX.times{ array.slice!(2,2) }
   }

   x.report("Array#slice!(range)"){
      array = [1,2,3,4,5]
      MAX.times{ array.slice!(2..4) }
   }

   x.report("Array#sort"){
      array = [2,3,1,4]
      MAX.times{ array.sort }
   }

   x.report("Array#sort{ block }"){
      array = [2,3,1,4]
      MAX.times{ array.sort{ |a,b| a <=> b } }
   }

   x.report("Array#sort!"){
      array = [2,3,1,4]
      MAX.times{ array.sort! }
   }

   x.report("Array#sort!{ block }"){
      array = [2,3,1,4]
      MAX.times{ array.sort!{ |a,b| a <=> b } }
   }

   x.report("Array#to_a"){
      array = [1,2,3,4]
      MAX.times{ array.to_a }
   }

   x.report("Array#to_ary"){
      array = [1,2,3,4]
      MAX.times{ array.to_ary }
   }

   x.report("Array#to_s"){
      array = [1,2,3,4]
      MAX.times{ array.to_s }
   }

   x.report("Array#transpose"){
      array = [ [1,2], [3,4], [5,6] ]
      MAX.times{ array.transpose }
   }

   x.report("Array#uniq"){
      array = [1,2,3,1,2,3,4]
      MAX.times{ array.uniq }
   }

   x.report("Array#uniq!"){
      array = [1,2,3,1,2,3,4]
      MAX.times{ array.uniq! }
   }

   x.report("Array#unshift"){
      array = [1,2,3,4]
      MAX.times{ array.unshift }
   }

   x.report("Array#values_at(int)"){
      array = [1,2,3,4]
      MAX.times{ array.values_at(0, 2) }
   }

   x.report("Array#values_at(range)"){
      array = [1,2,3,4]
      MAX.times{ array.values_at(0..2) }
   }
end
