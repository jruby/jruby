#
# Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

module Enumerable
  def collect
    result = Array.new
    each do |element|
      result << yield(element)
    end
    result
  end

  def each_with_index
    index = 0
    each do |element|
      yield(element, index)
      index += 1
    end
  end

  def entries
    collect do |element|
      element
    end
  end

  def find
    each do |element|
      return element if yield(element)
    end
    nil
  end

  def select
    result = Array.new
    each do |item|
      result << item if yield(item)
    end
    result
  end

  def grep (expression)
    result = Array.new
    each do |item|
      if block_given? then
	result << yield(item) if expression === item
      else
	result << item if expression === item
      end
    end
    result
  end

  def include? (anItem)
    each do |item|
      return true if item == anItem
    end
    false
  end

  def max
    entries.sort.last
  end

  def min
    entries.sort.first
  end

  def reject
    entries.reject
  end

  def sort
    entries.sort
  end

  alias map collect
  alias detect find
  alias find_all select
  alias member? include?
  alias to_a entries
end