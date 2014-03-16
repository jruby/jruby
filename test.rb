def r(val); a = yield(); puts [val, a].inspect; end
r([]){next *nil}
r([1]){next *1}
r([]){next *[]}
r([1]){next *[1]}
r([nil]){next *[nil]}
r([[]]){next *[[]]}
r([]){next *[*[]]}
r([1]){next *[*[1]]}
r([1,2]){next *[*[1,2]]}
