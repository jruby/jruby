# list_printer_1.rb [embed]

def print_list(list)
  print list.to_a.sort.join(" >> ")
  puts ": #{list.size} in total"
end