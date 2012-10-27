Dir.glob("test/test_*.rb").sort.reject{|t| t =~ /test_all/}.each {|t| require t }
