$i=99
def b()"#{$i>0?$i: 'No'} bottle#{'s'if$i!=1} of beer"end
$i.times{w=" on the wall";puts b()+w+", #{b}.
Take one down, pass it around,
#{$i-=1;b+w}.

"}

