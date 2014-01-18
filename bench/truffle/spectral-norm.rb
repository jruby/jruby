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
# Contributed by Sokolov Yura

# http://benchmarksgame.alioth.debian.org/u64q/program.php?test=spectralnorm&lang=yarv&id=1

def eval_A(i,j)
  return 1.0/((i+j)*(i+j+1)/2+i+1)
end

def eval_A_times_u(u)
        v, i = nil, nil
  (0..u.length-1).collect { |i|
                v = 0
    for j in 0..u.length-1
      v += eval_A(i,j)*u[j]
                end
                v
        }
end

def eval_At_times_u(u)
  v, i = nil, nil
  (0..u.length-1).collect{|i|
                v = 0
    for j in 0..u.length-1
      v += eval_A(j,i)*u[j]
                end
                v
        }
end

def eval_AtA_times_u(u)
  return eval_At_times_u(eval_A_times_u(u))
end

def spectral_norm(n)
  u=[1]*n
  for i in 1..10
          v=eval_AtA_times_u(u)
          u=eval_AtA_times_u(v)
  end
  vBv=0
  vv=0
  for i in 0..n-1
          vBv += u[i]*v[i]
          vv += v[i]*v[i]
  end

  Math.sqrt(vBv/vv)
end

def warmup
  100000.times do
    spectral_norm(5)
  end
end

def sample
  (spectral_norm(400) - 1.2742240813922308).abs < 0.000001
end

def name
  return "shootout-spectral-norm"
end
