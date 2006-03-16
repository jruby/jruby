###### BEGIN LICENSE BLOCK ######
# Version: CPL 1.0/GPL 2.0/LGPL 2.1
#
# The contents of this file are subject to the Common Public
# License Version 1.0 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a copy of
# the License at http://www.eclipse.org/legal/cpl-v10.html
#
# Software distributed under the License is distributed on an "AS
# IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# implied. See the License for the specific language governing
# rights and limitations under the License.
#
# Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
# Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
# Copyright (C) 2004 Charles O Nutter <headius@headius.com>
# 
# Alternatively, the contents of this file may be used under the terms of
# either of the GNU General Public License Version 2 or later (the "GPL"),
# or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# in which case the provisions of the GPL or the LGPL are applicable instead
# of those above. If you wish to allow use of your version of this file only
# under the terms of either the GPL or the LGPL, and not to allow others to
# use your version of this file under the terms of the CPL, indicate your
# decision by deleting the provisions above and replace them with the notice
# and other provisions required by the GPL or the LGPL. If you do not delete
# the provisions above, a recipient may use your version of this file under
# the terms of any one of the CPL, the GPL or the LGPL.
###### END LICENSE BLOCK ######

# The Enumerable mixin provides collection classes with several traversal and
# searching methods, and with the ability to sort. The class must provide a
# method each, which yields successive members of the collection. If
# Enumerable#max, #min, or #sort is used, the objects in the collection must
# also implement a meaningful <=> operator, as these methods rely on an
# ordering between members of the collection.
module Enumerable
  def to_a
    result = []
    each { |item| result << item }
    result
  end
  alias entries to_a

  def sort (&proc)
    to_a.sort &proc
  end

  def sort_by
    result = []
    each { |item| result << [yield(item), item] }
    result.sort! { |a, b| a.first <=> b.first }
    result.collect { |item| item.last }
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
    each { |element| return element if yield(element) }
    nothing_found.nil? ? nil : nothing_found.call
  end
  alias find detect

  def select
    result = []
    each { |item| result << item if yield(item) }
    result
  end
  alias find_all select

  def reject
    result = []
    each { |item| result << item unless yield(item) }
    result
  end

  def collect
    result = []
    each { |item| result << if block_given?; yield(item); else; item; end }
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
    each { |item| result[yield(item) ? 0 : 1] << item }
    result
  end

  def each_with_index
    index = 0
    each do |item|
      yield(item, index)
      index += 1
    end
    self
  end

  def include? (value)
    result = false
    each { |item| result = true if item == value }
    result
  end
  alias member? include?

  def max
    if block_given? then
      cmp = lambda { |a, b| yield(a, b) > 0 }
    else
      cmp = lambda { |a, b| (a <=> b) > 0 }
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
      cmp = lambda { |a, b| (a <=> b) < 0 }
    end
    result = nil
    each do |item|
      result = item if result.nil? || cmp.call(item, result)
    end
    result
  end

  def all?
    return all? {|obj| obj} unless block_given?

    result = true
    each { |item| result = false unless yield(item) }
    result
  end

  def any?
    return any? {|obj| obj} unless block_given?

    result = false
    each { |item| result = true if yield(item) }
    result
  end

  def zip(*args)
    zip = []
    i = 0
    each do |elem|
      array = [elem]
      args.each do |a| 
        array << a[i]
      end
      if block_given? then
        yield(array) 
      else 
        zip << array
      end
      i = i + 1
    end
    return nil if block_given?
    zip
  end

  # WARNING this isn't a default ruby method
  def group_by
    result = {}
    each do |item|
      (result[yield(item)] ||= []) << item
    end
    result
  end
end
