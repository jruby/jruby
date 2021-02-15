package org.jruby.javasupport.util;

import java.util.*;

import com.kenai.jffi.Array;

public class JavaClassConfiguration implements Cloneable
{
	private static final Set<String> DEFAULT_EXCLUDES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			//TODO: note this is the original list
			"class", "finalize", "initialize", "java_class", "java_object", "__jcreate!",
			// TODO: note these are new additions
			"java_interfaces", "java_proxy_class", "java_proxy_class="
			)));
	
	// general
	public Map<String, List<Map<Class<?>, Map<String,Object>>>> parameterAnnotations;
    public Map<String, Map<Class<?>, Map<String,Object>>> methodAnnotations;
    public Map<String, Map<Class<?>, Map<String,Object>>> fieldAnnotations;
    public Map<Class<?>, Map<String,Object>> classAnnotations;
    public Map<String, List<Class<?>[]>> methodSignatures;
    public Map<String, Class<?>> fieldSignatures;
	
	public boolean callInitialize = true;
	public boolean allMethods = true;
	public boolean allClassMethods = true; // TODO: ensure defaults are sane
	public boolean javaConstructable = true;
	public List<Class<?>[]> extraCtors = new ArrayList<>();
	
	// for java proxies
	public boolean allCtors  = false;
	public boolean rubyConstructable = true; // 
	public boolean IroCtors = true;
	
	public Map<String, String> renamedMethods = new HashMap<>();
	public String javaCtorMethodName = "initialize";
	private Set<String> excluded = null;
	private Set<String> included = null;
	
	
	public JavaClassConfiguration clone()
	{
		JavaClassConfiguration other = new JavaClassConfiguration();
		if (excluded != null) other.excluded = new HashSet<>(excluded);
		if (included != null) other.included = new HashSet<>(included);
		other.javaCtorMethodName = javaCtorMethodName;
		
		other.IroCtors = IroCtors;
		other.rubyConstructable = rubyConstructable;
		other.allCtors = allCtors;
		
		other.javaConstructable = javaConstructable;
		other.allClassMethods = allClassMethods;
		other.allMethods = allMethods;
		other.callInitialize = callInitialize;
		
		other.renamedMethods = new HashMap<>(renamedMethods);
		other.extraCtors = new ArrayList<>(extraCtors); // NOTE: doesn't separate the arrays, is that fine?

		if (parameterAnnotations != null) other.parameterAnnotations = new HashMap<>(parameterAnnotations); // TOOD: deep clone
		if (methodAnnotations != null) other.methodAnnotations = new HashMap<>(methodAnnotations); // TOOD: deep clone
		if (fieldAnnotations != null) other.fieldAnnotations = new HashMap<>(fieldAnnotations); // TOOD: deep clone
		if (classAnnotations != null) other.classAnnotations = new HashMap<>(classAnnotations); // TOOD: deep clone
		if (methodSignatures != null) other.methodSignatures = new HashMap<>(methodSignatures); // TOOD: deep clone
		if (fieldSignatures != null) other.fieldSignatures = new HashMap<>(fieldSignatures); // TOOD: deep clone
		
		return other;
	}
	

	public synchronized Set<String> getExcluded()
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
	
	public synchronized Set<String> getIncluded()
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
