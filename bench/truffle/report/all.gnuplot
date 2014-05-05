set terminal pdf enhanced size 10,6
set output 'all.pdf'

set style data histogram
set style histogram cluster gap 1

set title "JRuby+Truffle vs All Ruby Implementations"
set xlabel "Implementation" rotate by 270
set ylabel "Speedup Relative to 1.8.7-p374"
set key top left

set style fill solid border rgb 'black'
set auto x
set xtics nomirror rotate by -45
set yrange [0:*]

plot 'all.data' using 2:xtic(1) title col, \
        '' using 3:xtic(1) title col, \
        '' using 4:xtic(1) title col, \
        '' using 5:xtic(1) title col, \
        '' using 6:xtic(1) title col, \
        '' using 7:xtic(1) title col, \
        '' using 8:xtic(1) title col, \
        '' using 9:xtic(1) title col, \
        '' using 10:xtic(1) title col
