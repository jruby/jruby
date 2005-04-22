package org.jruby.javasupport.test;

import java.util.List;
import java.util.Map;

public interface SimpleInterface {
	public List getList();
	public List getEmptyList();
	public List getNestedList();
	public List getNilList();
	public Map getMap();
	
	public void setNilList(List list);
	
	public boolean isNilListNil();
	
	public void modifyNestedList();
}
