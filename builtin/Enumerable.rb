#
# Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
#
# JRuby - http://jruby.sourceforge.net
#
# This file is part of JRuby
#
# JRuby is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License as
# published by the Free Software Foundation; either version 2 of the
# License, or (at your option) any later version.
#
# JRuby is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public
# License along with JRuby; if not, write to
# the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
# Boston, MA  02111-1307 USA

# The Enumerable mixin provides collection classes with several traversal and
# searching methods, and with the ability to sort. The class must provide a
# method each, which yields successive members of the collection. If
# Enumerable#max, #min, or #sort is used, the objects in the collection must
# also implement a meaningful <=> operator, as these methods rely on an
# ordering between members of the collection.
module Enumerable
  
  def to_a
    result = []
    each do |item|
      result << item
    end
    result
  end
  alias entries to_a

  def sort
    #
    # Maybe block problem
    #
    to_a.sort
  end

  def sort_by
    result = []
    each do |item|
      result << [yield(item), item]
    end
    result.sort do |a, b|
      a.first <=> b.first
    end
    result.collect do |item|
      item.last
    end
  end

  def grep (pattern)
    result = []
    each do |item|
      if block_given? then
	result << yield(item) if pattern === item
      else
	result << item if pattern === item
      end
    end
    result
  end
  
  def detect (nothing_found = nil)
    each do |element|
      return element if yield(element)
    end
    nothing_found.call unless nothing_found.nil?
    nil
  end
  alias find detect

  def select
    result = []
    each do |item|
      result << item if yield(item)
    end
    result
  end
  alias find_all select

  def reject
    result = []
    each do |item|
      result << item unless yield(item)
    end
    result
  end

  def collect
    result = []
    each do |item|
      result << yield(item)
    end
    result
  end
  alias map collect

  def inject (*args)
    raise ArgumentError, "wrong number of arguments (#{args.length} for 1)" if args.length > 1
    if args.length == 1 then
      result = args[0]
      first = false
    else
      result = nil
      first = true
    end
    each do |item|
      if first then
	first = false
	result = item
      else
        result = yield(result, item)
      end
    end
    result
  end

  def partition
    result = [[], []]
    each do |item|
      if yield(item) then
	result[0] << item
      else
	result[1] << item
      end
    end
    result
  end

  def each_with_index
    index = 0
    each do |item|
      yield(item, index)
      index += 1
    end
  end

  def include? (value)
    each do |item|
      return true if item == value
    end
    false
  end
  alias member? include?

  def max
    if block_given? then
      cmp = lambda { |a, b| yield(a, b) > 0 }
    else
      cmp = lambda { |a, b| a <=> b > 0 }
    end
    result = nil
    each do |item|
      result = item if result.nil? || cmp.call(item, result)
    end
    result
  end

  def min
    if block_given? then
      cmp = lambda { |a, b| yield(a, b) < 0 }
    else
      cmp = lambda { |a, b| a <=> b < 0 }
    end
    result = nil
    each do |item|
      result = item if result.nil? || cmp.call(item, result)
    end
    result
  end

  def all?
    each do |item|
      return false unless yield(item)
    end
    true
  end

  def any?
    each do |item|
      return true if yield(item)
    end
    false
  end

  # WARNING this isn't a default ruby method
  def group_by
    result = {}
    each do |item|
      group = yield(item)
      result[group] = [] if result[group].nil?
      result[group] << item
    end
    result
  end
end
