# ----------------------------------------------------------------------
# The Computer Language Shootout
# http://shootout.alioth.debian.org/
#
# Code based on / inspired by existing, relevant Shootout submissions
#
# Contributed by Anthony Borla
# Optimized by Jesse Millikan
# ----------------------------------------------------------------------

class Recursive
    def self.ack(m, n)
      {m => :int, n => :int, :return => :int}
      if m == 0 then 
        n + 1
      elsif n == 0
        ack(m - 1, 1)
      else 
         ack(m - 1, ack(m, n - 1))
      end
    end

    # ---------------------------------

    def self.fib(n)
       {n => :int, :return => :int}
       if n > 1 then
         fib(n - 2) + fib(n - 1) 
       else 
         1
       end
    end

    # ---------------------------------

    def self.tak(x, y, z)
      {x => :int, y => :int, z => :int, :return => :int}
      if y < x then
       tak(tak(x - 1, y, z), tak(y - 1, z, x), tak(z - 1, x, y))
      else z
      end
    end

    # ---------------------------------

    def self.main(args)
        {args => :string[]}
        n = 10

        puts("Ack"); puts n; puts ack(3, n);
        puts("Fib"); puts(27 + n); puts fib(27 + n);

        n = n - 1;
        puts("Tak"); puts n * 3; puts n * 2; puts n; puts tak(n * 3, n * 2, n);

        puts("Fib(3)"); puts fib(3)
        puts("Tak(3,2,1)"); puts tak(3, 2, 1);
    end
end
