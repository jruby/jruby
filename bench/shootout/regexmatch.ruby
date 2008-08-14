#!/usr/bin/ruby
# -*- mode: ruby -*-
# $Id: regexmatch.ruby,v 1.2 2005-05-17 05:20:31 bfulgham Exp $
# http://shootout.alioth.debian.org/
# modified by: Jon-Carlos Rivera

re = Regexp.new(
    '(?:^|[^\d\(])' +			# must be preceeded by non-digit
    '(?:\((\d\d\d)\)|(\d\d\d))' +	# match 1 or 2: area code is 3 digits
    '[ ]' +				# area code followed by one space
    '(\d\d\d)' +			# match 3: prefix of 3 digits
    '[ -]' +				# separator is either space or dash
    '(\d\d\d\d)' +			# match 4: last 4 digits
    '\D'				# must be followed by a non-digit
)

num = Integer(ARGV[0] || 1)

phones = STDIN.readlines

phonenum, count = "", 0

(1..num).each do |iter|
  phones.each do |line|
	  if line =~ re 
	    phonenum = "(#{($1 || $2)}) #{$3}-#{$4}";
	    if iter == num
		    count += 1
		    puts "#{count}: #{phonenum}"
	    end
	  end
  end
end
