
package org.jvyamlb;

import org.jruby.util.ByteList;

public class ResolverScanner {
%%{
        machine resolver_scanner;

        action bool_tag { tag = "tag:yaml.org,2002:bool"; }
        action merge_tag { tag = "tag:yaml.org,2002:merge"; }
        action null_tag { tag = "tag:yaml.org,2002:null"; }
        action timestamp_ymd_tag { tag = "tag:yaml.org,2002:timestamp#ymd"; }
        action timestamp_tag { tag = "tag:yaml.org,2002:timestamp"; }
        action value_tag { tag = "tag:yaml.org,2002:value"; }
        action float_tag { tag = "tag:yaml.org,2002:float"; }
        action int_tag { tag = "tag:yaml.org,2002:int"; }

        Bool = ("yes" | "Yes" | "YES" | "no" | "No" | "NO" | 
                "true" | "True" | "TRUE" | "false" | "False" | "FALSE" | 
                "on" | "On" | "ON" | "off" | "Off" | "OFF") %/bool_tag;

        Merge = "<<" %/merge_tag;
        Value = "=" %/value_tag;
        Null  = ("~" | "null" | "Null" | "null" | "NULL" | " ") %/null_tag;

        digitF = digit | ",";
        digit2 = digit | "_" | ",";
        sign = "-" | "+";
        timestampFract = "." digit*;
        timestampZone = [ \t]* ("Z" | (sign digit{1,2} ( ":" digit{2} )?));
        TimestampYMD = digit{4} ("-" digit{2}){2} %/timestamp_ymd_tag;
        Timestamp = digit{4} ("-" digit{1,2}){2} ([Tt] | [ \t]+) digit{1,2} ":" digit{2} ":" digit{2} timestampFract? timestampZone %/timestamp_tag;

        exp = [eE] sign digit+;

        Float = ((sign? ((digitF+ "." digit* exp?)
                     | ((digitF+)? "." digit+ exp?)
                     | (digit+ (":" [0-5]? digit)+ "." digit*)
                     | "." ("inf" | "Inf" | "INF"))) 
                 | ("." ("nan" | "NaN" | "NAN"))) %/float_tag;

        binaryInt = "0b" [0-1_]+;
        octalInt = "0" [0-7_]+;
        decimalInt = "0" |
                     [1-9]digit2* (":" [0-5]? digit)*;
        hexaInt = "0x" [0-9a-fA-F_,]+;
        Int = sign? (binaryInt | octalInt | decimalInt | hexaInt) %/int_tag;

        Scalar = Bool | Null | Int | Float | Merge | Value | Timestamp | TimestampYMD;
        main := Scalar;
}%%

%% write data nofinal;

   public String recognize(ByteList list) {
       String tag = null;
       int cs;
       int act;
       int have = 0;
       int nread = 0;
       int p=list.begin;
       int pe = p+list.realSize;
       int tokstart = -1;
       int tokend = -1;

       byte[] data = list.bytes;
       if(pe == 0) {
         data = new byte[]{(byte)'~'};
         pe = 1;
       }
              
%% write init;

%% write exec;

%% write eof;
       return tag;
   }

   public static void main(String[] args) {
       ByteList b = new ByteList(78);
       b.append(args[0].getBytes());
/*
       for(int i=0;i<b.realSize;i++) {
           System.err.println("byte " + i + " is " + b.bytes[i] + " char is: " + args[0].charAt(i));
       }
*/
       System.err.println(new ResolverScanner().recognize(b));
   }
}
