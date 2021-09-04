#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: hash2.ruby,v 1.2 2004-11-10 06:36:29 bfulgham Exp $
# http://shootout.alioth.debian.org/
# Revised by Dave Anderson

n = Integer(ARGV.shift || 1)

hash1 = {}
i = 0
for i in 0 .. 9999
    hash1["foo_" << i.to_s] = i
end

hash2 = Hash.new(0)
n.times do
    for i in hash1.keys
	hash2[i] += hash1[i]
    end
end

printf "%d %d %d %d\n",
    hash1["foo_1"], hash1["foo_9999"], hash2["foo_1"], hash2["foo_9999"]
