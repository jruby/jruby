# Copyright (c) 2014, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle
  def self.get_data(path, offset)
    file = File.open(path)
    file.seek(offset)

    # I think if the file can't be locked then we just silently ignore
    file.flock(File::LOCK_EX | File::LOCK_NB)

    Truffle::Kernel.at_exit true do
      file.flock(File::LOCK_UN)
    end

    file
  end
  
  def self.load_arguments_from_array_kw_helper(array, kwrest_name, binding)
    array = array.dup

    last_arg = array.pop

    if last_arg.respond_to?(:to_hash)
      kwargs = last_arg.to_hash

      if kwargs.nil?
        array.push last_arg
        return array
      end

      raise TypeError.new("can't convert #{last_arg.class} to Hash (#{last_arg.class}#to_hash gives #{kwargs.class})") unless kwargs.is_a?(Hash)

      return array + [kwargs] unless kwargs.keys.any? { |k| k.is_a? Symbol }

      kwargs.select! do |key, value|
        symbol = key.is_a? Symbol
        array.push({key => value}) unless symbol
        symbol
      end
    else
      kwargs = {}
    end

    binding.local_variable_set(kwrest_name, kwargs) if kwrest_name
    array
  end

  def self.add_rejected_kwargs_to_rest(rest, kwargs)
    return if kwargs.nil?

    rejected = kwargs.select do |key, value|
      not key.is_a?(Symbol)
    end

    unless rejected.empty?
      rest.push rejected
    end
  end
  
  def self.when_splat(cases, expression)
    cases.any? do |c|
      c === expression
    end
  end
end
