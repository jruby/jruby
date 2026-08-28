# yaml_dump.rb [embed]

# no use of yaml since psych is not installed at this point of the tests

def dump
  puts @text.gsub( /\n/m, '' ).gsub( /-/, '')
end
