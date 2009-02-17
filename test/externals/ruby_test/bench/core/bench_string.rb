###############################################################
# bench_string.rb
#
# Benchmark suite for the String class and all its methods.
###############################################################
require "benchmark"

MAX = ARGV[0].chomp.to_i rescue 200000

Benchmark.bm(30) do |x|
   x.report("String.new"){
      MAX.times{ String.new("test") }
   }

   x.report("String#%"){
      string = "%05d"
      number = 123
      MAX.times{ string % number }
   }

   x.report("String#*"){
      string = "test"
      int = 3
      MAX.times{ string * int }
   }

   x.report("String#+"){
      string1 = "hello"
      string2 = "world"
      MAX.times{ string1 + string2 }
   }

   x.report("String#<<"){
      string1 = "hello"
      string2 = "world"
      MAX.times{ string1 << string2 }
   }

   x.report("String#<=>"){
      string1 = "abcdef"
      string2 = "abcdef"
      MAX.times{ string1 <=> string2 }
   }

   x.report("String#=="){
      string1 = "abc"
      string2 = "abcd"
      MAX.times{ string1 == string2 }
   }

   x.report("String#==="){
      string1 = "Hello"
      string2 = "HellO"
      MAX.times{ string1 === string2 }
   }

   x.report("String#=~"){
      string = "hello"
      MAX.times{ string =~ /\w+/ }
   }

   x.report("String#[int]"){
      string = "hello"
      MAX.times{ string[1] }
   }

   x.report("String#[int,int]"){
      string = "hello"
      MAX.times{ string[1,2] }
   }

   x.report("String#[range]"){
      string = "hello"
      MAX.times{ string[1..2] }
   }

   x.report("String#[regexp]"){
      string = "hello"
      MAX.times{ string[/\w+/] }
   }

   x.report("String#[regexp,int]"){
      string = "hello"
      MAX.times{ string[/\w+/,1] }
   }

   x.report("String#[string]"){
      string = "hello"
      MAX.times{ string["lo"] }
   }

   # TODO: Fix
   #x.report("String#~"){
   #   string = "hello"
   #   MAX.times{ ~ string }
   #}

   x.report("String#capitalize"){
      string = "hello"
      MAX.times{ string.capitalize }
   }

   x.report("String#capitalize!"){
      string = "hello"
      MAX.times{ string.capitalize! }
   }

   x.report("String#casecmp"){
      string1 = "hello"
      string2 = "HELLO"
      MAX.times{ string1.casecmp(string2) }
   }

   x.report("String#center"){
      string = "hello"
      MAX.times{ string.center(4) }
   }

   x.report("String#chomp"){
      string = "hello\n"
      MAX.times{ string.chomp }
   }

   x.report("String#chomp!"){
      string = "hello\n"
      MAX.times{ string.chomp! }
   }

   x.report("String#chop"){
      string = "hello"
      MAX.times{ string.chop }
   }

   x.report("String#chop!"){
      string = "hello"
      MAX.times{ string.chop! }
   }

   x.report("String#count(string)"){
      string = "hello"
      MAX.times{ string.count("lo") }
   }

   x.report("String#count(^string)"){
      string = "hello"
      MAX.times{ string.count("^l") }
   }

   x.report("String#crypt"){
      string = "hello"
      MAX.times{ string.crypt("sh") }
   }

   x.report("String#delete"){
      string = "hello"
      MAX.times{ string.delete("lo") }
   }

   x.report("String#delete!"){
      string = "hello"
      MAX.times{ string.delete!("lo") }
   }

   x.report("String#downcase"){
      string = "HELLO"
      MAX.times{ string.downcase }
   }

   x.report("String#downcase!"){
      string = "HELLO"
      MAX.times{ string.downcase! }
   }

   x.report("String#dump"){
      string = "hello&%"
      MAX.times{ string.dump }
   }

   x.report("String#each"){
      string = "hello\nworld"
      MAX.times{ string.each{ |e| } }
   }

   x.report("String#each_byte"){
      string = "hello"
      MAX.times{ string.each_byte{ |e| } }
   }

   x.report("String#empty?"){
      string = ""
      MAX.times{ string.empty? }
   }

   x.report("String#eql?"){
      string1= "hello"
      string2= "hello"
      MAX.times{ string1.eql?(string2) }
   }

   x.report("String#gsub(regexp, repl)"){
      string = "hello"
      MAX.times{ string.gsub(/[aeiou]/, '*') }
   }

   x.report("String#gsub(regexp){ block }"){
      string = "hello"
      MAX.times{ string.gsub(/./){ |s| } }
   }

   x.report("String#gsub!(regexp){ block }"){
      string = "hello"
      MAX.times{ string.gsub!(/./){ |s| } }
   }

   x.report("String#hex"){
      string = "0x0a"
      MAX.times{ string.hex }
   }

   x.report("String#include?"){
      string = "hello"
      MAX.times{ string.include?("lo") }
   }

   x.report("String#index(string)"){
      string = "hello"
      MAX.times{ string.index("e") }
   }

   x.report("String#index(string, offset)"){
      string = "hello"
      MAX.times{ string.index("e", -1) }
   }

   x.report("String#index(int)"){
      string = "hello"
      MAX.times{ string.index(1) }
   }

   x.report("String#index(int, offset)"){
      string = "hello"
      MAX.times{ string.index(1, -1) }
   }

   x.report("String#index(regexp)"){
      string = "hello"
      MAX.times{ string.index(/[aeiou]/) }
   }

   x.report("String#index(regexp, offset)"){
      string = "hello"
      MAX.times{ string.index(/[aeiou]/, -1) }
   }

   x.report("String#insert"){
      string = "hello"
      MAX.times{ string.insert(2, "world") }
   }

   x.report("String#intern"){
      string = "hello"
      MAX.times{ string.intern }
   }

   x.report("String#length"){
      string = "hello"
      MAX.times{ string.length }
   }

   x.report("String#ljust"){
      string = "hello"
      MAX.times{ string.ljust(10) }
   }

   x.report("String#lstrip"){
      string = "   hello"
      MAX.times{ string.lstrip }
   }

   x.report("String#lstrip!"){
      string = "   hello"
      MAX.times{ string.lstrip! }
   }

   x.report("String#match(regexp)"){
      string = "hello"
      MAX.times{ string.match(/lo/) }
   }

   x.report("String#match(string)"){
      string = "hello"
      MAX.times{ string.match("lo") }
   }
   
   x.report("String#oct"){
      string = "123"
      MAX.times{ string.oct }
   }

   x.report("String#replace"){
      string = "hello"
      MAX.times{ string.replace("world") }
   }

   x.report("String#reverse"){
      string = "hello"
      MAX.times{ string.reverse }
   }

   x.report("String#reverse!"){
      string = "hello"
      MAX.times{ string.reverse! }
   }

   x.report("String#rindex(string)"){
      string = "hello"
      MAX.times{ string.rindex("e") }
   }

   x.report("String#rindex(string, int)"){
      string = "hello"
      MAX.times{ string.rindex("e",1) }
   }

   x.report("String#rindex(int, int)"){
      string = "hello"
      MAX.times{ string.rindex(1,1) }
   }

   x.report("String#rindex(regexp)"){
      string = "hello"
      MAX.times{ string.rindex(/[aeiou]/) }
   }

   x.report("String#rindex(regexp, int)"){
      string = "hello"
      MAX.times{ string.rindex(/[aeiou]/, 1) }
   }

   x.report("String#rjust(width)"){
      string = "hello"
      MAX.times{ string.rjust(10) }
   }

   x.report("String#rjust(width, padding)"){
      string = "hello"
      MAX.times{ string.rjust(10, "-") }
   }

   x.report("String#rstrip"){
      string = "hello    "
      MAX.times{ string.rstrip }
   }

   x.report("String#rstrip!"){
      string = "hello    "
      MAX.times{ string.rstrip! }
   }

   x.report("String#scan"){
      string = "cruel world"
      MAX.times{ string.scan(/\w+/) }
   }

   x.report("String#scan{ block }"){
      string = "cruel world"
      MAX.times{ string.scan(/\w+/){ |w| } }
   }

   x.report("String#slice(int)"){
      string = "hello"
      MAX.times{ string.slice(1) }
   }

   x.report("String#slice(int, int)"){
      string = "hello"
      MAX.times{ string.slice(1,3) }
   }

   x.report("String#slice(range)"){
      string = "hello"
      MAX.times{ string.slice(1..3) }
   }

   x.report("String#slice(regexp)"){
      string = "hello"
      MAX.times{ string.slice(/ell/) }
   }

   x.report("String#slice(string)"){
      string = "hello"
      MAX.times{ string.slice("lo") }
   }

   x.report("String#split"){
      string = "now is the time"
      MAX.times{ string.split }
   }

   x.report("String#split(string)"){
      string = "now-is-the-time"
      MAX.times{ string.split("-") }
   }

   x.report("String#split(string, limit)"){
      string = "now-is-the-time"
      MAX.times{ string.split("-", 2) }
   }

   x.report("String#split(regexp)"){
      string = "now-is-the-time"
      MAX.times{ string.split(/\w+/) }
   }

   x.report("String#split(regexp, limit)"){
      string = "now-is-the-time"
      MAX.times{ string.split(/\w+/, 2) }
   }

   x.report("String#squeeze"){
      string = "foo    moo    hello"
      MAX.times{ string.squeeze }
   }

   x.report("String#squeeze(char)"){
      string = "foo    moo    hello"
      MAX.times{ string.squeeze(" ") }
   }

   x.report("String#squeeze!"){
      string = "foo    moo    hello"
      MAX.times{ string.squeeze! }
   }

   x.report("String#squeeze!(char)"){
      string = "foo    moo    hello"
      MAX.times{ string.squeeze!(" ") }
   }

   x.report("String#strip"){
      string = "   hello    "
      MAX.times{ string.strip }
   }

   x.report("String#strip!"){
      string = "   hello    "
      MAX.times{ string.strip! }
   }

   x.report("String#sub(pattern, repl)"){
      string = "hello"
      MAX.times{ string.sub(/[aeiou]/, '*') }
   }

   x.report("String#sub(pattern){ block }"){
      string = "hello"
      MAX.times{ string.sub(/./){ |s| } }
   }

   x.report("String#sub!(pattern, repl)"){
      string = "hello"
      MAX.times{ string.sub!(/[aeiou]/, '*') }
   }

   x.report("String#sub!(pattern){ block }"){
      string = "hello"
      MAX.times{ string.sub!(/./){ |s| } }
   }

   x.report("String#succ"){
      string = "hello"
      MAX.times{ string.succ }
   }

   x.report("String#succ!"){
      string = "hello"
      MAX.times{ string.succ! }
   }

   x.report("String#sum"){
      string = "now is the time"
      MAX.times{ string.sum }
   }

   x.report("String#sum(int)"){
      string = "now is the time"
      MAX.times{ string.sum(8) }
   }

   x.report("String#swapcase"){
      string = "Hello"
      MAX.times{ string.swapcase }
   }

   x.report("String#swapcase!"){
      string = "Hello"
      MAX.times{ string.swapcase! }
   }

   x.report("String#to_f"){
      string = "123.45"
      MAX.times{ string.to_f }
   }

   x.report("String#to_i"){
      string = "12345"
      MAX.times{ string.to_i }
   }

   x.report("String#to_i(base)"){
      string = "12345"
      MAX.times{ string.to_i(8) }
   }

   x.report("String#to_s"){
      string = "hello"
      MAX.times{ string.to_s }
   }

   x.report("String#to_str"){
      string = "hello"
      MAX.times{ string.to_str }
   }

   x.report("String#to_sym"){
      string = "hello"
      MAX.times{ string.to_sym }
   }

   x.report("String#tr"){
      string = "hello"
      MAX.times{ string.tr("el","ip") }
   }

   x.report("String#tr!"){
      string = "hello"
      MAX.times{ string.tr!("el","ip") }
   }

   x.report("String#tr_s"){
      string = "hello"
      MAX.times{ string.tr_s("l","r") }
   }

   x.report("String#tr_s!"){
      string = "hello"
      MAX.times{ string.tr_s!("l","r") }
   }
   
   # TODO: Add more variations for String#unpack
   x.report("String#unpack"){
      string = "hello"
      MAX.times{ string.unpack("A5") }
   }

   x.report("String#upcase"){
      string = "heLLo"
      MAX.times{ string.upcase }
   }

   x.report("String#upcase!"){
      string = "heLLo"
      MAX.times{ string.upcase! }
   }

   x.report("String#upto"){
      string = "a1"
      MAX.times{ string.upto("b6"){ |s| } }
   }
end
