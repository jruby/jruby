require 'win32ole'
require 'benchmark'

COUNT = (ARGV[0] || 250).to_i
ITERATIONS = (ARGV[1] || 5).to_i

ie = WIN32OLE.new('InternetExplorer.Application')


ITERATIONS.times do
  printf "visible+name #{COUNT} of 32 calls: "
  puts Benchmark.measure {
    COUNT.times do 
      ie.visible; ie.name; ie.visible; ie.name; ie.visible; ie.name
      ie.visible; ie.name; ie.visible; ie.name; ie.visible; ie.name
      ie.visible; ie.name; ie.visible; ie.name; ie.visible; ie.name
      ie.visible; ie.name; ie.visible; ie.name; ie.visible; ie.name
      ie.visible; ie.name; ie.visible; ie.name; ie.visible; ie.name
      ie.visible; ie.name
    end
  }

  printf "visible=false #{COUNT} of 32 calls: "
  puts Benchmark.measure {
    COUNT.times do 
      ie.Visible = false; ie.Visible = false; ie.Visible = false; 
      ie.Visible = false; ie.Visible = false; ie.Visible = false; 
      ie.Visible = false; ie.Visible = false; ie.Visible = false; 
      ie.Visible = false; ie.Visible = false; ie.Visible = false; 
      ie.Visible = false; ie.Visible = false; ie.Visible = false; 
      ie.Visible = false; ie.Visible = false; ie.Visible = false; 
      ie.Visible = false; ie.Visible = false; ie.Visible = false; 
      ie.Visible = false; ie.Visible = false; ie.Visible = false; 
      ie.Visible = false; ie.Visible = false; ie.Visible = false; 
      ie.Visible = false; ie.Visible = false; ie.Visible = false; 
      ie.Visible = false; ie.Visible = false
    end
  }
end

ie.quit 



