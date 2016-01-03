package database.institution.mongo;

import com.coursesketch.test.utilities.AuthenticationHelper;
import com.github.fakemongo.junit.FongoRule;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import coursesketch.database.auth.*;
import database.DatabaseAccessException;
import database.RequestConverter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import protobuf.srl.grading.Grading.ProtoGrade;
import protobuf.srl.grading.Grading.GradeHistory;
import protobuf.srl.school.School;
import protobuf.srl.services.authentication.Authentication;
import protobuf.srl.utils.Util;
import protobuf.srl.school.School.SrlCourse;

import java.util.ArrayList;
import java.util.List;

import static database.DatabaseStringConstants.ASSIGNMENT_ID;
import static database.DatabaseStringConstants.COMMENT;
import static database.DatabaseStringConstants.COURSE_ID;
import static database.DatabaseStringConstants.COURSE_PROBLEM_ID;
import static database.DatabaseStringConstants.CURRENT_GRADE;
import static database.DatabaseStringConstants.EXTERNAL_GRADE;
import static database.DatabaseStringConstants.GRADED_DATE;
import static database.DatabaseStringConstants.GRADE_COLLECTION;
import static database.DatabaseStringConstants.GRADE_HISTORY;
import static database.DatabaseStringConstants.GRADE_VALUE;
import static database.DatabaseStringConstants.SELF_ID;
import static database.DatabaseStringConstants.USER_ID;
import static database.DatabaseStringConstants.WHO_CHANGED;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for GradeManager.
 * @see GradeManager
 *
 * Created by matt on 4/11/15.
 */
