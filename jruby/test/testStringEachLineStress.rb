require 'test/minirunit'
test_check "Test string#each stress test:"

# this test originally caused out of memory errors on the 0.9.0 release

mess="A\nB\nC\nD\nE\nF\nG\nH\nI\nJ\nK\nL\nM\nN\nO\nP\nQ\nR\nS\nT\nU\nV\nW\nX\nY\nZ\na\nb\nc\nd\ne\nf\ng\nh\ni\nh\nk\nl\nm\nn\no\np\nq\nr\ns\nt\nu\nv\nw\nx\ny\nz\n0\n1\n2\n3\n4\n5\n6\n7\n8\n9\n!\n+\n" #64
mess << mess
mess << mess
mess << mess
mess << mess
mess << mess
mess << mess
mess << mess

# this test originally caused out of memory errors on the 0.9.0 release
count = 0
my_arr = []
mess.each do |line|
  # mri and jruby do not see the new value of mess being show in |line| within the block
  mess = "12345"
  my_arr << line
  count += 1
  # this shows E under mri and jruby
  test_equal("E\n", line) if count == 5
  # print "Expecting E got " + c if count == 5
end
# puts count

