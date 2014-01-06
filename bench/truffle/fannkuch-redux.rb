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

# http://benchmarksgame.alioth.debian.org/

# Contributed by Wesley Moxam

# Modified by Sokolov Yura aka funny_falcon

# http://benchmarksgame.alioth.debian.org/u64q/program.php?test=fannkuchredux&lang=yarv&id=1

def fannkuch(n)
  p = (0..n).to_a
  s = p.dup
  q = p.dup
  sign = 1
  sum = maxflips = 0
  while(true)
    # flip.

    if (q1 = p[1]) != 1
      q[0..-1] = p
      flips = 1
      until (qq = q[q1]) == 1
        q[q1] = q1
        if q1 >= 4
          i, j = 2, q1 - 1
          while i < j
            q[i], q[j] = q[j], q[i]
            i += 1
            j -= 1
          end
        end
        q1 = qq
        flips += 1
      end
      sum += sign * flips
      maxflips = flips if flips > maxflips # New maximum?

    end
    # Permute.

    if sign == 1
      # Rotate 1<-2.

      p[1], p[2] = p[2], p[1]
      sign = -1
    else
      # Rotate 1<-2 and 1<-2<-3.

      p[2], p[3] = p[3], p[2]
      sign = 1
      i = 3
      while i <= n && s[i] == 1
        #return [sum, maxflips] if i == n     # Out of permutations.
        return sum if i == n

        s[i] = i
        # Rotate 1<-...<-i+1.

        t = p.delete_at(1)
        i += 1
        p.insert(i, t)
      end
      s[i] -= 1  if i <= n
    end
  end
end

def warmup
  1000000.times do
    fannkuch(4)
  end
end

def sample
  fannkuch(9) == 8629
end

def name
  return "shootout-fannkuch-redux"
end
