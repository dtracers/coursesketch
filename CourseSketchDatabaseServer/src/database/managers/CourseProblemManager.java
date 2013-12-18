package database.managers;

import static database.StringConstants.*;

import java.util.ArrayList;

import org.bson.types.ObjectId;

import protobuf.srl.school.School.SrlBankProblem;
import protobuf.srl.school.School.SrlPermission;
import protobuf.srl.school.School.SrlProblem;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;

import database.DatabaseAccessException;
import database.auth.AuthenticationException;
import database.auth.Authenticator;
import database.auth.Authenticator.AuthType;

public class CourseProblemManager 
{
	public static String mongoInsertCourseProblem(DB dbs, String userId, SrlProblem problem) throws AuthenticationException, DatabaseAccessException
	{
		DBCollection new_user = dbs.getCollection("Problems");

		// make sure person is mod or admin for the assignment
		AuthType auth = new AuthType();
		auth.checkAdminOrMod = true;
		if (!Authenticator.mognoIsAuthenticated(dbs, ASSIGNMENT_COLLECTION, problem.getAssignmentId(), userId, 0, auth))
		{
			throw new AuthenticationException(AuthenticationException.INVALID_PERMISSION);
		}

		BasicDBObject query = new BasicDBObject(COURSE_ID,problem.getCourseId())
										 .append(ASSIGNMENT_ID,problem.getAssignmentId())
										 .append(PROBLEM_BANK_ID,problem.getProblemBankId())
										 .append(GRADE_WEIGHT,problem.getGradeWeight())
										 .append(ADMIN, problem.getAccessPermission().getAdminPermissionList())
										 .append(MOD, problem.getAccessPermission().getModeratorPermissionList())
										 .append(USERS, problem.getAccessPermission().getUserPermissionList());
		new_user.insert(query);
		DBObject corsor = new_user.findOne(query);

		// inserts the id into the previous the course
		AssignmentManager.mongoInsert(dbs, problem.getAssignmentId(), corsor.get(SELF_ID).toString());

		return corsor.get(SELF_ID).toString();
	}

	public static SrlProblem mongoGetCourseProblem(DB dbs, String problemId,String userId, long checkTime) throws AuthenticationException, DatabaseAccessException
	{
		DBRef myDbRef = new DBRef(dbs, "Problems", new ObjectId(problemId));
		DBObject corsor = myDbRef.fetch();
		if (corsor == null) {
			throw new DatabaseAccessException("Course was not found with the following ID " + problemId);
		}

		ArrayList adminList = (ArrayList<Object>)corsor.get(ADMIN);
		ArrayList modList = (ArrayList<Object>)corsor.get(MOD);	
		ArrayList usersList = (ArrayList<Object>)corsor.get(USERS);
		boolean isAdmin,isMod,isUsers;
		isAdmin = Authenticator.checkAuthentication(dbs, userId, adminList);
		isMod = Authenticator.checkAuthentication(dbs, userId, modList);
		isUsers = Authenticator.checkAuthentication(dbs, userId, usersList);

		if(!isAdmin && !isMod && !isUsers)
		{
			throw new AuthenticationException(AuthenticationException.INVALID_PERMISSION);
		}

		SrlProblem.Builder exactProblem = SrlProblem.newBuilder();

		exactProblem.setId(problemId);
		exactProblem.setCourseId((String)corsor.get(COURSE_ID));
		exactProblem.setAssignmentId((String)corsor.get(ASSIGNMENT_ID));
		exactProblem.setGradeWeight((String)corsor.get(GRADE_WEIGHT));

		// check to make sure the problem is within the time period that the assignment is open and the user is in the assignment
		AuthType auth = new AuthType();
		auth.checkDate = true;
		auth.user = true;
		if (isUsers) {
			if (!Authenticator.mognoIsAuthenticated(dbs, COURSE_COLLECTION, (String)corsor.get(COURSE_ID), userId, checkTime, auth))
			{
				throw new AuthenticationException(AuthenticationException.EARLY_ACCESS);
			}
		}
		
		// problem manager get problem from bank (as a user!)
		SrlBankProblem problemBank = BankProblemManager.mongoGetBankProblem(dbs, (String)corsor.get(PROBLEM_BANK_ID), (String)exactProblem.getCourseId()); // problem bank look up
		if (problemBank != null) {
			exactProblem.setProblemInfo(problemBank);
		}

		SrlPermission.Builder permissions = SrlPermission.newBuilder();
		if (isAdmin) 
		{
			permissions.addAllAdminPermission((ArrayList)corsor.get(ADMIN)); // admin
			permissions.addAllModeratorPermission((ArrayList)corsor.get(MOD));	 // admin
		}
		if (isAdmin || isMod) {
			permissions.addAllUserPermission((ArrayList)corsor.get(USERS)); // mod
			exactProblem.setAccessPermission(permissions.build());
		}
		return exactProblem.build();

	}

	public static boolean mongoUpdateCourseProblem(DB dbs, String problemId,String userId,SrlProblem problem) throws AuthenticationException
	{
		DBRef myDbRef = new DBRef(dbs, "Problems", new ObjectId(problemId));
		DBObject corsor = myDbRef.fetch();

		ArrayList adminList = (ArrayList<Object>)corsor.get(ADMIN);
		ArrayList modList = (ArrayList<Object>)corsor.get(MOD);	
		ArrayList usersList = (ArrayList<Object>)corsor.get(USERS);
		boolean isAdmin,isMod,isUsers;
		isAdmin = Authenticator.checkAuthentication(dbs, userId, adminList);
		isMod = Authenticator.checkAuthentication(dbs, userId, modList);

		if(!isAdmin && !isMod)
		{
			throw new AuthenticationException(AuthenticationException.INVALID_PERMISSION);
		}

		BasicDBObject updated = new BasicDBObject();
		if (isAdmin || isMod) 
		{
			if (problem.hasGradeWeight()) {
				updated.append("$set", new BasicDBObject(GRADE_WEIGHT, problem.getGradeWeight()));
			}
			if (problem.hasProblemBankId()) {
				updated.append("$set", new BasicDBObject(PROBLEM_BANK_ID, problem.getProblemBankId()));
			}
		//Optimization: have something to do with pulling values of an array and pushing values to an array
			if (problem.hasAccessPermission()) {
				SrlPermission permissions = problem.getAccessPermission();
				if (isAdmin)
				{
					// ONLY ADMIN CAN CHANGE ADMIN OR MOD
					if (permissions.getAdminPermissionCount() > 0) {
						updated.append("$set", new BasicDBObject(ADMIN, permissions.getAdminPermissionList()));
					}
					if (permissions.getModeratorPermissionCount() > 0) {
						updated.append("$set", new BasicDBObject(MOD, permissions.getModeratorPermissionList()));
					}
				}
				if (permissions.getUserPermissionCount() > 0) 
				{
					updated.append("$set", new BasicDBObject(USERS, permissions.getUserPermissionList()));
				}
			}
		}
		return true;
	}

}
