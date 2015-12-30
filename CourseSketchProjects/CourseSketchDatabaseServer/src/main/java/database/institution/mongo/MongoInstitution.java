package database.institution.mongo;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import coursesketch.database.auth.AuthenticationException;
import coursesketch.database.auth.AuthenticationUpdater;
import coursesketch.database.auth.Authenticator;
import coursesketch.database.identity.IdentityManagerInterface;
import coursesketch.database.interfaces.AbstractCourseSketchDatabaseReader;
import coursesketch.database.submission.SubmissionManagerInterface;
import coursesketch.identity.IdentityWebSocketClient;
import coursesketch.server.authentication.HashManager;
import coursesketch.server.interfaces.AbstractServerWebSocketHandler;
import coursesketch.server.interfaces.MultiConnectionManager;
import coursesketch.server.interfaces.ServerInfo;
import coursesketch.services.submission.SubmissionWebSocketClient;
import database.DatabaseAccessException;
import database.institution.Institution;
import database.submission.SubmissionManager;
import database.user.UserClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protobuf.srl.lecturedata.Lecturedata.Lecture;
import protobuf.srl.lecturedata.Lecturedata.LectureSlide;
import protobuf.srl.request.Message;
import protobuf.srl.school.School;
import protobuf.srl.school.School.SrlAssignment;
import protobuf.srl.school.School.SrlBankProblem;
import protobuf.srl.school.School.SrlCourse;
import protobuf.srl.school.School.SrlProblem;
import protobuf.srl.submission.Submission;
import utilities.LoggingConstants;

import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import static database.DatabaseStringConstants.DATABASE;
import static database.DatabaseStringConstants.SELF_ID;
import static database.DatabaseStringConstants.UPDATE_COLLECTION;
import static database.DatabaseStringConstants.USER_COLLECTION;

/**
 * A Mongo implementation of the Institution it inserts and gets courses as
 * needed.
 *
 * @author gigemjt
 */
@SuppressWarnings({ "PMD.CommentRequired", "PMD.TooManyMethods" })
public final class MongoInstitution extends AbstractCourseSketchDatabaseReader implements Institution {

    /**
     * Declaration and Definition of Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MongoInstitution.class);

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
     * Used to change authentication values.
     */
    private final AuthenticationUpdater authUpdater;

    /**
     * Used to get user identity information.
     */
    private final IdentityManagerInterface identityManager;

    /**
     * A private Database that stores all of the data used by mongo.
     */
    private DB database;

    /**
     * Creates a mongo institution based on the server info.
     *
     * @param info Server information.
     * @param authenticator What is used to authenticate access to the different resources.
     * @param authUpdater Used to change authentication data.
     * @param identityManagerInterface @see {@link #identityManager}
     */
    public MongoInstitution(final ServerInfo info, final Authenticator authenticator, final AuthenticationUpdater authUpdater,
            final IdentityManagerInterface identityManagerInterface) {
        super(info);
        auth = authenticator;
        this.authUpdater = authUpdater;
        this.identityManager = identityManagerInterface;
    }

    /**
     * This is only used for testing and references the test database not the real database.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Double-checked_locking">Double Checked Locking</a>.
     * @param authenticator What is used to authenticate access to the different resources.
     * @return An instance of the mongo client. Creates it if it does not exist.
     */
    @Deprecated
    @SuppressWarnings("checkstyle:innerassignment")
    public static MongoInstitution getInstance(final Authenticator authenticator) {
        MongoInstitution result = instance;
        if (result == null) {
            synchronized (MongoInstitution.class) {
                if (result == null) {
                    result = instance;
                    instance = result = new MongoInstitution(ServerInfo.createDefaultServerInfo(), authenticator, null, null);
                    result.auth = authenticator;
                }
            }
        }
        return result;
    }

    /**
     * Called when startDatabase is called if the database has not already been started.
     *
     * This method should be synchronous.
     */
    @Override protected void onStartDatabase() {
        final MongoClient mongoClient = new MongoClient(super.getServerInfo().getDatabaseUrl());
        database = mongoClient.getDB(super.getServerInfo().getDatabaseName());
        super.setDatabaseStarted();
    }

