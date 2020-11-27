package org.jruby.javasupport.util;

public class JavaClassConfiguration
{
	public boolean callInitialize = true;
	public boolean allMethods = true;
	public boolean allClassMethods = true; // TODO: ensure defaults are sane
	public boolean javaConstructable = true;
	public Class<?>[][] extraCtors = null;
	
	// for java proxies
	public boolean allCtors  = false;
	public boolean rubyConstructable = true; // 
	public boolean IroCtors = true;
	
	//TODO: init method name?
	//TODO: renames?
}
