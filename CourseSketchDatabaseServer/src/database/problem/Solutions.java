package database.problem;

import static database.StringConstants.ACCESS_DATE;
import static database.StringConstants.ADMIN;
import static database.StringConstants.ASSIGNMENT_LIST;
import static database.StringConstants.CLOSE_DATE;
import static database.StringConstants.COURSE_ACCESS;
import static database.StringConstants.COURSE_SEMESTER;
import static database.StringConstants.DESCRIPTION;
import static database.StringConstants.IMAGE;
import static database.StringConstants.MOD;
import static database.StringConstants.NAME;
import static database.StringConstants.USERS;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import protobuf.srl.school.School.SrlCourse;
import protobuf.srl.school.School.SrlPermission;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import database.DatabaseAccessException;
import database.PermissionBuilder;
import database.RequestConverter;
import database.auth.AuthenticationException;
import database.auth.Authenticator;

public class Solutions 
{
	public static String mongoInsertCourse(DB dbs, SrlSolution solution)
	{
		DBCollection new_user = dbs.getCollection("Solutions");
		BasicDBObject query = new BasicDBObject(ALLOWED_IN_PROBLEMBANK,solution.getAlowedProblemBank())
										 .append(PRACTICE_PROBLEM,solution.getPracticeProblem()) 
										 .append(SOLUTION, solution.getAccessDate().getSolution())
										 .append(ADMIN, solution.getAccessPermission().getAdminPermissionList())
										 .append(MOD,solution.getAccessPermission().getModeratorPermissionList())
										 .append(USERS, solution.getAccessPermission().getUserPermissionList());
										 
		new_user.insert(query);
		DBObject corsor = new_user.findOne(query);
		return corsor.get("SELF_ID").toString();
	}
	
	public static SrlCourse mongoGetSolutions(DB dbs, String solutionId,String userId) throws AuthenticationException, DatabaseAccessException
	{
		DBRef myDbRef = new DBRef(dbs, "Solutions", new ObjectId(solutionId));
		DBObject corsor = myDbRef.fetch();
		if (corsor == null) {
			throw new DatabaseAccessException("Course was not found with the following ID " + solutionId);
		}
		ArrayList adminList =  (ArrayList<Object>) corsor.get(ADMIN); //convert to ArrayList<String>
		ArrayList modList =  (ArrayList<Object>) corsor.get(MOD); //convert to ArrayList<String>
		ArrayList usersList =  (ArrayList<Object>) corsor.get(USERS); //convert to ArrayList<String>
		boolean isAdmin,isMod,isUsers;
		isAdmin = Authenticator.checkAuthentication(dbs, userId,adminList);
		isMod = Authenticator.checkAuthentication(dbs, userId, modList);
		isUsers = Authenticator.checkAuthentication(dbs, userId, usersList);

		if(!isAdmin && !isMod && !isUsers)
		{
			throw new AuthenticationException(AuthenticationException.INVALID_PERMISSION);
		}
		//need to figure out how to add the SrlSolutin which is similar to SrlCourse
		SrlSolution.Builder exactCourse = SrlSolution.newBuilder();
		exactCourse.setSemester((String)corsor.get(ALLOWED_IN_PROBLEMBANK));
		exactCourse.setSemester((String)corsor.get(PRACTICE_PROBLEM));
		exactCourse.setSemester((String)corsor.get(SOLUTION));
		
		if (isAdmin) 
		{
			try {
				exactCourse.setAccess(SrlCourse.Accessibility.valueOf((Integer) corsor.get(COURSE_ACCESS))); // admin
			} catch(Exception e) {
				
			}
			SrlPermission.Builder permissions = SrlPermission.newBuilder();
			permissions.addAllAdminPermission((ArrayList)corsor.get(ADMIN)); // admin
			permissions.addAllModeratorPermission((ArrayList)corsor.get(MOD));	 // admin
			permissions.addAllUserPermission((ArrayList)corsor.get(USERS)); //admin
			exactCourse.setAccessPermission(permissions.build());
		}
		return exactCourse.build();
		
	}
	
	
	public static boolean mongoUpdateSolutions(DB dbs, String courseID, String userId, SrlCourse course) throws AuthenticationException
	{
		DBRef myDbRef = new DBRef(dbs, "Solutions", new ObjectId(courseID));
		DBObject corsor = myDbRef.fetch();
		DBObject updateObj = null;
		DBCollection courses = dbs.getCollection("Solutions");
		
		ArrayList adminList = (ArrayList<Object>)corsor.get(ADMIN);
		ArrayList modList = (ArrayList<Object>)corsor.get(MOD);
		boolean isAdmin,isMod;
		isAdmin = Authenticator.checkAuthentication(dbs, userId, adminList);
		isMod = Authenticator.checkAuthentication(dbs, userId, modList);

		if(!isAdmin && !isMod)
		{
			throw new AuthenticationException(AuthenticationException.INVALID_PERMISSION);
		}

		BasicDBObject updated = new BasicDBObject();
		if (isAdmin) 
		{
			if (course.hasSemester()) 
			{
				updateObj = new BasicDBObject(COURSE_SEMESTER, course.getSemester());
				courses.update(corsor, new BasicDBObject ("$set",updateObj));
			}
			if (course.hasAccessDate()) 
			{
				
				updateObj = new BasicDBObject(ACCESS_DATE, course.getAccessDate().getMillisecond());
				courses.update(corsor, new BasicDBObject ("$set", updateObj));
				
			}
		//Optimization: have something to do with pulling values of an array and pushing values to an array
			if (course.hasCloseDate()) 
			{
				updateObj = new BasicDBObject(CLOSE_DATE, course.getCloseDate().getMillisecond());
				courses.update(corsor, new BasicDBObject ("$set", updateObj));
			}
			
			if (course.hasImageUrl()) {
				updateObj = new BasicDBObject(IMAGE, course.getImageUrl());
				courses.update(corsor, new BasicDBObject ("$set", updateObj));
			}
			if (course.hasDescription()) {
				updateObj = new BasicDBObject(DESCRIPTION, course.getDescription());
				courses.update(corsor, new BasicDBObject ("$set",updateObj));
			}
			if (course.hasName()) {
				updateObj = new BasicDBObject(NAME, course.getName());
				courses.update(corsor, new BasicDBObject ("$set",updateObj));
			}
			if (course.hasAccess()) 
			{
				updateObj = new BasicDBObject(COURSE_ACCESS, course.getAccess().getNumber());
				courses.update(corsor, new BasicDBObject ("$set",updateObj));
				
			}
		//Optimization: have something to do with pulling values of an array and pushing values to an array
			if (course.getAccessPermission() != null) {
				SrlPermission permissions = course.getAccessPermission();
				if (permissions.getAdminPermissionList() != null) {
					updateObj = new BasicDBObject(ADMIN, permissions.getAdminPermissionList());
					courses.update(corsor, new BasicDBObject ("$set",updateObj));
				}
				if (permissions.getModeratorPermissionList() != null) {
					updateObj = new BasicDBObject(MOD, permissions.getModeratorPermissionList());
					courses.update(corsor, new BasicDBObject ("$set",updateObj));
				}
				if (permissions.getUserPermissionList() != null) {
					updateObj = new BasicDBObject(USERS, permissions.getUserPermissionList());
					courses.update(corsor, new BasicDBObject ("$set",updateObj));
				}
			}
			
			
		}
		//courses.update(corsor, new BasicDBObject ("$set",updateObj));
		
		return true;
		
	}
}
