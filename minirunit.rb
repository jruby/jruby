
$testnum=0
$ntest=0
$failed = 0

def test_check(what)
  printf "\n\n%s\n", what
  $what = what
  $testnum = 0
end

def test_ok(cond)
  $testnum+=1
  $ntest+=1
  if cond
    printf "ok %d\n", $testnum
  else
    where = caller[0]
    printf "not ok %s %d -- %s\n", $what, $testnum, where
    $failed+=1 
  end
end

