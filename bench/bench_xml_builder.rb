require "rubygems"
require "builder"
 
require "rbench"
 
20_000.times do
  b = Builder::XmlMarkup.new
  b.date do
    b.year "2008"
    b.month "December"
    b.day "2"
  end
  
end
 
RBench.run do
  report("simple") do
    200_000.times do
      b = Builder::XmlMarkup.new
      b.date do
        b.year "2008"
        b.month "December"
        b.day "2"
      end
    end
  end
end
