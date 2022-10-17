
# pass in any argument to assume we are processing for ripper instead
# of the main parser.
IS_RIPPER=!!ARGV.shift

def read_line
  line = gets
  return nil unless line
  
  line.gsub(/@@([^@]+)@@/) do |word|
    sub = SUBS[$1]
    raise ArgumentError, "Cannot find substitution for #{word}" unless sub
    sub[IS_RIPPER ? 1 : 0]
  end
end

# We define substitutions at the top of the file where a constant
# named SUBS contains a key which represents a subtition and two value
# where the first value is what is substituted when writing the Parser
# and the second value is what is substituted when writing Ripper Parser.
#
# Any reference to @@name@@ will be replaced with first or second value
# later on in the grammar file.
def read_text_substitutions
  code = ''
  while line = read_line
    break if line =~ %r{^=@@\*/}
    code << line
  end
  # Reads in substitions into the constant SUBS
  eval code
end

while line = read_line
  if line =~ %r{^/\*@@=}
    read_text_substitutions
  else
    puts line
  end
end

