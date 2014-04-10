set terminal pdf enhanced
set output 'java-c.pdf'

set style data histogram
set style histogram cluster gap 1

set title "2.1.1 vs JRuby+Truffle, Java and C"
set xlabel "Implementation" rotate by 270
set ylabel "Speedup Relative to 2.1.1"
set key top left

set style fill solid border rgb 'black'
set auto x
set xtics nomirror rotate by -45
set yrange [0:*]

plot 'java-c.data' using 2:xtic(1) title col#, \
        #'' using 3:xtic(1) title col, \
        #'' using 4:xtic(1) title col, \
        #'' using 5:xtic(1) title col, \
        #'' using 6:xtic(1) title col, \
        #'' using 7:xtic(1) title col, \
        #'' using 8:xtic(1) title col
