# Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Kernel

  def itself
    self
  end

  module_function

  def p(*args)
    args.each do |arg|
      print arg.inspect
      print "\n"
    end

    args.size <= 1 ? args.first : args
  end

  def puts(*args)
    print "\n" if args.empty?
    args.each do |arg|
      if arg.is_a?(Array)
        arg.each do |child|
          puts child
        end
      else
        print arg
        print "\n"
      end
    end
  end

  def printf(*args)
    puts sprintf(*args)
  end

end

# Here temporarily

class Hash

  def include?(key)
    keys.include? key
  end

end
