package org.jruby.ext.posix;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Structure;
import com.sun.jna.StringArray;
import com.sun.jna.*;
import com.sun.jna.ptr.PointerByReference;

public class NativeGroup extends Structure implements Group {
    public String gr_name;   // name
    public String gr_passwd; // group password (encrypted)
    public int gr_gid;       // group id
    public Pointer gr_mem;
    
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
        int size = Pointer.SIZE;
        int i=0;
        List<String> lst = new ArrayList<String>();
        while(gr_mem.getPointer(i) != null) {
            lst.add(gr_mem.getPointer(i).getString(0));
            i+=size;
        }
        return lst.toArray(new String[0]);
    }
}
