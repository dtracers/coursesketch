package database.institution.mongo;

import com.google.protobuf.ByteString;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import com.mongodb.MongoClient;
import coursesketch.server.interfaces.MultiConnectionManager;
import database.DatabaseAccessException;
import database.auth.AuthenticationException;
import database.auth.Authenticator;
import database.auth.MongoAuthenticator;
import database.institution.Institution;
import database.submission.SubmissionManager;
import database.user.GroupManager;
import database.user.UserClient;
import org.bson.types.ObjectId;
import protobuf.srl.lecturedata.Lecturedata.Lecture;
import protobuf.srl.lecturedata.Lecturedata.LectureSlide;
import protobuf.srl.school.School.SrlAssignment;
import protobuf.srl.school.School.SrlBankProblem;
import protobuf.srl.school.School.SrlCourse;
import protobuf.srl.school.School.SrlGroup;
import protobuf.srl.school.School.SrlPermission;
import protobuf.srl.school.School.SrlProblem;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static database.DatabaseStringConstants.DATABASE;
import static database.DatabaseStringConstants.GROUP_PREFIX;
import static database.DatabaseStringConstants.SELF_ID;
import static database.DatabaseStringConstants.UPDATE_COLLECTION;
import static database.DatabaseStringConstants.USER_COLLECTION;
import static database.DatabaseStringConstants.USER_GROUP_COLLECTION;
import static database.DatabaseStringConstants.USER_LIST;

/**
 * A Mongo implementation of the Institution it inserts and gets courses as
 * needed.
 *
 * @author gigemjt
 *
 */
@SuppressWarnings({ "PMD.CommentRequired", "PMD.TooManyMethods" })
public final class MongoInstitution implements Institution {
    /**
     * A single instance of the mongo institution.
     */
    @SuppressWarnings("PMD.AssignmentToNonFinalStatic")
    private static volatile MongoInstitution instance;

    /**
     * Holds an Authenticator used to authenticate specific users.
     */
    private Authenticator auth;

    /**
     * A private Database that stores all of the data used by mongo.
     */
    private DB database;

