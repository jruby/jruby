def generate_yyname_remaps(file)
  remaps = {}
  File.readlines(file).each do |line|
    if /%token\s+\S+\s*(?<name>\S+)[^{]*{{(?<replacement>.*)}}/ =~ line
      replacement = if replacement.length == 1
                      %Q{\"'#{replacement}'\"}
                    else
                      %Q{\"#{replacement}\"}
                    end
      name = %Q{"#{name}"}
#      $stderr.puts "NAME #{name}(#{name.length}) = #{replacement.inspect}"
      remaps[name] = replacement   
    end
  end
  remaps
end

def get_numbers_until_end_block
  table = []
  while line = gets
    break if /\};/ =~ line
    next if /^\/\// =~ line
    line.split(/,/).each do |number|
      n = number.strip
      table.push(n.to_i) unless n == ""
    end
  end
  table
end

def print_yynames_until_end_block(remaps)
  while line = gets
    if /\};/ =~ line
      print line
      break
    end
    next if /^\/\// =~ line
    new_line = line.split(/,/).map do |element|
#      $stderr.puts "ELEMENT #{element.strip} #{element.strip.size}"
      key = element.strip.gsub("\\\\", '\\')

      # In .y we use '\\' which jay does realize is backslash in generated code
      # but in yyNames it wraps the string in extra "" and then expands the number
      # of \\ to make each \ its own backslash.  Part of me does not quite grok
      # how it encodes the name like this yet the specification works on matching
      # an incoming token.  Working around this by making it the key we need.
      key = "\"\'\\\\'\"" if key == "\"'\\\\'\""
      remaps[key] || element
    end.join(',')
    puts new_line
  end
end

# We use this script to generate our normal parser and the parser for 
# the ripper extension.
package = ARGV[0] =~ /Ripper/ ? 'org.jruby.ext.ripper' : 'org.jruby.parser'

while gets
  break if /protected static final short\[\] yyTable = \{/ =~ $_
  print $_
end

yytable_prefix = ARGV.length == 1 ? '' : ARGV.shift
original_grammar_file = ARGV.shift
  
remaps = generate_yyname_remaps original_grammar_file

table4 = get_numbers_until_end_block

puts "    protected static final short[] yyTable = #{yytable_prefix}YyTables.yyTable();"

while gets
  break if /protected static final short\[\] yyCheck = \{/ =~ $_
  print $_
end

check4 = get_numbers_until_end_block

puts "    protected static final short[] yyCheck = #{yytable_prefix}YyTables.yyCheck();"

while gets
  print $_
  break if /protected static final String\[\] yyNames = \{/ =~ $_
end

print_yynames_until_end_block remaps

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

open("#{yytable_prefix}YyTables.java", "w") { |f|
  f.print <<END
/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2013-2017 The JRuby Team (jruby@jruby.org)
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package #{package};

public class #{yytable_prefix}YyTables {
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
