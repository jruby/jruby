require 'benchmark'

def unexisting_var
  # aa-dv
  aa ||= 1; ab ||= 1; ac ||= 1; ad ||= 1; ae ||= 1; af ||= 1; ag ||= 1; ah ||= 1; ai ||= 1; aj ||= 1
  ak ||= 1; al ||= 1; am ||= 1; an ||= 1; ao ||= 1; ap ||= 1; aq ||= 1; ar ||= 1; as ||= 1; at ||= 1
  au ||= 1; av ||= 1; aw ||= 1; ax ||= 1; ay ||= 1; az ||= 1; ba ||= 1; bb ||= 1; bc ||= 1; bd ||= 1
  be ||= 1; bf ||= 1; bg ||= 1; bh ||= 1; bi ||= 1; bj ||= 1; bk ||= 1; bl ||= 1; bm ||= 1; bn ||= 1
  bo ||= 1; bp ||= 1; bq ||= 1; br ||= 1; bs ||= 1; bt ||= 1; bu ||= 1; bv ||= 1; bw ||= 1; bx ||= 1
  by ||= 1; bz ||= 1; ca ||= 1; cb ||= 1; cc ||= 1; cd ||= 1; ce ||= 1; cf ||= 1; cg ||= 1; ch ||= 1
  ci ||= 1; cj ||= 1; ck ||= 1; cl ||= 1; cm ||= 1; cn ||= 1; co ||= 1; cp ||= 1; cq ||= 1; cr ||= 1
  cs ||= 1; ct ||= 1; cu ||= 1; cv ||= 1; cw ||= 1; cx ||= 1; cy ||= 1; cz ||= 1; da ||= 1; db ||= 1
  dc ||= 1; dd ||= 1; de ||= 1; df ||= 1; dg ||= 1; dh ||= 1; di ||= 1; dj ||= 1; dk ||= 1; dl ||= 1
  dm ||= 1; dn ||= 1; _do ||= 1; dp ||= 1; dq ||= 1; dr ||= 1; ds ||= 1; dt ||= 1; du ||= 1; dv ||= 1
end

def existing_var
  aa=1
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
  aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; aa ||= 2; 
end


puts "Existing var:"
5.times do 
  puts Benchmark.measure { 100_000.times { existing_var() } }
end

puts "Unexisting var:"
5.times do 
  puts Benchmark.measure { 100_000.times { unexisting_var() } }
end
