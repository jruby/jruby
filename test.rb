#
# test.rb - No description
# Created on 04. Juli 2001, 22:53
# 
# Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust
# Jan Arne Petersen <japetersen@web.de>
# Stefan Matthias Aust <sma@3plus4.de>
# 
# JRuby - http://jruby.sourceforge.net
# 
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or any later version.
# 
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
# 
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
# 

#Class test

class Top
  def Top.inherited(sub)
    print "New subclass: ", sub, "\n"
  end
end

class Middle < Top
end

class Bottom < Middle
end

#Array test

arr = ["zero", "first"]

arr.unshift "second", "third"

puts arr[1..4].inspect

arr << "fourth"

puts arr.pop();
puts arr.shift();

puts ["zero", "first"].type

puts

# test variables and method calls

a = String.new("Hello World")
b = a.reverse
c = " "
d = "Hello".reverse
e = a[6, 5].reverse

f = 100 + 35
g =  2 * 10
h = 13 % 5

puts(a)
puts b
puts d.reverse, c, e.reverse

puts f, " ", g, " ",  h

# test ifs

if (true)
   puts "True"
end

if (FALSE | true)
   puts "False"
end

# test loops

i = 0
j = 0
while (i < 10)
   break if (i == 6)

   j = j + 1
   puts i

   redo if (j < 2)

   j = 0
   i = i + 1
end

# test methods

def testMethod
    puts "method tested."
    "some output"
end

puts testMethod
puts

# test global variable.

$output = "a global variable."

def testGlobalVariable
    puts $output
end
testGlobalVariable

puts

# test classes

class Hello
    def saveHelloWorld
        @hello = "Hello World."
    end
    def sayHelloWorld
        puts @hello
    end
end

hello = Hello.new
hello.saveHelloWorld
hello.sayHelloWorld

# number test

puts 25.eql? 25
puts 20.between? 15, 25
puts 20.between? 10, 15

# Float test

puts "Float test\n"

puts 3.5 + 5.7
puts 3.0 * 3.3
puts 2.5 / 2.1
puts 2.5 ** 4
puts 2.56

# Block test

5.times do |i|
   print i, " "
end

puts

2.step 10, 2 do |i|
    print i, " "
end

puts

puts 78.chr

#Range test

(5..10).each do |i|
    print i, " * ", i, " = " , i * i, "\n"
end