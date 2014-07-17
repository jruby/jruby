Dir[File.expand_path("#{File.dirname(__FILE__)}/*.rb")].uniq.each {|file| require file }
 
