package org.jruby.util;

import java.util.*;

public class NameList extends ArrayList {
    private int count;
    
	public NameList() {
	    super();
	}
	
	public NameList(int size) {
	    super(size);
	    
	    for (int i = 0; i < size; i++) {
	        add(null);
	    }
	}
	
    /**
     * Gets the count.
     * @return Returns a int
     */
    public int getCount() {
        return count;
    }

    /**
     * Sets the count.
     * @param count The count to set
     */
    public void setCount(int count) {
        this.count = count;
    }
}