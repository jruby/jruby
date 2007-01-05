require 'test/minirunit'
require 'java'

t = Time.now

dc = org.jruby.javasupport.test.DateHolder.new
dc.date = t

test_ok(t.to_s.eql?(dc.date.to_s), "Ruby time #{t} not converted to java date correctly: #{dc.date}")
