require 'test/minirunit'

test_check "while/until";

tmp = open("while_tmp", "w")
tmp.print "tvi925\n";
tmp.print "tvi920\n";
tmp.print "vt100\n";
tmp.print "Amiga\n";
tmp.print "paper\n";
tmp.close

# test break

tmp = open("while_tmp", "r")
test_ok(tmp.kind_of?(File))

while tmp.gets()
  break if /vt100/
end

test_ok(false) unless /vt100/
test_ok(!tmp.eof?)
tmp.close

# test next
$bad = false
tmp = open("while_tmp", "r")
while tmp.gets()
  next if /vt100/;
  $bad = 1 if /vt100/;
end
test_ok(tmp.eof?)
test_ok(false) if /vt100/
test_ok(!$bad)
tmp.close

# test redo
$bad = false
tmp = open("while_tmp", "r")
while tmp.gets()
  line = $_
  $_ = gsub(/vt100/, 'VT100')
  if $_ != line
    gsub!('VT100', 'Vt100')
    redo;
  end
  $bad = 1 if /vt100/
  $bad = 1 if /VT100/
end
test_ok(tmp.eof? && !$bad)
tmp.close

sum=0
for i in 1..10
  sum += i
  i -= 1
  if i > 0
    redo
  end
end
test_ok(sum == 220)

File.unlink "while_tmp" or `/bin/rm -f "while_tmp"`
test_ok(!File.exist?("while_tmp"))

i = 0
until i>4
  i+=1
end
test_ok(i>4)


