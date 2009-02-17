##############################################################
# bench_hash.rb
#
# Benchmark suite for the Hash class and instance methods.
##############################################################
require "benchmark"

MAX = ARGV[0].chomp.to_i rescue 200000

Benchmark.bm(30) do |x|
   x.report("Hash[]"){
      MAX.times{ Hash["a","b","c","d"] }
   }

   x.report("Hash.new"){
      MAX.times{ Hash.new }
   }

   x.report("Hash.new(obj)"){
      MAX.times{ Hash.new(0) }
   }

   x.report("Hash.new{ block }"){
      MAX.times{ Hash.new{ |k,v| } }
   }

   x.report("Hash#=="){
      hash1 = {"a"=>1, "b"=>2, "c"=>3}
      hash2 = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash1 == hash2 }
   }

   x.report("Hash#[]"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash["a"] }
   }

   x.report("Hash#[]="){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash["a"] = 4 }
   }

   x.report("Hash#clear"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.clear }
   }

   x.report("Hash#default"){
      hash = Hash.new(0)
      MAX.times{ hash.default }
   }

   x.report("Hash#default(key)"){
      hash = Hash.new(0)
      MAX.times{ hash.default(2) }
   }

   x.report("Hash#default="){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.default = 5 }
   }

   x.report("Hash#default_proc"){
      hash = Hash.new{ |k,v| }
      MAX.times{ hash.default_proc }
   }

   x.report("Hash#delete(key)"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.delete("a") }
   }

   x.report("Hash#delete(key){ block }"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.delete("a"){ |e| } }
   }

   x.report("Hash#delete_if"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.delete_if{ |k,v| } }
   }

   x.report("Hash#each"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.each{ |e| } }
   }

   x.report("Hash#each_key"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.each_key{ |k| } }
   }

   x.report("Hash#each_value"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.each_value{ |v| } }
   }

   x.report("Hash#empty?"){
      hash = {}
      MAX.times{ hash.empty? }
   }

   x.report("Hash#fetch"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.fetch("a") }
   }

   x.report("Hash#fetch{ block }"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.fetch("a"){ |e| } }
   }

   x.report("Hash#has_key?"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.has_key?("b") }
   }

   x.report("Hash#has_value?"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.has_value?("b") }
   }

   x.report("Hash#index"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.index("b") }
   }

   x.report("Hash#invert"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.invert }
   }

   x.report("Hash#keys"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.keys } 
   }

   x.report("Hash#length"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.length } 
   }

   x.report("Hash#merge(hash)"){
      hash1 = {"a"=>1, "b"=>2, "c"=>3}
      hash2 = {"d"=>4, "e"=>5, "f"=>6}
      MAX.times{ hash1.merge(hash2) } 
   }

   x.report("Hash#merge(hash){ block }"){
      hash1 = {"a"=>1, "b"=>2, "c"=>3}
      hash2 = {"d"=>4, "e"=>5, "f"=>6}
      MAX.times{ hash1.merge(hash2){ |k,o,n| } }
   }

   x.report("Hash#merge!(hash){ block }"){
      hash1 = {"a"=>1, "b"=>2, "c"=>3}
      hash2 = {"d"=>4, "e"=>5, "f"=>6}
      MAX.times{ hash1.merge!(hash2){ |k,o,n| } }
   }

   # TODO: Make a better sample case for Hash#rehash
   x.report("Hash#rehash"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.rehash }
   }

   x.report("Hash#reject"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.reject{ |k,v| } }
   }

   x.report("Hash#reject!"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.reject!{ |k,v| } }
   }

   x.report("Hash#replace"){
      hash1 = {"a"=>1, "b"=>2, "c"=>3}
      hash2 = {"d"=>4, "e"=>5, "f"=>6}
      MAX.times{ hash1.replace(hash2) }
   }

   x.report("Hash#select"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.select{ |k,v| } }
   }

   x.report("Hash#shift"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.shift }
   }

   x.report("Hash#sort"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.sort{ |k,v| k <=> v } }
   }

   x.report("Hash#to_a"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.to_a }
   }

   x.report("Hash#to_hash"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.to_hash }
   }

   x.report("Hash#to_s"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.to_s }
   }

   x.report("Hash#values"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.values }
   }

   x.report("Hash#values_at"){
      hash = {"a"=>1, "b"=>2, "c"=>3}
      MAX.times{ hash.values_at("b") }
   }
end
