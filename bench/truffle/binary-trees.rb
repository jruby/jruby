# Copyright © 2004-2013 Brent Fulgham
#
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
#   * Redistributions of source code must retain the above copyright notice,
#     this list of conditions and the following disclaimer.
#
#   * Redistributions in binary form must reproduce the above copyright notice,
#     this list of conditions and the following disclaimer in the documentation
#     and/or other materials provided with the distribution.
#
#   * Neither the name of "The Computer Language Benchmarks Game" nor the name
#     of "The Computer Language Shootout Benchmarks" nor the names of its
#     contributors may be used to endorse or promote products derived from this
#     software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
# FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
# DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
# SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
# CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

# The Computer Language Benchmarks Game
# http://benchmarksgame.alioth.debian.org
#
# contributed by Jesse Millikan
# Modified by Wesley Moxam

# http://benchmarksgame.alioth.debian.org/u64q/program.php?test=binarytrees&lang=yarv&id=1

def item_check(left, item, right)
  return item if left.nil?
  item + item_check(*left) - item_check(*right)
end

def bottom_up_tree(item, depth)
  return [nil, item, nil] unless depth > 0
  item_item = 2 * item
  depth -= 1
  [bottom_up_tree(item_item - 1, depth), item, bottom_up_tree(item_item, depth)]
end

def binary_trees(max_depth)
  sum = 0

  min_depth = 4

  max_depth = min_depth + 2 if min_depth + 2 > max_depth

  stretch_depth = max_depth + 1
  stretch_tree = bottom_up_tree(0, stretch_depth)

  sum += item_check(*stretch_tree)
  stretch_tree = nil

  long_lived_tree = bottom_up_tree(0, max_depth)

  min_depth.step(max_depth + 1, 2) do |depth|
    iterations = 2**(max_depth - depth + min_depth)

    check = 0

    for i in 1..iterations
      temp_tree = bottom_up_tree(i, depth)
      check += item_check(*temp_tree)

      temp_tree = bottom_up_tree(-i, depth)
      check += item_check(*temp_tree)
    end

    sum += depth
    sum += check
  end

  sum += item_check(*long_lived_tree)

  sum
end

def warmup
  10000.times do
    binary_trees(1)
  end
end

def sample
  binary_trees(15) == -87308
end

def name
  return "shootout-binary-trees"
end
