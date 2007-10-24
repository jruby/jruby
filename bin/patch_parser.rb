def get_numbers_until_end_block(table)
  while gets
    break if /\};/ =~ $_
    next if /^\/\// =~ $_
    split(/,/).each do |number|
      n = number.strip
      table.push(n.to_i) unless n == ""
    end
  end
  table
end

while gets
  break if /protected static final short\[\] yyTable = \{/ =~ $_
  print $_
end

table4 = get_numbers_until_end_block([])

puts "    protected static final short[] yyTable = YyTables.yyTable();"

while gets
  break if /protected static final short\[\] yyCheck = \{/ =~ $_
  print $_
end

check4 = get_numbers_until_end_block([])

puts "    protected static final short[] yyCheck = YyTables.yyCheck();"

while gets
  print $_
end

table2 = table4.slice!(0, table4.size / 2)
table3 = table4.slice!(0, table4.size / 2)
table1 = table2.slice!(0, table2.size / 2)
check2 = check4.slice!(0, check4.size / 2)
check3 = check4.slice!(0, check4.size / 2)
check1 = check2.slice!(0, check2.size / 2)

def printShortArray(table, f)
  table.each_with_index { |e, i|
    f.print "\n         " if (i % 10 == 0)
    begin
      f.printf "%4d, ", e
    rescue ArgumentError => a
      $stderr.puts "Trouble printing '#{e}' on index #{i}"
    end
  }
end

def printShortMethod(f, table, name)
  f.puts "   private static final short[] yy#{name}() {"
  f.puts "      return new short[] {"
  printShortArray table, f
  f.puts
  f.puts "      };"
  f.puts "   }"
  f.puts
end

open("YyTables.java", "w") { |f|
  f.print <<END
package org.jruby.parser;

public class YyTables {
   private static short[] combine(short[] t1, short[] t2, 
                                  short[] t3, short[] t4) {
      short[] t = new short[t1.length + t2.length + t3.length + t4.length];
      int index = 0;
      System.arraycopy(t1, 0, t, index, t1.length);
      index += t1.length;
      System.arraycopy(t2, 0, t, index, t2.length);
      index += t2.length;
      System.arraycopy(t3, 0, t, index, t3.length);
      index += t3.length;
      System.arraycopy(t4, 0, t, index, t4.length);
      return t;
   }

   public static final short[] yyTable() {
      return combine(yyTable1(), yyTable2(), yyTable3(), yyTable4());
   }

   public static final short[] yyCheck() {
      return combine(yyCheck1(), yyCheck2(), yyCheck3(), yyCheck4());
   }
END

  printShortMethod(f, table1, "Table1")
  printShortMethod(f, table2, "Table2")
  printShortMethod(f, table3, "Table3")
  printShortMethod(f, table4, "Table4")

  printShortMethod(f, check1, "Check1")
  printShortMethod(f, check2, "Check2")
  printShortMethod(f, check3, "Check3")
  printShortMethod(f, check4, "Check4")

  f.puts "}"
}