@RunWith(MockitoJUnitRunner.class)
public class GradeManagerTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Rule
    public FongoRule fongo = new FongoRule();
    @Mock
    AuthenticationChecker authChecker;
    @Mock
    AuthenticationOptionChecker optionChecker;

    public DB db;
    public Authenticator authenticator;
    public SrlCourse.Builder courseBuilder = SrlCourse.newBuilder();

    public ProtoGrade.Builder protoGradeGrabber = ProtoGrade.newBuilder();
    public ProtoGrade.Builder fakeProtoGrade = ProtoGrade.newBuilder();
    public GradeHistory.Builder fakeProtoHistory1 = GradeHistory.newBuilder();
    public GradeHistory.Builder fakeProtoHistory2 = GradeHistory.newBuilder();

    public BasicDBObject fakeMongoGrade = new BasicDBObject();
    public List<BasicDBObject> fakeMongoHistory = new ArrayList<>();

    public static final String FAKE_COURSE_ID = "courseId";
    public static final String FAKE_USER_ID = "userId";
    public static final String FAKE_ADMIN_ID = "adminId";
    public static final String FAKE_ASGN_ID = "assignmentId";
    public static final String FAKE_PROB_ID = "problemId";
    public static final float FAKE_CURRENT_GRADE = 95;
    public static final float FAKE_OLD_GRADE = 75;
    public static final float FAKE_NEW_GRADE = 95;
    public static final String FAKE_COMMENT = "hi";
    public static final boolean FAKE_EXTERNAL_GRADE = false;
    public static final long FAKE_OLD_MILLISECONDS = 12345;
    public static final long FAKE_NEW_MILLISECONDS = 223456;

    @Before
    public void before() {
        db = fongo.getDB();

        try {
            // general rules
            AuthenticationHelper.setMockPermissions(authChecker, null, null, null, null, Authentication.AuthResponse.PermissionLevel.NO_PERMISSION);

            when(optionChecker.authenticateDate(any(AuthenticationDataCreator.class), anyLong()))
                    .thenReturn(false);

            when(optionChecker.isItemPublished(any(AuthenticationDataCreator.class)))
                    .thenReturn(false);

            when(optionChecker.isItemRegistrationRequired(any(AuthenticationDataCreator.class)))
                    .thenReturn(true);

        } catch (DatabaseAccessException e) {
            e.printStackTrace();
        } catch (AuthenticationException e) {
            e.printStackTrace();
        }
        authenticator = new Authenticator(authChecker, optionChecker);

        /**
         * Setting up fake Proto data.
         */
        fakeProtoHistory1.setGradeValue(FAKE_OLD_GRADE).setComment(FAKE_COMMENT + "1").setWhoChanged(FAKE_ADMIN_ID)
                .setGradedDate(RequestConverter.getProtoFromMilliseconds(FAKE_OLD_MILLISECONDS));
        fakeProtoHistory2.setGradeValue(FAKE_NEW_GRADE).setComment(FAKE_COMMENT + "2").setWhoChanged(FAKE_ADMIN_ID)
                .setGradedDate(RequestConverter.getProtoFromMilliseconds(FAKE_NEW_MILLISECONDS));

        fakeProtoGrade.setCourseId(FAKE_COURSE_ID).setUserId(FAKE_USER_ID).setAssignmentId(FAKE_ASGN_ID)
                .setProblemId(FAKE_PROB_ID).setCurrentGrade(FAKE_CURRENT_GRADE).setExternalGrade(FAKE_EXTERNAL_GRADE);
        fakeProtoGrade.addGradeHistory(fakeProtoHistory1.build());
        fakeProtoGrade.addGradeHistory(fakeProtoHistory2.build());

        protoGradeGrabber.setCourseId(FAKE_COURSE_ID).setUserId(FAKE_USER_ID).setAssignmentId(FAKE_ASGN_ID)
                .setProblemId(FAKE_PROB_ID);

        /**
         * Setting up fake Mongo data.
         */
        BasicDBObject mongoHistory1 = new BasicDBObject(GRADE_VALUE, FAKE_OLD_GRADE)
                .append(COMMENT, FAKE_COMMENT + "1").append(WHO_CHANGED, FAKE_ADMIN_ID)
                .append(GRADED_DATE, FAKE_OLD_MILLISECONDS);
        BasicDBObject mongoHistory2 = new BasicDBObject(GRADE_VALUE, FAKE_NEW_GRADE)
                .append(COMMENT, FAKE_COMMENT + "2").append(WHO_CHANGED, FAKE_ADMIN_ID)
                .append(GRADED_DATE, FAKE_NEW_MILLISECONDS);
        fakeMongoHistory.add(mongoHistory1);
        fakeMongoHistory.add(mongoHistory2);

        fakeMongoGrade.append(COURSE_ID, FAKE_COURSE_ID).append(USER_ID, FAKE_USER_ID).append(ASSIGNMENT_ID, FAKE_ASGN_ID)
                .append(COURSE_PROBLEM_ID, FAKE_PROB_ID).append(CURRENT_GRADE, FAKE_CURRENT_GRADE)
                .append(GRADE_HISTORY, fakeMongoHistory).append(EXTERNAL_GRADE, FAKE_EXTERNAL_GRADE);

        courseBuilder.setId(FAKE_COURSE_ID);
    }

    @Test
    public void buildMongoGradeHistoryTest() {
        BasicDBObject mongoHistory = GradeManager.buildMongoGradeHistory(fakeProtoHistory2.build());
        Assert.assertEquals(fakeMongoHistory.get(1), mongoHistory);
    }

    @Test
    public void addGradeTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        DBObject testGrade = db.getCollection(GRADE_COLLECTION).findOne(new BasicDBObject(COURSE_ID, courseId), new BasicDBObject(SELF_ID, false));
        fakeMongoGrade.put(COURSE_ID, courseId);
        Assert.assertEquals(fakeMongoGrade, testGrade);
    }

    @Test
    public void addCourseGradeTest() throws Exception {
        // This will test a grade with no assignmentId or problemId
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.clearAssignmentId();
        fakeProtoGrade.clearProblemId();

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        DBObject testGrade = db.getCollection(GRADE_COLLECTION).findOne(new BasicDBObject(COURSE_ID, courseId), new BasicDBObject(SELF_ID, false));
        fakeMongoGrade.put(COURSE_ID, courseId);
        fakeMongoGrade.remove(ASSIGNMENT_ID);
        fakeMongoGrade.remove(COURSE_PROBLEM_ID);
        Assert.assertEquals(fakeMongoGrade, testGrade);
    }

    @Test(expected = AuthenticationException.class)
    public void userAddingGradeHasNoPermission() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);

        GradeManager.addGrade(authenticator, db, FAKE_USER_ID, fakeProtoGrade.build());
    }

    @Test // FUTURE: make this test pass (expected = DatabaseAccessException.class)
    public void userNotInCourseExternalGradeTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.setExternalGrade(true);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
    }

    @Test // FUTURE: make this test pass (expected = DatabaseAccessException.class)
    public void userNotAssignedProblemTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
    }

    @Test // FUTURE: make this test pass (expected = DatabaseAccessException.class)
    public void userNotAssignedAssignmentTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.clearProblemId();

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.ASSIGNMENT, FAKE_ASGN_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
    }

    @Test // FUTURE: make this test pass (expected = DatabaseAccessException.class)
    public void userNotInCourseTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.clearProblemId();
        fakeProtoGrade.clearAssignmentId();

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
    }

    @Test(expected = DatabaseAccessException.class)
    public void gradeHasNoHistoryTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.clearGradeHistory();

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
    }

    @Test
    public void noCurrentGradeTest1() throws Exception {
        // Tests the gradeHistory applies the correct currentGrade if it does not exist and gradeHistory is in chronological order (old to new)
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.clearCurrentGrade();

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        DBObject testGrade = db.getCollection(GRADE_COLLECTION).findOne(new BasicDBObject(COURSE_ID, courseId), new BasicDBObject(SELF_ID, false));
        fakeMongoGrade.put(COURSE_ID, courseId);
        Assert.assertEquals(fakeMongoGrade, testGrade);
    }

    @Test
    public void noCurrentGradeTest2() throws Exception {
        // Tests the gradeHistory applies the correct currentGrade if it does not exist and gradeHistory is in chronological order (new to old)
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.clearCurrentGrade();
        fakeProtoGrade.clearGradeHistory();
        fakeProtoGrade.addGradeHistory(fakeProtoHistory2);
        fakeProtoGrade.addGradeHistory(fakeProtoHistory1);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        DBObject testGrade = db.getCollection(GRADE_COLLECTION).findOne(new BasicDBObject(COURSE_ID, courseId), new BasicDBObject(SELF_ID, false));
        fakeMongoGrade.put(COURSE_ID, courseId);

        // The assert fails if the list contents are in reverse order, so reversing to make the assert not fail due to that.
        List<BasicDBObject> reverseMongoHistory = new ArrayList<>();
        reverseMongoHistory.add(fakeMongoHistory.get(1));
        reverseMongoHistory.add(fakeMongoHistory.get(0));
        fakeMongoGrade.put(GRADE_HISTORY, reverseMongoHistory);
        Assert.assertEquals(fakeMongoGrade, testGrade);
    }

    @Test
    public void buildProtoGradeHistoryTest() throws Exception {
        GradeHistory testHistory = GradeManager.buildProtoGradeHistory(fakeMongoHistory.get(0));
        Assert.assertEquals(fakeProtoHistory1.build(), testHistory);
    }

    @Test(expected = DatabaseAccessException.class)
    public void buildProtoGradeHistoryNoValuesTest() throws Exception {
        GradeHistory testHistory = GradeManager.buildProtoGradeHistory(new BasicDBObject());
    }

    @Test
    public void buildProtoGradeTest() throws Exception {
        ProtoGrade testGrade = GradeManager.buildProtoGrade(fakeMongoGrade);
        Assert.assertEquals(fakeProtoGrade.build(), testGrade);
    }

    @Test(expected = DatabaseAccessException.class)
    public void buildProtoGradeNoCourseIdTest() throws Exception {
        fakeMongoGrade.remove(COURSE_ID);
        ProtoGrade testGrade = GradeManager.buildProtoGrade(fakeMongoGrade);
    }

    @Test(expected = DatabaseAccessException.class)
    public void buildProtoGradeNoUserIdTest() throws Exception {
        fakeMongoGrade.remove(USER_ID);
        ProtoGrade testGrade = GradeManager.buildProtoGrade(fakeMongoGrade);
    }

    @Test
    public void getProbGradeTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        protoGradeGrabber.setCourseId(courseId);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_USER_ID, null, Authentication.AuthResponse.PermissionLevel.STUDENT);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());

        // Tests that both the user of the grade and a course admin can get a valid problem grade.
        ProtoGrade testUserGrade = GradeManager.getGrade(authenticator, db, FAKE_USER_ID,
                FAKE_USER_ID, protoGradeGrabber.build());
        ProtoGrade testAdminGrade = GradeManager.getGrade(authenticator, db, FAKE_ADMIN_ID, FAKE_USER_ID,
                protoGradeGrabber.build());

        Assert.assertEquals(fakeProtoGrade.build(), testUserGrade);
        Assert.assertEquals(fakeProtoGrade.build(), testAdminGrade);
    }

    @Test
    public void getCourseGradeTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.clearAssignmentId();
        fakeProtoGrade.clearProblemId();

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId,
                FAKE_USER_ID, null, Authentication.AuthResponse.PermissionLevel.STUDENT);


        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        protoGradeGrabber.clearAssignmentId().clearProblemId();
        protoGradeGrabber.setCourseId(courseId);



        // Tests that both the user of the grade and a course admin can get a valid course grade.
        ProtoGrade testUserGrade = GradeManager.getGrade(authenticator, db, FAKE_USER_ID, FAKE_USER_ID,
                protoGradeGrabber.build());
        ProtoGrade testAdminGrade = GradeManager.getGrade(authenticator, db, FAKE_ADMIN_ID, FAKE_USER_ID,
                protoGradeGrabber.build());
        Assert.assertEquals(fakeProtoGrade.build(), testUserGrade);
        Assert.assertEquals(fakeProtoGrade.build(), testAdminGrade);
    }

    @Test
    public void nonAdminRequestingDifferentUserGradeTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        protoGradeGrabber.setCourseId(courseId);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_USER_ID, null, Authentication.AuthResponse.PermissionLevel.STUDENT);

        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());

        exception.expect(AuthenticationException.class);

        // User should be able to get their grade still. FAKE_ADMIN cannot because they are not on the adminList.
        ProtoGrade testUserGrade = GradeManager.getGrade(authenticator, db, FAKE_USER_ID, FAKE_USER_ID,
                protoGradeGrabber.build());

        Assert.assertEquals(fakeProtoGrade.build(), testUserGrade);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.PEER_TEACHER);

        ProtoGrade testAdminGrade = GradeManager.getGrade(authenticator, db, FAKE_ADMIN_ID, FAKE_ADMIN_ID,
                protoGradeGrabber.build());
    }

    @Test(expected = DatabaseAccessException.class)
    public void gradeNotInDatabaseTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        fakeProtoGrade.setCourseId(courseId);
        protoGradeGrabber.setCourseId(courseId);
        // Not adding grade here like previous tests so we can get the exception!

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, FAKE_PROB_ID,
                FAKE_USER_ID, null, Authentication.AuthResponse.PermissionLevel.STUDENT);

        ProtoGrade testUserGrade = GradeManager.getGrade(authenticator, db, FAKE_USER_ID, FAKE_USER_ID,
                protoGradeGrabber.build());
    }

    @Test
    public void getAllAssignmentGradesInstructorTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.ASSIGNMENT, FAKE_ASGN_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        List<ProtoGrade> expectedGrades = new ArrayList<>();
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.setUserId(FAKE_USER_ID + "1");
        fakeProtoGrade.clearProblemId();
        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        expectedGrades.add(fakeProtoGrade.build());

        fakeProtoGrade.setUserId(FAKE_USER_ID + "2");
        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        expectedGrades.add(fakeProtoGrade.build());

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        List<ProtoGrade> testGrades = GradeManager.getAllAssignmentGradesInstructor(authenticator, db, courseId, FAKE_ADMIN_ID);
        Assert.assertEquals(expectedGrades, testGrades);
    }

    @Test
    public void getAllAssignmentGradesInstructorThrowsWhenNoCourseAuthEvenThoughTheyAddedAssignment() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.ASSIGNMENT, FAKE_ASGN_ID,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        List<ProtoGrade> expectedGrades = new ArrayList<>();
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.setUserId(FAKE_USER_ID + "1");
        fakeProtoGrade.clearProblemId();
        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        expectedGrades.add(fakeProtoGrade.build());

        fakeProtoGrade.setUserId(FAKE_USER_ID + "2");
        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        expectedGrades.add(fakeProtoGrade.build());

        exception.expect(AuthenticationException.class);

        List<ProtoGrade> testGrades = GradeManager.getAllAssignmentGradesInstructor(authenticator, db, courseId, FAKE_ADMIN_ID);
        Assert.assertEquals(expectedGrades, testGrades);
    }

    @Test(expected = AuthenticationException.class)
    public void getAllAssignmentGradesInstructorNotAuthorizedTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());

        List<ProtoGrade> testGrades = GradeManager.getAllAssignmentGradesInstructor(authenticator, db, courseId, FAKE_USER_ID);
    }

    @Test(expected = DatabaseAccessException.class)
    public void getAllAssignmentGradesInstructorNoGradesTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        List<ProtoGrade> testGrades = GradeManager.getAllAssignmentGradesInstructor(authenticator, db, courseId, FAKE_ADMIN_ID);
    }

    @Test
    public void getAllAssignmentGradesStudentTest() throws Exception {

        // for all assignments teacher permission
        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.ASSIGNMENT, null,
                FAKE_ADMIN_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        List<ProtoGrade> expectedGrades = new ArrayList<>();
        fakeProtoGrade.setCourseId(courseId);
        fakeProtoGrade.setAssignmentId(FAKE_ASGN_ID + "1");
        fakeProtoGrade.clearProblemId();
        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        expectedGrades.add(fakeProtoGrade.build());

        fakeProtoGrade.setAssignmentId(FAKE_ASGN_ID + "2");


        GradeManager.addGrade(authenticator, db, FAKE_ADMIN_ID, fakeProtoGrade.build());
        expectedGrades.add(fakeProtoGrade.build());

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId,
                FAKE_USER_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        List<ProtoGrade> testGrades = GradeManager.getAllAssignmentGradesStudent(authenticator, db, courseId, FAKE_USER_ID, FAKE_USER_ID);
        Assert.assertEquals(expectedGrades, testGrades);
    }

    @Test(expected = AuthenticationException.class)
    public void getAllAssignmentGradesStudentNotInCourseTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());
        List<ProtoGrade> testGrades = GradeManager.getAllAssignmentGradesStudent(authenticator, db, courseId, FAKE_USER_ID, FAKE_USER_ID);
    }

    @Test(expected = DatabaseAccessException.class)
    public void getAllAssignmentGradesStudentNoGradesTest() throws Exception {
        String courseId = CourseManager.mongoInsertCourse(db, courseBuilder.build());

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId,
                FAKE_USER_ID, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        List<ProtoGrade> testGrades = GradeManager.getAllAssignmentGradesStudent(authenticator, db, courseId, FAKE_USER_ID, FAKE_USER_ID);
    }
}
