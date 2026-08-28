#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: heapsort.ruby,v 1.5 2005-04-14 15:59:37 igouy-guest Exp $
#
# The Great Computer Language Shootout
# http://shootout.alioth.debian.org/
#
# modified by Jabari Zakiya

IM = 139968
IA =   3877
IC =  29573

$last = 42.0
def gen_random (max) (max * ($last = ($last * IA + IC) % IM)) / IM end

def heapsort(n, ra)
    j = i = rra = 0
    l = (n >> 1) + 1
    ir = n - 1

    while (1) do
	if (l > 1) then
	    rra = ra.at(l -= 1)
	else
	    rra = ra.at(ir)
	    ra[ir] = ra.at(1)
	    if ((ir -= 1) == 1) then
		ra[1] = rra
		return
	    end
	end
	i = l
	j = l << 1
	while (j <= ir) do
	    if ((j < ir) and (ra.at(j) < ra.at(j+1))) then
		j += 1
	    end
	    if (rra < ra.at(j)) then
		ra[i] = ra.at(j)
		j += (i = j)
	    else
		j = ir + 1
	    end
	end
	ra[i] = rra
    end
end

N = Integer(ARGV.shift || 1)
ary = Array.new(N) { gen_random(1.0) }

heapsort(N, ary)

printf "%.10f\n", ary.last
