package org.jruby.javasupport.test;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RubyTestObject
{
    public RubyTestObject duplicate ();
    public boolean isSelf (RubyTestObject obj);

    public void noArgs ();

    public void setNumber (double d);
    public void setNumber (int i);
    public void setNumber (long l);
    public void setNumber (Double d);
    public void setNumber (Integer i);
    public void setNumber (Long l);

    public double getNumberAsDouble ();
    public int getNumberAsInt ();
    public long getNumberAsLong ();
    public Double getNumberAsDoubleObj ();
    public Integer getNumberAsIntObj ();
    public Long getNumberAsLongObj ();

    public String getString ();
    public void setString (String s);

    public boolean getBool ();
    public void setBool (boolean b);

    public Object getObject ();
    public void setObject (Object obj);

    public void setList (Collection l);
    public void setList (int[] l);
    public void setList (Object[] l);
    public List getList ();
    public Object[]  getListAsArray ();
    public String[]  getListAsStringArray ();
    public int[]     getListAsIntArray ();
    public Integer[] getListAsIntegerArray ();
    public Set       getListAsSet ();
    public Collection getListAsCollection ();

    public void setMap (Map m);
    public Map getMap ();

    public void addToList (Object obj);
    public void removeFromList (Object obj);
    public String joinList ();
}
