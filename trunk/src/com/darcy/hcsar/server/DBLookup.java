package com.darcy.hcsar.server;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

public class DBLookup {
	
	public static String getParamValue(String name){
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Query.Filter paramFilter = 
			      new Query.FilterPredicate("name", 
			      Query.FilterOperator.EQUAL, 
			      name);
		Query q = new Query("config").setFilter(paramFilter);
		PreparedQuery pq = ds.prepare(q);
	    Entity result = pq.asSingleEntity();
	    String returnValue = null;
	    if (result != null){
	    	returnValue =(String)result.getProperty("value"); 
	    }
	    
		return returnValue;
	}

}
