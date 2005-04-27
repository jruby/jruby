package org.jruby.javasupport.test;

import java.util.List;
import java.util.Map;

public interface SimpleInterface {
	public List getList();
	public List getEmptyList();
	public List getNestedList();
	public List getNilList();
	public Map getMap();
	public Map getEmptyMap();
	public Map getNestedMap();
	public Map getNilMap();
	public Map getMixedMap();
	
	public void setNilList(List list);
	public void setNilMap(Map map);
	
	public boolean isNilListNil();
	public boolean isNilMapNil();
	
	public void modifyNestedList();
}
