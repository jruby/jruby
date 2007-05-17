def recurse(i)
  begin
    recurse(i+1)
  rescue SystemStackError
    return i - 1
  end
end

puts "Maximum single-recursive stack depth: #{recurse(1)}"
