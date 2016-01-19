# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

=begin
Run first with JRuby+Truffle, then with MRI:

$ .../jruby -X+T test_methods_parity.rb > truffle.methods
$ .../ruby test_methods_parity.rb truffle.methods > mri.methods
Compare with:
$ git diff -U10 --no-index mri.methods truffle.methods
  Red is what we don't implement yet,
  Green is methods not existing in MRI (which should be fixed!)
=end

start = BasicObject # Numeric

modules = Object.constants.sort.map { |c|
  Object.const_get(c)
}.select { |v|
  Module === v and !v.instance_methods(false).empty?
}.select { |mod|
  !mod.name.end_with?("Error")
}.select { |mod|
  mod <= start
}

if RUBY_ENGINE == "ruby" and truffle_file = ARGV.first
  truffle_modules = File.readlines(truffle_file).map(&:chomp).grep(/^[A-Z]/)
  modules = modules.select { |mod| truffle_modules.include? mod.name }
end

modules.each do |mod|
  puts
  puts mod.name
  puts mod.instance_methods(false).sort
end
