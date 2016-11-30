def fac(n)
  if n <= 1
    1
  else
    nMinusOne = n - 1
    nMOFact = fac(nMinusOne)
    res = n * nMOFact
    res
  end
end

def main
  res = fac(2)
  puts res
  res
end
Truffle::Interop.export_method(:main)

# Don't add a licence header to this file - we use the line offets!
