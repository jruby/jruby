require 'rubygems'
require "rexml/document"

include REXML  # so that we don't have to prefix everything with REXML::...

STRING = <<EOF
  <mydoc>
    <someelement attribute="nanoo">Text, text, text</someelement>
  </mydoc>
EOF


#build a test case with enough iterations to reproduce the problem.
#Since it's a race condition it'll occur randomly throughout the test
#
#if you can't reproduce the problem on your computer, try to increase the number of iterations
th = []

100.times do  
  th << Thread.new {
    1000.times do
      doc = Document.new STRING
    end
  }
end
th.each do |t|
  t.join
end
