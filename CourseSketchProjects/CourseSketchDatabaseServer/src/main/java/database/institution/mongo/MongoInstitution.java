package database.institution.mongo;

import com.google.common.base.Strings;
import com.google.protobuf.ByteString;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import coursesketch.database.auth.AuthenticationException;
import coursesketch.database.auth.AuthenticationUpdater;
import coursesketch.database.auth.Authenticator;
import coursesketch.database.identity.IdentityManagerInterface;
import coursesketch.database.interfaces.AbstractCourseSketchDatabaseReader;
import coursesketch.server.interfaces.AbstractServerWebSocketHandler;
import coursesketch.server.interfaces.MultiConnectionManager;
import coursesketch.server.interfaces.ServerInfo;
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
import utilities.LoggingConstants;

import java.net.UnknownHostException;
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
    private final AuthenticationUpdater updater;

    private final IdentityManagerInterface identityManager;

    /**
     * A private Database that stores all of the data used by mongo.
     */
    private DB database;

    /**
     * Creates a mongo institution based on the server info.
     * @param info Server information.
     * @param authenticator What is used to authenticate access to the different resources.
     * @param updater Used to change authentication data.
     */
    public MongoInstitution(final ServerInfo info, final Authenticator authenticator, final AuthenticationUpdater updater,
            final IdentityManagerInterface identityManagerInterface) {
        super(info);
        auth = authenticator;
        this.updater = updater;
        this.identityManager = identityManagerInterface;
    }

    /**
     * @return An instance of the mongo client. Creates it if it does not exist.
     *
     * This is only used for testing and references the test database not the real database.
     *
     * @see <a href="http://en.wikipedia.org/wiki/Double-checked_locking">Double Checked Locking</a>.
     * @param authenticator What is used to authenticate access to the different resources.
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
     * Used only for the purpose of testing overwrite the instance with a test
     * instance that can only access a test database.
     * @param testOnly
     *         if true it uses the test database. Otherwise it uses the real
     *         name of the database.
     * @param fakeDB The fake database.
     * @param authenticator What is used to authenticate access to the different resources.
     * @param updater Used to change authentication data.
     */
    public MongoInstitution(final boolean testOnly, final DB fakeDB, final Authenticator authenticator, final AuthenticationUpdater updater,
            final IdentityManagerInterface identityManagerInterface) {
        this(null, authenticator, updater, identityManagerInterface);
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
            allCourses.add(CourseManager.mongoGetCourse(auth, database, courseId, authId, currentTime));
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
                        auth, database, problemID.get(index), authId, currentTime));
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
                        auth, database, assignmentID.get(assignments), authId, currentTime));
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
                    auth, database, problemID.get(problem), authId));
        }
        return allProblems;
    }

    @Override
    public List<SrlCourse> getAllPublicCourses() {
        return CourseManager.mongoGetAllPublicCourses(database);
    }

    @Override
    public String insertCourse(final String authId, final SrlCourse course) throws DatabaseAccessException {
        final String registrationId = AbstractServerWebSocketHandler.Encoder.nextID().toString();

        LOG.debug("Course is being inserted with registration key {}", registrationId);
        // we first add the registration key before we add it to the database.
        final String resultId = CourseManager.mongoInsertCourse(database, SrlCourse.newBuilder(course).setRegistrationKey(registrationId).build());

        try {
            updater.createNewItem(School.ItemType.COURSE, resultId, null, authId, registrationId);
        } catch (AuthenticationException e) {
            // Revert the adding of the course to the database!
            throw new DatabaseAccessException("Problem creating authentication data", e);
        }

        // adds the course to the users list
        UserClient.addCourseToUser(authId, resultId);

        // FUTURE: try to undo what has been done! (and more error handling!)

        return resultId;
    }

    @Override
    public String insertAssignment(final String authId, final SrlAssignment assignment) throws AuthenticationException, DatabaseAccessException {
        final String resultId = AssignmentManager.mongoInsertAssignment(auth, database, authId, assignment);

        try {
            updater.createNewItem(School.ItemType.ASSIGNMENT, resultId, assignment.getCourseId(), authId, null);
        } catch (AuthenticationException e) {
            // Revert the adding of the course to the database!
            throw new AuthenticationException("Failed to create auth data while inserting assignment", e);
        }

        return resultId;
    }

    @Override
    public String insertLecture(final String authId, final Lecture lecture) throws AuthenticationException, DatabaseAccessException {
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
    public String insertCourseProblem(final String authId, final SrlProblem problem) throws AuthenticationException, DatabaseAccessException {
        final String resultId = CourseProblemManager.mongoInsertCourseProblem(auth, database, authId, problem);

        if (problem.hasProblemBankId()) {
            putCourseInBankProblem(authId, problem.getCourseId(), problem.getProblemBankId(), null);
        }

        try {
            updater.createNewItem(School.ItemType.COURSE_PROBLEM, resultId, problem.getAssignmentId(), authId, null);
        } catch (AuthenticationException e) {
            // Revert the adding of the course to the database!
            throw new AuthenticationException("Faild to create auth data while inserting course problem", e);
        }

        return resultId;
    }

    @Override
    public String insertBankProblem(final String authId, final SrlBankProblem problem) throws AuthenticationException {

        final String registrationId = AbstractServerWebSocketHandler.Encoder.nextID().toString();

        LOG.debug("Course is being inserted with registration key {}", registrationId);
        // we first add the registration key before we add it to the database.
        final String resultId = BankProblemManager.mongoInsertBankProblem(database, SrlBankProblem.newBuilder(problem)
                .setRegistrationKey(registrationId).build());

        updater.createNewItem(School.ItemType.BANK_PROBLEM, resultId, null, authId, registrationId);

        return resultId;
    }

    @Override
    public void updateLecture(final String authId, final Lecture lecture) throws AuthenticationException, DatabaseAccessException {
        LectureManager.mongoUpdateLecture(auth, database, lecture.getId(), authId, lecture);
    }

    @Override
    public void updateCourse(final String authId, final SrlCourse course) throws AuthenticationException, DatabaseAccessException {
        CourseManager.mongoUpdateCourse(auth, database, course.getId(), authId, course);
    }

    @Override
    public void updateAssignment(final String authId, final SrlAssignment assignment) throws AuthenticationException, DatabaseAccessException {
        AssignmentManager.mongoUpdateAssignment(auth, database, assignment.getId(), authId, assignment);
    }

    @Override
    public void updateCourseProblem(final String authId, final SrlProblem srlProblem) throws AuthenticationException, DatabaseAccessException {
        CourseProblemManager.mongoUpdateCourseProblem(auth, database, srlProblem.getId(), authId, srlProblem);

        if (srlProblem.hasProblemBankId()) {
            putCourseInBankProblem(authId, srlProblem.getCourseId(), srlProblem.getProblemBankId(), null);
        }
    }

    @Override
    public void updateBankProblem(final String authId, final SrlBankProblem srlBankProblem) throws AuthenticationException, DatabaseAccessException {
        BankProblemManager.mongoUpdateBankProblem(auth, database, srlBankProblem.getId(), authId, srlBankProblem);
    }

    @Override
    public void updateLectureSlide(final String authId, final LectureSlide lectureSlide) throws AuthenticationException, DatabaseAccessException {
        SlideManager.mongoUpdateLectureSlide(auth, database, lectureSlide.getId(), authId, lectureSlide);
    }

    @Override
     public boolean putUserInCourse(final String authId, final String courseId, final String clientRegistrationKey)
            throws DatabaseAccessException, AuthenticationException {

        String registrationKey = clientRegistrationKey;
        if (Strings.isNullOrEmpty(clientRegistrationKey)) {
            LOG.debug("Registration key was not sent from client.  Trying to get it from course itself.");
            registrationKey = CourseManager.mongoGetRegistrationKey(auth, database, courseId, authId, false);
        }
        try {
            LOG.debug("Registration user with registration key {} into course {}", registrationKey, courseId);
            updater.registerUser(School.ItemType.COURSE, courseId, authId, registrationKey);
        } catch (AuthenticationException e) {
            // Revert the adding of the course to the database!
            throw new AuthenticationException("Failed to register the user in the course", e);
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
            LOG.debug("Registration key was not sent from client.  Trying to get it from course itself.");
            registrationKey = BankProblemManager.mongoGetRegistrationKey(auth, database, bankProblemId, authId);
        }
        try {
            LOG.debug("Registration user with registration key {} into course {}", registrationKey, courseId);
            updater.registerUser(School.ItemType.BANK_PROBLEM, bankProblemId, courseId, registrationKey);
        } catch (AuthenticationException e) {
            // Revert the adding of the course to the database!
            throw new AuthenticationException("Failed to register the user in the course", e);
        }
        return true;
    }

    @Override
    public ArrayList<SrlCourse> getUserCourses(final String authId) throws AuthenticationException, DatabaseAccessException {
        return this.getCourses(authId, UserClient.getUserCourses(authId));
    }

    @Override
    public void insertSubmission(final String userId, final String problemId, final String submissionId,
            final boolean experiment)
            throws DatabaseAccessException {
        SubmissionManager.mongoInsertSubmission(database, userId, problemId, submissionId, experiment);
    }

    @Override
    public void getExperimentAsUser(final String authId, final String problemId, final Message.Request sessionInfo,
            final MultiConnectionManager internalConnections) throws DatabaseAccessException {
        LOG.debug("Getting experiment for user: {}", authId);
        LOG.info("Problem: {}", problemId);
        SubmissionManager.mongoGetExperiment(database, authId, problemId, sessionInfo, internalConnections);
    }

    @Override
    public void getExperimentAsInstructor(final String authId, final String problemId, final Message.Request sessionInfo,
            final MultiConnectionManager internalConnections, final ByteString review) throws DatabaseAccessException, AuthenticationException {
        SubmissionManager.mongoGetAllExperimentsAsInstructor(auth, database, authId, problemId, sessionInfo,
                internalConnections, review);
    }

    @Override
    public List<SrlBankProblem> getAllBankProblems(final String authId, final String courseId, final int page)
            throws AuthenticationException, DatabaseAccessException {
        return BankProblemManager.mongoGetAllBankProblems(auth, database, authId, courseId, page);
    }
}