    /**
     * Used only for the purpose of testing. Overwrites the instance with a test instance that has access to a test database.
     *
     * Because we only want the database set once it has to be set in the constructor.
     * We also want the class to be final so the test code has to be here.
     * @param testOnly
     *         if true it uses the test database. Otherwise it uses the real
     *         name of the database.
     * @param fakeDB The fake database.
     * @param authenticator What is used to authenticate access to the different resources.
     * @param authUpdater Used to change authentication data.
     * @param identityManagerInterface @see {@link #identityManager}
     */
    public MongoInstitution(final boolean testOnly, final DB fakeDB, final Authenticator authenticator, final AuthenticationUpdater authUpdater,
            final IdentityManagerInterface identityManagerInterface) {
        this(null, authenticator, authUpdater, identityManagerInterface);
        if (testOnly && fakeDB != null) {
            database = fakeDB;
        } else {
            MongoClient mongoClient = null;
            try {
                mongoClient = new MongoClient("localhost");
            } catch (UnknownHostException e) {
                LOG.error(LoggingConstants.EXCEPTION_MESSAGE, e);
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
        instance.auth = authenticator;
    }

    @Override
    public void setUpIndexes() {
        LOG.info("Setting up the indexes");
        database.getCollection(USER_COLLECTION).ensureIndex(new BasicDBObject(SELF_ID, 1).append("unique", true));
        database.getCollection(UPDATE_COLLECTION).ensureIndex(new BasicDBObject(SELF_ID, 1).append("unique", true));
    }

    @Override
    public ArrayList<SrlCourse> getCourses(final String authId, final List<String> courseIds) throws AuthenticationException,
            DatabaseAccessException {
        final long currentTime = System.currentTimeMillis();
        final ArrayList<SrlCourse> allCourses = new ArrayList<SrlCourse>();
        for (String courseId : courseIds) {
            allCourses.add(CourseManager.mongoGetCourse(auth, database, authId, courseId, currentTime));
        }
        LOG.debug("{} Courses were loaded from the database for user {}", allCourses.size(), authId);
        return allCourses;
    }

    @Override
    public ArrayList<SrlProblem> getCourseProblem(final String authId, final List<String> problemID) throws AuthenticationException,
            DatabaseAccessException {
        final long currentTime = System.currentTimeMillis();
        final ArrayList<SrlProblem> allCourses = new ArrayList<SrlProblem>();
        for (int index = 0; index < problemID.size(); index++) {
            try {
                allCourses.add(CourseProblemManager.mongoGetCourseProblem(
                        auth, database, authId, problemID.get(index), currentTime));
            } catch (DatabaseAccessException e) {
                LOG.error(LoggingConstants.EXCEPTION_MESSAGE, e);
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

    @Override
    public ArrayList<SrlAssignment> getAssignment(final String authId, final List<String> assignmentID) throws AuthenticationException,
            DatabaseAccessException {
        final long currentTime = System.currentTimeMillis();
        final ArrayList<SrlAssignment> allAssignments = new ArrayList<SrlAssignment>();
        for (int assignments = assignmentID.size() - 1; assignments >= 0; assignments--) {
            try {
                allAssignments.add(AssignmentManager.mongoGetAssignment(
                        auth, database, authId, assignmentID.get(assignments), currentTime));
            } catch (DatabaseAccessException e) {
                LOG.error(LoggingConstants.EXCEPTION_MESSAGE, e);
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
    public ArrayList<Lecture> getLecture(final String authId, final List<String> lectureId) throws AuthenticationException,
            DatabaseAccessException {
        final long currentTime = System.currentTimeMillis();
        final ArrayList<Lecture> allLectures = new ArrayList<Lecture>();
        for (int lectures = lectureId.size() - 1; lectures >= 0; lectures--) {
            try {
                allLectures.add(LectureManager.mongoGetLecture(auth, database, lectureId.get(lectures),
                        authId, currentTime));
            } catch (DatabaseAccessException e) {
                LOG.error(LoggingConstants.EXCEPTION_MESSAGE, e);
                if (!e.isRecoverable()) {
                    throw e;
                }
            } catch (AuthenticationException e) {
                LOG.error(LoggingConstants.EXCEPTION_MESSAGE, e);
                if (e.getType() != AuthenticationException.INVALID_DATE) {
                    throw e;
                }
            }
        }
        return allLectures;
    }

    @Override
    public ArrayList<LectureSlide> getLectureSlide(final String authId, final List<String> slideId) throws AuthenticationException,
            DatabaseAccessException {
        final long currentTime = System.currentTimeMillis();
        final ArrayList<LectureSlide> allSlides = new ArrayList<LectureSlide>();
        for (int slides = slideId.size() - 1; slides >= 0; slides--) {
            try {
                allSlides.add(SlideManager.mongoGetLectureSlide(auth, database, slideId.get(slides),
                        authId, currentTime));
            } catch (DatabaseAccessException e) {
                LOG.error(LoggingConstants.EXCEPTION_MESSAGE, e);
                if (!e.isRecoverable()) {
                    throw e;
                }
            } catch (AuthenticationException e) {
                LOG.error(LoggingConstants.EXCEPTION_MESSAGE, e);
                if (e.getType() != AuthenticationException.INVALID_DATE) {
                    throw e;
                }
            }
        }
        return allSlides;
    }

    @Override
    public ArrayList<SrlBankProblem> getProblem(final String authId, final List<String> problemID)
            throws AuthenticationException, DatabaseAccessException {
        final ArrayList<SrlBankProblem> allProblems = new ArrayList<>();
        for (int problem = problemID.size() - 1; problem >= 0; problem--) {
            allProblems.add(BankProblemManager.mongoGetBankProblem(
                    auth, database, authId, problemID.get(problem)));
        }
        return allProblems;
    }

    @Override
    public List<SrlCourse> getAllPublicCourses() {
        return CourseManager.mongoGetAllPublicCourses(database);
    }

    @Override
    public String insertCourse(final String userId, final String authId, final SrlCourse course) throws DatabaseAccessException {
        final String registrationId = AbstractServerWebSocketHandler.Encoder.nextID().toString();

        LOG.debug("Course is being inserted with registration key {}", registrationId);
        // we first add the registration key before we add it to the database.
        final String resultId = CourseManager.mongoInsertCourse(database, SrlCourse.newBuilder(course).setRegistrationKey(registrationId).build());

        try {
            authUpdater.createNewItem(authId, resultId, School.ItemType.COURSE, null, registrationId);
        } catch (AuthenticationException e) {
            // Revert the adding of the course to the database!
            throw new DatabaseAccessException("Problem creating authentication data", e);
        }

        try {
            identityManager.createNewItem(userId, authId, resultId, School.ItemType.COURSE, null, auth);
        } catch (AuthenticationException | DatabaseAccessException e) {
            // Revert the adding of the course to the database!
            throw new DatabaseAccessException("Problem creating userData", e);
        }

        // adds the course to the users list
        UserClient.addCourseToUser(authId, resultId);

        // FUTURE: try to undo what has been done! (and more error handling!)

        return resultId;
    }

    @Override
    public String insertAssignment(final String userId, final String authId, final SrlAssignment assignment) throws AuthenticationException,
            DatabaseAccessException {
        final String resultId = AssignmentManager.mongoInsertAssignment(auth, database, authId, assignment);

        try {
            authUpdater.createNewItem(authId, resultId, School.ItemType.ASSIGNMENT, assignment.getCourseId(), null);
        } catch (AuthenticationException e) {
            // Revert the adding of the course to the database!
            throw new AuthenticationException("Failed to create auth data while inserting assignment", e);
        }

        try {
            identityManager.createNewItem(userId, authId, resultId, School.ItemType.ASSIGNMENT, assignment.getCourseId(), auth);
        } catch (AuthenticationException | DatabaseAccessException e) {
            // Revert the adding of the course to the database!
            throw new DatabaseAccessException("Problem creating identity data while inserting assignment", e);
        }

        return resultId;
    }

    @Override
    public String insertLecture(final String userId, final String authId, final Lecture lecture) throws AuthenticationException,
            DatabaseAccessException {
        final String resultId = LectureManager.mongoInsertLecture(auth, database, authId, lecture);

        final List<String>[] ids = CourseManager.mongoGetDefaultGroupList(database, lecture.getCourseId());
        LectureManager.mongoInsertDefaultGroupId(database, resultId, ids);

        return resultId;
    }

    @Override
    public String insertLectureSlide(final String authId, final LectureSlide lectureSlide) throws AuthenticationException, DatabaseAccessException {
        return SlideManager.mongoInsertSlide(auth, database, authId, lectureSlide);
    }

    @Override
    public String insertCourseProblem(final String userId, final String authId, final SrlProblem problem) throws AuthenticationException,
            DatabaseAccessException {
        final String resultId = CourseProblemManager.mongoInsertCourseProblem(auth, database, authId, problem);

        if (problem.hasProblemBankId()) {
            putCourseInBankProblem(authId, problem.getCourseId(), problem.getProblemBankId(), null);
        }

        try {
            authUpdater.createNewItem(authId, resultId, School.ItemType.COURSE_PROBLEM, problem.getAssignmentId(), null);
        } catch (AuthenticationException e) {
            // Revert the adding of the course to the database!
            throw new AuthenticationException("Faild to create auth data while inserting course problem", e);
        }

        try {
            identityManager.createNewItem(userId, authId, resultId, School.ItemType.COURSE_PROBLEM, problem.getAssignmentId(), auth);
        } catch (AuthenticationException | DatabaseAccessException e) {
            // Revert the adding of the course to the database!
            throw new DatabaseAccessException("Problem creating identity data while inserting course problem", e);
        }

        return resultId;
    }

    @Override
    public String insertBankProblem(final String userId, final String authId, final SrlBankProblem problem)
            throws AuthenticationException, DatabaseAccessException {

        final String registrationId = AbstractServerWebSocketHandler.Encoder.nextID().toString();

        LOG.debug("Bank problem is being inserted with registration key {}", registrationId);
        // we first add the registration key before we add it to the database.
        final String resultId = BankProblemManager.mongoInsertBankProblem(database, SrlBankProblem.newBuilder(problem)
                .setRegistrationKey(registrationId).build());

        authUpdater.createNewItem(authId, resultId, School.ItemType.BANK_PROBLEM, null, registrationId);

        try {
            identityManager.createNewItem(userId, authId, resultId, School.ItemType.BANK_PROBLEM, null, auth);
        } catch (AuthenticationException | DatabaseAccessException e) {
            // Revert the adding of the course to the database!
            throw new DatabaseAccessException("Problem creating identity data while inserting bank problem", e);
        }

        return resultId;
    }

    @Override
    public void updateLecture(final String authId, final Lecture lecture) throws AuthenticationException, DatabaseAccessException {
        LectureManager.mongoUpdateLecture(auth, database, lecture.getId(), authId, lecture);
    }

    @Override
    public void updateCourse(final String authId, final SrlCourse course) throws AuthenticationException, DatabaseAccessException {
        CourseManager.mongoUpdateCourse(auth, database, authId, course.getId(), course);
    }

    @Override
    public void updateAssignment(final String authId, final SrlAssignment assignment) throws AuthenticationException, DatabaseAccessException {
        AssignmentManager.mongoUpdateAssignment(auth, database, authId, assignment.getId(), assignment);
    }

    @Override
    public void updateCourseProblem(final String authId, final SrlProblem srlProblem) throws AuthenticationException, DatabaseAccessException {
        CourseProblemManager.mongoUpdateCourseProblem(auth, database, authId, srlProblem.getId(), srlProblem);

        if (srlProblem.hasProblemBankId()) {
            putCourseInBankProblem(authId, srlProblem.getCourseId(), srlProblem.getProblemBankId(), null);
        }
    }

    @Override
    public void updateBankProblem(final String authId, final SrlBankProblem srlBankProblem) throws AuthenticationException, DatabaseAccessException {
        BankProblemManager.mongoUpdateBankProblem(auth, database, authId, srlBankProblem.getId(), srlBankProblem);
    }

    @Override
    public void updateLectureSlide(final String authId, final LectureSlide lectureSlide) throws AuthenticationException, DatabaseAccessException {
        SlideManager.mongoUpdateLectureSlide(auth, database, lectureSlide.getId(), authId, lectureSlide);
    }

    @Override
     public boolean putUserInCourse(final String userId, final String authId, final String courseId, final String clientRegistrationKey)
            throws DatabaseAccessException, AuthenticationException {

        String registrationKey = clientRegistrationKey;
        if (Strings.isNullOrEmpty(clientRegistrationKey)) {
            LOG.debug("Registration key was not sent from client.  Trying to get it from course itself.");
            registrationKey = CourseManager.mongoGetRegistrationKey(auth, database, authId, courseId, false);
        }
        try {
            LOG.debug("Registration user with registration key {} into course {}", registrationKey, courseId);
            authUpdater.registerUser(authId, courseId, School.ItemType.COURSE, registrationKey);
        } catch (AuthenticationException e) {
            // Revert the adding of the course to the database!
            throw new AuthenticationException("Failed to register the user in the course", e);
        }

        try {
            LOG.debug("Registration user with userId key {} into course {}", userId, courseId);
            identityManager.registerUserInItem(userId, authId, courseId, School.ItemType.COURSE, auth);
        } catch (AuthenticationException | DatabaseAccessException e) {
            // Revert the adding of the course to the database!
            throw new DatabaseAccessException("Failed to register the user in the course", e);
        }

        UserClient.addCourseToUser(authId, courseId);
        return true;
    }

    @Override
    public boolean putCourseInBankProblem(final String authId, final String courseId, final String bankProblemId,
            final String clientRegistrationKey)
            throws DatabaseAccessException, AuthenticationException {

        String registrationKey = clientRegistrationKey;
        if (Strings.isNullOrEmpty(clientRegistrationKey)) {
            LOG.debug("Registration key was not sent from client.  Trying to get it from bank problem itself.");
            registrationKey = BankProblemManager.mongoGetRegistrationKey(auth, database, authId, bankProblemId);
        }

        try {
            LOG.debug("Registration user with registration key {} into bank problem {}", registrationKey, bankProblemId);
            authUpdater.registerUser(courseId, bankProblemId, School.ItemType.BANK_PROBLEM, registrationKey);
        } catch (AuthenticationException e) {
            // Revert the adding of the course to the database!
            throw new AuthenticationException("Failed to register the user in the bank problem", e);
        }

        try {
            LOG.debug("Registration user with userId key {} into bank problem  {}", courseId, bankProblemId);
            identityManager.registerUserInItem(courseId, authId, bankProblemId, School.ItemType.BANK_PROBLEM, auth);
        } catch (AuthenticationException | DatabaseAccessException e) {
            // Revert the adding of the course to the database!
            throw new DatabaseAccessException("Failed to register the user in the bank problem", e);
        }
        return true;
    }

    @Override
    public ArrayList<SrlCourse> getUserCourses(final String authId) throws AuthenticationException, DatabaseAccessException {
        return this.getCourses(authId, UserClient.getUserCourses(authId));
    }

    @Override
    public void insertSubmission(final String userId, final String authId, final String problemId, final String submissionId,
            final boolean experiment)
            throws DatabaseAccessException {
        SubmissionManager.mongoInsertSubmission(database, userId, problemId, submissionId, experiment);
    }

    @Override
    public Submission.SrlExperiment getExperimentAsUser(final String userId, final String authId, final String problemId,
            final SubmissionManagerInterface submissionManager) throws DatabaseAccessException, AuthenticationException {
        LOG.debug("Getting experiment for user: {}", userId);
        LOG.info("Problem: {}", problemId);

        final String courseId = this.getCourseProblem(authId, Lists.newArrayList(problemId)).get(0).getCourseId();
        return SubmissionManager.mongoGetExperiment(auth, database, userId, authId, courseId, problemId, submissionManager);
    }

    @Override
    public List<Submission.SrlExperiment> getExperimentAsInstructor(final String authId, final String problemId, final Message.Request sessionInfo,
            final MultiConnectionManager internalConnections, final ByteString review) throws DatabaseAccessException, AuthenticationException {
        return SubmissionManager.mongoGetAllExperimentsAsInstructor(auth, database, authId, problemId,
                internalConnections.getBestConnection(SubmissionWebSocketClient.class),
                internalConnections.getBestConnection(IdentityWebSocketClient.class));
    }

    @Override
    public List<SrlBankProblem> getAllBankProblems(final String authId, final String courseId, final int page)
            throws AuthenticationException, DatabaseAccessException {
        return BankProblemManager.mongoGetAllBankProblems(auth, database, authId, courseId, page);
    }

    /**
     * Hashes a userId based on the courseId.
     *
     * Only hashed user Ids are stored in the database.
     * @param userId The userId that is being hashed.
     * @param courseId The courseId that is being used as a salt.
     * @return A hashed version of this Id.
     * @throws AuthenticationException Thrown if there are problems creating the hash.
     */
    public static String hashUserId(final String userId, final String courseId) throws AuthenticationException {
        try {
            return HashManager.toHex(HashManager.createHash(userId, HashManager.generateUnSecureSalt(courseId)).getBytes());
        } catch (NoSuchAlgorithmException e) {
            throw new AuthenticationException(e);
        }
    }
}