    /**
     * A private institution that accepts a url for the database location.
     *
     * @param url
     *            The location that the server is taking place.
     */
    private MongoInstitution(final String url) {
        MongoClient mongoClient = null;
        try {
            mongoClient = new MongoClient(url);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if (mongoClient == null) {
            return;
        }
        database = mongoClient.getDB(DATABASE);
    }

    /**
     * A default constructor that creates an instance at a specific database
     * location.
     */
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    private MongoInstitution() {
        this("192.168.56.201");
        // this("localhost");
    }

    /**
     * @return An instance of the mongo client. Creates it if it does not exist.
     */
    @SuppressWarnings("checkstyle:innerassignment")
    public static MongoInstitution getInstance() {
        MongoInstitution result = instance;
        if (result == null) {
            synchronized (MongoInstitution.class) {
                if (result == null) {
                    result = instance;
                    instance = result = new MongoInstitution();
                    result.auth = new Authenticator(new MongoAuthenticator(instance.database));
                }
            }
        }
        return result;
    }

    /**
     * Used only for the purpose of testing overwrite the instance with a test
     * instance that can only access a test database.
     *
     * @param testOnly
     *            if true it uses the test database. Otherwise it uses the real
     *            name of the database.
     * @param fakeDB
     *            uses a fake DB for its unit tests. This is typically used for
     *            unit test.
     */
    public MongoInstitution(final boolean testOnly, final DB fakeDB) {
        if (testOnly && fakeDB != null) {
            database = fakeDB;
        } else {
            MongoClient mongoClient = null;
            try {
                mongoClient = new MongoClient("localhost");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            if (mongoClient == null) {
                return;
            }
            if (testOnly) {
                database = mongoClient.getDB("test");
            } else {
                database = mongoClient.getDB(DATABASE);

            }
        }
        instance = this;
        instance.auth = new Authenticator(new MongoAuthenticator(instance.database));
    }

    /*
     * (non-Javadoc)
     *
     * @see database.institution.mongo.Institution#setUpIndexes()
     */
    @Override
    public void setUpIndexes() {
        System.out.println("Setting up the indexes");
        database.getCollection(USER_COLLECTION).ensureIndex(new BasicDBObject(SELF_ID, 1).append("unique", true));
        database.getCollection(UPDATE_COLLECTION).ensureIndex(new BasicDBObject(SELF_ID, 1).append("unique", true));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * database.institution.mongo.Institution#mongoGetCourses(java.util.List,
     * java.lang.String)
     */
    @Override
    public ArrayList<SrlCourse> getCourses(final List<String> courseIds, final String userId) throws AuthenticationException {
        final long currentTime = System.currentTimeMillis();
        final ArrayList<SrlCourse> allCourses = new ArrayList<SrlCourse>();
        for (String courseId : courseIds) {
            try {
                allCourses.add(CourseManager.mongoGetCourse(getInstance().auth, getInstance().database, courseId, userId, currentTime));
            } catch (DatabaseAccessException e) {
                e.printStackTrace();
            }
        }
        return allCourses;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * database.institution.mongo.Institution#mongoGetCourseProblem(java.util
     * .List, java.lang.String)
     */
    @Override
    public ArrayList<SrlProblem> getCourseProblem(final List<String> problemID, final String userId) throws AuthenticationException,
            DatabaseAccessException {
        final long currentTime = System.currentTimeMillis();
        final ArrayList<SrlProblem> allCourses = new ArrayList<SrlProblem>();
        for (int index = 0; index < problemID.size(); index++) {
            try {
                allCourses.add(CourseProblemManager.mongoGetCourseProblem(getInstance().auth, getInstance().database, problemID.get(index), userId,
                        currentTime));
            } catch (DatabaseAccessException e) {
                e.printStackTrace();
                if (!e.isRecoverable()) {
                    throw e;
                }
            } catch (AuthenticationException e) {
                if (e.getType() != AuthenticationException.INVALID_DATE) {
                    throw e;
                }
            }
        }
        return allCourses;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * database.institution.mongo.Institution#mongoGetAssignment(java.util.List,
     * java.lang.String)
     */
    @Override
    public ArrayList<SrlAssignment> getAssignment(final List<String> assignmentID, final String userId) throws AuthenticationException,
            DatabaseAccessException {
        final long currentTime = System.currentTimeMillis();
        final ArrayList<SrlAssignment> allAssignments = new ArrayList<SrlAssignment>();
        for (int assignments = assignmentID.size() - 1; assignments >= 0; assignments--) {
            try {
                allAssignments.add(AssignmentManager.mongoGetAssignment(getInstance().auth, getInstance().database, assignmentID.get(assignments),
                        userId, currentTime));
            } catch (DatabaseAccessException e) {
                e.printStackTrace();
                if (!e.isRecoverable()) {
                    throw e;
                }
            } catch (AuthenticationException e) {
                if (e.getType() != AuthenticationException.INVALID_DATE) {
                    throw e;
                }
            }
        }
        return allAssignments;
    }

    @Override
    public ArrayList<Lecture> getLecture(final List<String> lectureId, final String userId) throws AuthenticationException,
            DatabaseAccessException {
        final long currentTime = System.currentTimeMillis();
        final ArrayList<Lecture> allLectures = new ArrayList<Lecture>();
        for (int lectures = lectureId.size() - 1; lectures >= 0; lectures--) {
            try {
                allLectures.add(LectureManager.mongoGetLecture(getInstance().auth, getInstance().database, lectureId.get(lectures),
                        userId, currentTime));
            } catch (DatabaseAccessException e) {
                e.printStackTrace();
                if (!e.isRecoverable()) {
                    throw e;
                }
            } catch (AuthenticationException e) {
                e.printStackTrace(System.err);
                if (e.getType() != AuthenticationException.INVALID_DATE) {
                    throw e;
                }
            }
        }
        return allLectures;
    }

    @Override
    public ArrayList<LectureSlide> getLectureSlide(final List<String> slideId, final String userId) throws AuthenticationException,
            DatabaseAccessException {
        final long currentTime = System.currentTimeMillis();
        final ArrayList<LectureSlide> allSlides = new ArrayList<LectureSlide>();
        for (int slides = slideId.size() - 1; slides >= 0; slides--) {
            try {
                allSlides.add(SlideManager.mongoGetLectureSlide(getInstance().auth, getInstance().database, slideId.get(slides),
                        userId, currentTime));
            } catch (DatabaseAccessException e) {
                e.printStackTrace();
                if (!e.isRecoverable()) {
                    throw e;
                }
            } catch (AuthenticationException e) {
                e.printStackTrace(System.err);
                if (e.getType() != AuthenticationException.INVALID_DATE) {
                    throw e;
                }
            }
        }
        return allSlides;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * database.institution.mongo.Institution#mongoGetProblem(java.util.List,
     * java.lang.String)
     */
    @Override
    public ArrayList<SrlBankProblem> getProblem(final List<String> problemID, final String userId) throws AuthenticationException {
        final ArrayList<SrlBankProblem> allProblems = new ArrayList<SrlBankProblem>();
        for (int problem = problemID.size() - 1; problem >= 0; problem--) {
            allProblems.add(BankProblemManager.mongoGetBankProblem(getInstance().auth, getInstance().database, problemID.get(problem), userId));
        }
        return allProblems;
    }

    /*
     * (non-Javadoc)
     *
     * @see database.institution.mongo.Institution#getAllPublicCourses()
     */
    @Override
    public List<SrlCourse> getAllPublicCourses() {
        return CourseManager.mongoGetAllPublicCourses(getInstance().database);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * database.institution.mongo.Institution#mongoInsertCourse(java.lang.String
     * , protobuf.srl.school.School.SrlCourse)
     */
    @Override
    public String insertCourse(final String userId, final SrlCourse course) throws DatabaseAccessException {

        // Creates the default permissions for the courses.
        SrlPermission permission = null;
        if (course.hasAccessPermission()) {
            permission = course.getAccessPermission();
        }

        final SrlGroup.Builder courseGroup = SrlGroup.newBuilder();
        courseGroup.addAdmin(userId);
        courseGroup.setGroupName(course.getName() + "_User");
        courseGroup.clearUserId();
        if (permission != null && permission.getUserPermissionCount() > 0) {
            courseGroup.addAllUserId(permission.getUserPermissionList());
        }
        final String userGroupId = GroupManager.mongoInsertGroup(getInstance().database, courseGroup.buildPartial());

        courseGroup.setGroupName(course.getName() + "_Mod");
        courseGroup.clearUserId();
        if (permission != null && permission.getModeratorPermissionCount() > 0) {
            courseGroup.addAllUserId(permission.getModeratorPermissionList());
        }
        final String modGroupId = GroupManager.mongoInsertGroup(getInstance().database, courseGroup.buildPartial());

        courseGroup.setGroupName(course.getName() + "_Admin");
        courseGroup.clearUserId();
        if (permission != null && permission.getAdminPermissionCount() > 0) {
            courseGroup.addAllUserId(permission.getAdminPermissionList());
        }
        courseGroup.addUserId(userId); // an admin will always exist
        final String adminGroupId = GroupManager.mongoInsertGroup(getInstance().database, courseGroup.buildPartial());

        // overwrites the existing permissions with the new user specific course
        // permission
        final SrlCourse.Builder builder = SrlCourse.newBuilder(course);
        final SrlPermission.Builder permissions = SrlPermission.newBuilder();
        permissions.addAdminPermission(GROUP_PREFIX + adminGroupId);
        permissions.addModeratorPermission(GROUP_PREFIX + modGroupId);
        permissions.addUserPermission(GROUP_PREFIX + userGroupId);
        builder.setAccessPermission(permissions.build());
        final String resultId = CourseManager.mongoInsertCourse(getInstance().database, builder.buildPartial());

        // links the course to the group!
        CourseManager.mongoInsertDefaultGroupId(getInstance().database, resultId, userGroupId, modGroupId, adminGroupId);

        // adds the course to the users list
        final boolean success = this.putUserInCourse(resultId, userId);
        if (!success) {
            throw new DatabaseAccessException("No success: ", false);
        }

        // FUTURE: try to undo what has been done! (and more error handling!)

        return resultId;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * database.institution.mongo.Institution#mongoInsertAssignment(java.lang
     * .String, protobuf.srl.school.School.SrlAssignment)
     */
    @Override
    public String insertAssignment(final String userId, final SrlAssignment assignment) throws AuthenticationException, DatabaseAccessException {
        final String resultId = AssignmentManager.mongoInsertAssignment(getInstance().auth, getInstance().database, userId, assignment);

        final List<String>[] ids = CourseManager.mongoGetDefaultGroupList(getInstance().database, assignment.getCourseId());
        AssignmentManager.mongoInsertDefaultGroupId(getInstance().database, resultId, ids);

        return resultId;
    }

    @Override
    public String insertLecture(final String userId, final Lecture lecture) throws AuthenticationException, DatabaseAccessException {
        final String resultId = LectureManager.mongoInsertLecture(getInstance().auth, getInstance().database, userId, lecture);

        final List<String>[] ids = CourseManager.mongoGetDefaultGroupList(getInstance().database, lecture.getCourseId());
        LectureManager.mongoInsertDefaultGroupId(getInstance().database, resultId, ids);

        return resultId;
    }

    @Override
    public String insertLectureSlide(final String userId, final LectureSlide lectureSlide) throws AuthenticationException, DatabaseAccessException {
        return SlideManager.mongoInsertSlide(getInstance().auth, getInstance().database, userId, lectureSlide);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * database.institution.mongo.Institution#mongoInsertCourseProblem(java.
     * lang.String, protobuf.srl.school.School.SrlProblem)
     */
    @Override
    public String insertCourseProblem(final String userId, final SrlProblem problem) throws AuthenticationException, DatabaseAccessException {
        final String resultId = CourseProblemManager.mongoInsertCourseProblem(getInstance().auth, getInstance().database, userId, problem);

        final List<String>[] ids = AssignmentManager.mongoGetDefaultGroupId(getInstance().database, problem.getAssignmentId());
        CourseProblemManager.mongoInsertDefaultGroupId(getInstance().database, resultId, ids);
        return resultId;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * database.institution.mongo.Institution#mongoInsertBankProblem(java.lang
     * .String, protobuf.srl.school.School.SrlBankProblem)
     */
    @Override
    public String insertBankProblem(final String userId, final SrlBankProblem problem) throws AuthenticationException {
        return BankProblemManager.mongoInsertBankProblem(getInstance().database, problem);
    }

    @Override
    public void updateLecture(final String userId, final Lecture lecture) throws AuthenticationException, DatabaseAccessException {
        LectureManager.mongoUpdateLecture(getInstance().auth, getInstance().database, lecture.getId(), userId, lecture);
    }

    @Override
     public void updateCourse(final String userId, final SrlCourse course) throws AuthenticationException, DatabaseAccessException {
        CourseManager.mongoUpdateCourse(getInstance().auth, getInstance().database, course.getId(), userId, course);
    }

    @Override
    public void updateAssignment(final String userId, final SrlAssignment assignment) throws AuthenticationException, DatabaseAccessException {
        AssignmentManager.mongoUpdateAssignment(getInstance().auth, getInstance().database, assignment.getId(), userId, assignment);
    }

    @Override
    public void updateLectureSlide(final String userId, final LectureSlide lectureSlide) throws AuthenticationException, DatabaseAccessException {
        SlideManager.mongoUpdateLectureSlide(getInstance().auth, getInstance().database, lectureSlide.getId(), userId, lectureSlide);
    }

    @Override
    public boolean putUserInCourse(final String courseId, final String userId) throws DatabaseAccessException {
        // this actually requires getting the data from the course itself
        final String userGroupId = CourseManager.mongoGetDefaultGroupId(getInstance().database, courseId)[2]; // user
                                                                                                        // group!

        // FIXME: when mongo version 2.5.5 java client comes out please change
        // this!
        final ArrayList<String> hack = new ArrayList<String>();
        hack.add(GROUP_PREFIX + userGroupId);
        if (getInstance().auth.checkAuthentication(userId, hack)) {
            return false;
        }
        // DO NOT USE THIS CODE ANY WHERE ESLE
        final DBRef myDbRef = new DBRef(getInstance().database, USER_GROUP_COLLECTION, new ObjectId(userGroupId));
        final DBObject corsor = myDbRef.fetch();
        final DBCollection courses = getInstance().database.getCollection(USER_GROUP_COLLECTION);
        final BasicDBObject object = new BasicDBObject("$addToSet", new BasicDBObject(USER_LIST, userId));
        courses.update(corsor, object);

        UserClient.addCourseToUser(userId, courseId);
        return true;
    }

    @Override
    public ArrayList<SrlCourse> getUserCourses(final String userId) throws AuthenticationException, DatabaseAccessException {
        return this.getCourses(UserClient.getUserCourses(userId), userId);
    }

    @Override
    public void insertSubmission(final String userId, final String problemId, final String submissionId,
            final boolean experiment)
            throws DatabaseAccessException {
        SubmissionManager.mongoInsertSubmission(getInstance().database, userId, problemId, submissionId, experiment);
    }

    @Override
    public void getExperimentAsUser(final String userId, final String problemId, final String sessionInfo,
            final MultiConnectionManager internalConnections) throws DatabaseAccessException {
        System.out.println("Getting experiment for user: " + userId + " problem: " + problemId);
        SubmissionManager.mongoGetExperiment(getInstance().database, userId, problemId, sessionInfo, internalConnections);
    }

    @Override
    public void getExperimentAsInstructor(final String userId, final String problemId, final String sessionInfo,
            final MultiConnectionManager internalConnections, final ByteString review) throws DatabaseAccessException, AuthenticationException {
        SubmissionManager.mongoGetAllExperimentsAsInstructor(getInstance().auth, getInstance().database, userId, problemId, sessionInfo,
                internalConnections, review);
    }
}
