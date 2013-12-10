package database;

import java.util.ArrayList;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import database.auth.AuthenticationException;
import database.auth.Authenticator;

public class GroupManager
{
	private static String mongoInsertGroup(DB dbs, GroupBuilder group)
	{
		DBCollection new_user = dbs.getCollection("UserGroups");
		BasicDBObject query = new BasicDBObject("UserList",group.userList)
										.append("Name",group.name)
										.append("Admin",group.admin);
										 
		new_user.insert(query);
		DBObject corsor = new_user.findOne(query);
		return ("Group"+(String) corsor.get("_id"));
	}
	
	public static GroupBuilder mongoGetCourse(DB dbs, String courseID,String userId) throws AuthenticationException
	{
		DBCollection courses = dbs.getCollection("UserGroups");
		BasicDBObject query = new BasicDBObject("_id",courseID);
		DBObject corsor = courses.findOne(query);
		
		ArrayList<String> adminList = (ArrayList)corsor.get("Admin");
		boolean isAdmin;
		isAdmin = Authenticator.checkAuthentication(dbs, userId, adminList);
		
		if(!isAdmin)
		{
			throw new AuthenticationException(AuthenticationException.INVALID_PERMISSION);
		}
		
		GroupBuilder exactGroup = new GroupBuilder();
		
		exactGroup.setuserList((ArrayList)corsor.get("UserList"));
		exactGroup.setName((String)corsor.get("Name"));
		if (isAdmin) 
		{
			exactGroup.setAdmin((ArrayList)corsor.get("Admin")); // admin
		}
		
		return exactGroup;
		
	}
	
	
	public static boolean mongoUpdateGroup(DB dbs, String courseID,String userId,GroupBuilder group) throws AuthenticationException
	{
		DBCollection courses = dbs.getCollection("UserGroups");
		BasicDBObject query = new BasicDBObject("_id",courseID);
		DBObject corsor = courses.findOne(query);
		
		ArrayList<String> adminList = (ArrayList)corsor.get("Admin");
		boolean isAdmin;
		isAdmin = Authenticator.checkAuthentication(dbs, userId, adminList);

		if(!isAdmin)
		{
			throw new AuthenticationException(AuthenticationException.INVALID_PERMISSION);
		}
		
		BasicDBObject updated = new BasicDBObject();
		if (isAdmin) 
		{
			
			if (group.name != null) {
				updated.append("$set", new BasicDBObject("Name", group.name));
			}
			if (group.userList != null) {
				updated.append("$set", new BasicDBObject("UserList", group.userList));
			}

		}
		return true;
		
	}

}