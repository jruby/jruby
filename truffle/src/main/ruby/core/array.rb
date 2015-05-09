# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

class Array

  def element_reference_fallback(method_name, args)
    if args.length == 1
      arg = args.first
      case arg
      when Range
        unless arg.begin.respond_to?(:to_int)
          raise TypeError, "no implicit conversion of #{arg.begin.class} into Integer"
        end
        unless arg.end.respond_to?(:to_int)
          raise TypeError, "no implicit conversion of #{arg.end.class} into Integer"
        end
        start_index = arg.begin.to_int
        end_index = arg.end.to_int
        if start_index.is_a?(Bignum) || end_index.is_a?(Bignum)
          raise RangeError, "bignum too big to convert into `long'"
        end
        if arg.exclude_end?
          range = start_index...end_index
        else
          range = start_index..end_index
        end
        send(method_name, range)
      when Bignum
        raise RangeError, "bignum too big to convert into `long'"
      else
        send(method_name, arg.to_int)
      end
    else
      start_index = args[0].to_int
      end_index = args[1].to_int
      if start_index.is_a?(Bignum) || end_index.is_a?(Bignum)
        raise RangeError, "bignum too big to convert into `long'"
      end
      send(method_name, start_index, end_index)
    end
  end

  def sort!(&block)
    replace sort(&block)
  end

end
