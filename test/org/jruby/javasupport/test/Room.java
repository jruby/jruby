package org.jruby.javasupport.test;

public class Room {
    private final String name;
    private String owner = null;
	
    public Room(String name) {
        this.name = name;
    }
	
    public boolean equals(Object obj) {
        if (! (obj instanceof Room))
            return false;
            Room that = (Room) obj;
            return name.equals(that.name);
    }
	
    public String toString() {
        return name;
    }
	
    public int hashCode() {
        return name.hashCode();
    }
}
