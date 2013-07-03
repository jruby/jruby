# list_printer.rb [embed]

def print_list
  print @list.join(" >> ")
end
print_list
puts ": #{@list.size} in total"