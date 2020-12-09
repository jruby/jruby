package org.jruby.javasupport.util;

import java.util.*;

import com.kenai.jffi.Array;

public class JavaClassConfiguration
{
	private static final Set<String> DEFAULT_EXCLUDES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			//TODO: note this is the original list
			"class", "finalize", "initialize", "java_class", "java_object", "__jcreate!",
			// TODO: note these are new additions
			"java_interfaces", "java_proxy_class", "java_proxy_class="
			)));
	public boolean callInitialize = true;
	public boolean allMethods = true;
	public boolean allClassMethods = true; // TODO: ensure defaults are sane
	public boolean javaConstructable = true;
	public Class<?>[][] extraCtors = null;
	
	// for java proxies
	public boolean allCtors  = false;
	public boolean rubyConstructable = true; // 
	public boolean IroCtors = true;
	
	public Map<String, String> renamedMethods = new HashMap<>();
	public String javaCtorMethodName = "initialize";
	private Set<String> excluded = null;
	private Set<String> included = null;
	
	//TODO: renames?
	

	public Set<String> getExcluded()
	{
		if (excluded == null) return DEFAULT_EXCLUDES;
		
		return excluded ;
	}
	
	public synchronized void exclude(String name)
	{
		if (included == null) included = new HashSet<>();
		if (excluded == null) excluded = new HashSet<>(DEFAULT_EXCLUDES);
		
		excluded.add(name);
		included.remove(name);
	}
	
	public Set<String> getIncluded()
	{
		if (included == null) return Collections.EMPTY_SET;
		
		return included;
	}
	
	public synchronized void include(String name)
	{
		if (included == null) included = new HashSet<>();
		if (excluded == null) excluded = new HashSet<>(DEFAULT_EXCLUDES);
		
		included.add(name);
		excluded.remove(name);
	}
}
