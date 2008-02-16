package org.jruby.ext.posix;

import com.sun.jna.Structure;
import com.sun.jna.StringArray;
import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

public class NativeGroup extends Structure implements Group {
    public String gr_name;   // name
    public String gr_passwd; // group password (encrypted)
    public int gr_gid;       // group id
    public StringArray gr_mem;
    //    public byte[][] gr_mem;  // group members
    
    public String getName() {
        return gr_name;
    }
    public String getPassword() {
        return gr_passwd;
    }
    public long getGID() {
        return gr_gid;
    }
    public String[] getMembers() {
        //        System.err.println(gr_mem.getPointer(0).getByte(0));
        /* 
       if(gr_mem.getValue() != null) {
            Pointer p = gr_mem.getValue();
            int index = -1;
            while(p.getByte(++index) != 0);
            try {
                String s = new String(p.getByteArray(0, index), "ISO-8859-1");
                System.err.println("pointerZ:" + s);
            } catch(Exception e) {}
            System.err.println("1:"+p.getByte(index+21));
            System.err.println("2:"+p.getByte(index+22));
            System.err.println("3:"+p.getByte(index+20));
        }
        */
        return new String[0];
    }
}
