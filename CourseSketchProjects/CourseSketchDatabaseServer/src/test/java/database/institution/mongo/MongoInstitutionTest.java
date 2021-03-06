package database.institution.mongo;

import com.coursesketch.test.utilities.AuthenticationHelper;
import com.coursesketch.test.utilities.DatabaseHelper;
import com.coursesketch.test.utilities.ProtobufComparisonBuilder;
import com.github.fakemongo.junit.FongoRule;
import com.google.common.collect.Lists;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import coursesketch.database.auth.AuthenticationChecker;
import coursesketch.database.auth.AuthenticationDataCreator;
import coursesketch.database.auth.AuthenticationException;
import coursesketch.database.auth.AuthenticationOptionChecker;
import coursesketch.database.auth.AuthenticationUpdater;
import coursesketch.database.auth.Authenticator;
import database.DatabaseAccessException;
import database.DatabaseStringConstants;
import database.DbSchoolUtility;
import database.user.UserClient;
import org.bson.types.ObjectId;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import protobuf.srl.commands.Commands;
import protobuf.srl.school.School;
import protobuf.srl.services.authentication.Authentication;
import protobuf.srl.utils.Util;

import static database.DatabaseStringConstants.BASE_SKETCH;
import static database.DatabaseStringConstants.COURSE_TOPIC;
import static database.DatabaseStringConstants.PROBLEM_BANK_COLLECTION;
import static database.DatabaseStringConstants.QUESTION_TEXT;
import static database.DatabaseStringConstants.QUESTION_TYPE;
import static database.DatabaseStringConstants.REGISTRATION_KEY;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by dtracers on 10/26/2015.
 */
@RunWith(MockitoJUnitRunner.class)
public class MongoInstitutionTest {

    @Rule
    public FongoRule fongo = new FongoRule();
    @Mock AuthenticationChecker authChecker;
    @Mock AuthenticationOptionChecker optionChecker;
    @Mock AuthenticationDataCreator dataCreator;
    @Mock AuthenticationUpdater authenticationUpdater;

    public DB db;
    public Authenticator authenticator;
    public MongoInstitution institution;

    public static final String VALID_REGISTRATION_KEY = "VALID KEY!";
    public static final String VALID_NAME = "Valid course name!";
    public static final String FAKE_DESCRIPTION = "DESCRIPTIONS YAY";
    public static final String FAKE_ID = "507f1f77bcf86cd799439011";
    public static final String FAKE_QUESTION_TEXT = "Question Texts";
    public static final String FAKE_SCRIPT = "fake script";
    public static final String ADMIN_USER = "adminUser";
    public static final String USER_USER = "userUser";

    public static final long FAKE_VALID_DATE = 1000;
    public static final Util.DateTime FAKE_VALID_DATE_OBJECT = Util.DateTime.newBuilder().setMillisecond(FAKE_VALID_DATE).build();
    public static final long FAKE_INVALID_DATE = 1001;

    public static final Commands.SrlUpdateList.Builder FAKE_UPDATELIST = Commands.SrlUpdateList.newBuilder();
    public static final Util.QuestionType FAKE_QUESTION_TYPE = Util.QuestionType.FREE_RESP;
    public static final School.SrlAssignment.AssignmentType VALID_ASSIGNMENT_TYPE = School.SrlAssignment.AssignmentType.EXAM;
    public static final int VALID_ASSIGNMENT_TYPE_VALUE = School.SrlAssignment.AssignmentType.EXAM_VALUE;

    private String courseId;
    private String assignmentId;
    private String courseProblemId;
    private String bankProblemId;

    private School.SrlCourse.Builder defaultCourse;
    private School.SrlAssignment.Builder defaultAssignment;
    private School.SrlProblem.Builder defaultProblem;
    private School.SrlBankProblem.Builder bankProblem;

    @Before
    public void before() {
        db = fongo.getDB();

        // used to make the user client use the mock database
        new UserClient(true, db);

        try {
            // general results
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
        institution = new MongoInstitution(true, db, authenticator, authenticationUpdater);

        defaultCourse = School.SrlCourse.newBuilder();
        defaultCourse.setId(FAKE_ID);
        defaultCourse.setAccess(School.SrlCourse.Accessibility.PRIVATE);
        defaultCourse.setDescription(FAKE_DESCRIPTION);
        defaultCourse.setAccessDate(FAKE_VALID_DATE_OBJECT);
        defaultCourse.setCloseDate(FAKE_VALID_DATE_OBJECT);
        defaultCourse.setName(VALID_NAME);

        defaultAssignment = School.SrlAssignment.newBuilder();
        defaultAssignment.setId(FAKE_ID);

        defaultProblem = School.SrlProblem.newBuilder();
        defaultProblem.setId(FAKE_ID);

        courseId = null;
        assignmentId = null;
        courseProblemId = null;
        bankProblemId = null;
    }

    public void insertCourse() throws DatabaseAccessException, AuthenticationException {

        // creating the course
        final School.SrlCourse.Builder course = School.SrlCourse.newBuilder();
        course.setId("ID");
        courseId = CourseManager.mongoInsertCourse(db, course.build());
        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId, ADMIN_USER,
                null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        // creating assignment
        defaultAssignment.setCourseId(courseId);

        dataCreator = AuthenticationHelper.setMockDate(optionChecker, dataCreator, School.ItemType.COURSE, courseId, FAKE_VALID_DATE, true);
    }

    public void insertCourseAndAssignment() throws DatabaseAccessException, AuthenticationException {

        // creating bank problem
        bankProblem = School.SrlBankProblem.newBuilder();
        bankProblem.setId("NOT REAL ID");
        bankProblem.setQuestionText(FAKE_QUESTION_TEXT);

        bankProblemId = institution.insertBankProblem(ADMIN_USER, bankProblem.build());
        bankProblem.setId(bankProblemId);

        // creating the course
        courseId = institution.insertCourse(ADMIN_USER, defaultCourse.build());
        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE, courseId, ADMIN_USER,
                null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        // creating assignment
        defaultAssignment.setCourseId(courseId);

        assignmentId = institution.insertAssignment(ADMIN_USER, defaultAssignment.build());
        updateProblemIds(courseId, assignmentId, bankProblemId);

        // sets the course able to use the bank problem
        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.BANK_PROBLEM, bankProblemId, courseId,
                null, Authentication.AuthResponse.PermissionLevel.STUDENT);

        dataCreator = AuthenticationHelper.setMockDate(optionChecker, dataCreator, School.ItemType.ASSIGNMENT, assignmentId, FAKE_VALID_DATE, true);
        dataCreator = AuthenticationHelper.setMockDate(optionChecker, dataCreator, School.ItemType.COURSE, courseId, FAKE_VALID_DATE, true);
    }

    public void updateProblemIds(String courseId, String assignmentId, String bankProblemId) {
        defaultProblem.setCourseId(courseId);
        defaultProblem.setAssignmentId(assignmentId);
        defaultProblem.setProblemBankId(bankProblemId);
    }

    @Test
    public void insertingBankProblemCreatesRegistrationKey() throws AuthenticationException, InvalidProtocolBufferException {

        School.SrlBankProblem.Builder bankProblem = School.SrlBankProblem.newBuilder();
        bankProblem.setId("NOT REAL ID");
        bankProblem.setRegistrationKey(VALID_REGISTRATION_KEY);
        bankProblem.setQuestionText(FAKE_QUESTION_TEXT);
        bankProblem.setCourseTopic(FAKE_QUESTION_TEXT);
        bankProblem.setQuestionType(FAKE_QUESTION_TYPE);
        bankProblem.setBaseSketch(FAKE_UPDATELIST.build());

        String problemBankId = institution.insertBankProblem(ADMIN_USER, bankProblem.build());

        final DBRef myDbRef = new DBRef(db, PROBLEM_BANK_COLLECTION, new ObjectId(problemBankId));
        final DBObject mongoBankProblem = myDbRef.fetch();

        Assert.assertTrue(mongoBankProblem.containsField(REGISTRATION_KEY));
        Assert.assertEquals(mongoBankProblem.get(QUESTION_TEXT), FAKE_QUESTION_TEXT);
        Assert.assertEquals(mongoBankProblem.get(COURSE_TOPIC), FAKE_QUESTION_TEXT);
        Assert.assertEquals(mongoBankProblem.get(QUESTION_TYPE), FAKE_QUESTION_TYPE.getNumber());
        Assert.assertEquals(Commands.SrlUpdateList.parseFrom((byte[]) mongoBankProblem.get(BASE_SKETCH)),
                FAKE_UPDATELIST.build());

        String registrationKey = (String) mongoBankProblem.get(REGISTRATION_KEY);

        verify(authenticationUpdater, atLeastOnce()).createNewItem(eq(School.ItemType.BANK_PROBLEM), eq(problemBankId), (String)isNull(), eq(ADMIN_USER),
                eq(registrationKey));
    }

    @Test
    public void insertingCourseCreatesRegistrationKey() throws AuthenticationException, InvalidProtocolBufferException, DatabaseAccessException {

        defaultCourse.setAccess(School.SrlCourse.Accessibility.PRIVATE);
        defaultCourse.setDescription(FAKE_DESCRIPTION);
        defaultCourse.setAccessDate(FAKE_VALID_DATE_OBJECT);
        defaultCourse.setCloseDate(FAKE_VALID_DATE_OBJECT);
        defaultCourse.setName(VALID_NAME);

        String courseId = institution.insertCourse(ADMIN_USER, defaultCourse.build());

        final DBRef myDbRef = new DBRef(db, DbSchoolUtility.getCollectionFromType(School.ItemType.COURSE, true), new ObjectId(courseId));
        final DBObject mongoCourse = myDbRef.fetch();

        Assert.assertTrue(mongoCourse.containsField(REGISTRATION_KEY));
        Assert.assertEquals(mongoCourse.get(DatabaseStringConstants.NAME), VALID_NAME);
        Assert.assertEquals(mongoCourse.get(DatabaseStringConstants.DESCRIPTION), FAKE_DESCRIPTION);
        Assert.assertEquals(mongoCourse.get(DatabaseStringConstants.ACCESS_DATE), FAKE_VALID_DATE);
        Assert.assertEquals(mongoCourse.get(DatabaseStringConstants.CLOSE_DATE), FAKE_VALID_DATE);
        Assert.assertEquals(mongoCourse.get(DatabaseStringConstants.COURSE_ACCESS), School.SrlCourse.Accessibility.PRIVATE_VALUE);

        String registrationKey = (String) mongoCourse.get(REGISTRATION_KEY);

        verify(authenticationUpdater, atLeastOnce()).createNewItem(eq(School.ItemType.COURSE), eq(courseId), (String)isNull(), eq(ADMIN_USER),
                eq(registrationKey));
    }


    @Test
    public void insertingAssignmentAlsoCreatesUserGroup() throws Exception {
        insertCourse();

        defaultAssignment.setDescription(FAKE_DESCRIPTION);
        defaultAssignment.setAccessDate(FAKE_VALID_DATE_OBJECT);
        defaultAssignment.setCloseDate(FAKE_VALID_DATE_OBJECT);
        defaultAssignment.setDueDate(FAKE_VALID_DATE_OBJECT);
        defaultAssignment.setName(VALID_NAME);
        defaultAssignment.setAssignmentType(VALID_ASSIGNMENT_TYPE);

        assignmentId = institution.insertAssignment(ADMIN_USER, defaultAssignment.build());

        final DBRef myDbRef = new DBRef(db, DbSchoolUtility.getCollectionFromType(School.ItemType.ASSIGNMENT, true), new ObjectId(assignmentId));
        final DBObject mongoAssignment = myDbRef.fetch();

        Assert.assertEquals(mongoAssignment.get(DatabaseStringConstants.NAME), VALID_NAME);
        Assert.assertEquals(mongoAssignment.get(DatabaseStringConstants.DESCRIPTION), FAKE_DESCRIPTION);
        Assert.assertEquals(mongoAssignment.get(DatabaseStringConstants.ACCESS_DATE), FAKE_VALID_DATE);
        Assert.assertEquals(mongoAssignment.get(DatabaseStringConstants.CLOSE_DATE), FAKE_VALID_DATE);
        Assert.assertEquals(mongoAssignment.get(DatabaseStringConstants.DUE_DATE), FAKE_VALID_DATE);
        Assert.assertEquals(mongoAssignment.get(DatabaseStringConstants.ASSIGNMENT_TYPE), VALID_ASSIGNMENT_TYPE_VALUE);

        verify(authenticationUpdater, atLeastOnce()).createNewItem(eq(School.ItemType.ASSIGNMENT), eq(assignmentId), eq(courseId), eq(ADMIN_USER),
                (String) isNull());
    }

    @Test
    public void insertCourseProblemCreatesUserGroup() throws Exception {
        insertCourseAndAssignment();

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.ASSIGNMENT, assignmentId, ADMIN_USER,
                null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        defaultProblem.setName(VALID_NAME);

        courseProblemId = institution.insertCourseProblem(ADMIN_USER, defaultProblem.build());

        final DBRef myDbRef = new DBRef(db, DbSchoolUtility.getCollectionFromType(School.ItemType.COURSE_PROBLEM, true), new ObjectId(courseProblemId));
        final DBObject mongoProblem = myDbRef.fetch();

        Assert.assertEquals(mongoProblem.get(DatabaseStringConstants.NAME), VALID_NAME);
        Assert.assertEquals(mongoProblem.get(DatabaseStringConstants.PROBLEM_BANK_ID), bankProblemId);

        verify(authenticationUpdater, atLeastOnce()).createNewItem(eq(School.ItemType.COURSE_PROBLEM), eq(courseProblemId), eq(assignmentId),
                eq(ADMIN_USER),
                (String) isNull());
    }

    @Test
    public void insertCourseProblemInsertsBankProblemPermissions() throws Exception {
        insertCourseAndAssignment();

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.ASSIGNMENT, assignmentId, ADMIN_USER,
                null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.BANK_PROBLEM,
                bankProblemId, ADMIN_USER, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        defaultProblem.setName(VALID_NAME);

        courseProblemId = institution.insertCourseProblem(ADMIN_USER, defaultProblem.build());

        final DBRef myDbRef = new DBRef(db, DbSchoolUtility.getCollectionFromType(School.ItemType.COURSE_PROBLEM, true), new ObjectId(courseProblemId));
        final DBObject mongoProblem = myDbRef.fetch();

        Assert.assertEquals(mongoProblem.get(DatabaseStringConstants.NAME), VALID_NAME);
        Assert.assertEquals(mongoProblem.get(DatabaseStringConstants.PROBLEM_BANK_ID), bankProblemId);

        verify(authenticationUpdater, atLeastOnce()).createNewItem(eq(School.ItemType.COURSE_PROBLEM), eq(courseProblemId), eq(assignmentId),
                eq(ADMIN_USER),
                (String) isNull());

        verify(authenticationUpdater, atLeastOnce()).registerUser(eq(School.ItemType.BANK_PROBLEM), eq(bankProblemId), eq(courseId),
                (String) isNotNull());
    }

    @Test
    public void registerCourseInBankProblemWorksWithRgistrationKey() throws Exception {
        institution.putCourseInBankProblem(courseId, bankProblemId, ADMIN_USER, VALID_REGISTRATION_KEY);

        verify(authenticationUpdater, atLeastOnce()).registerUser(eq(School.ItemType.BANK_PROBLEM), eq(bankProblemId), eq(courseId),
                eq(VALID_REGISTRATION_KEY));
    }

    @Test(expected = DatabaseAccessException.class)
    public void registerCourseInBankProblemThrowsExceptionWithInvalidBankId() throws Exception {
        institution.putCourseInBankProblem(courseId, bankProblemId, ADMIN_USER, null);
    }

    @Test
    public void updateCourseProblemAsInstructorWorksWithValidBankId() throws Exception {
        insertCourseAndAssignment();
        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.ASSIGNMENT, assignmentId, ADMIN_USER,
                null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        courseProblemId = institution.insertCourseProblem(ADMIN_USER, defaultProblem.build());
        defaultProblem.setId(courseProblemId);
        defaultProblem.setProblemInfo(bankProblem);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, courseProblemId, ADMIN_USER,
                null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        School.SrlProblem problem = institution.getCourseProblem(Lists.newArrayList(courseProblemId), ADMIN_USER).get(0);
        new ProtobufComparisonBuilder()
                .build().equals(defaultProblem.build(), problem);

        bankProblem = School.SrlBankProblem.newBuilder();
        bankProblem.setId("NOT REAL ID");
        bankProblem.setQuestionText(FAKE_QUESTION_TEXT);

        String newBankProblemId = institution.insertBankProblem(ADMIN_USER, bankProblem.build());

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.BANK_PROBLEM,
                newBankProblemId, ADMIN_USER, null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        School.SrlProblem updatedProblem = School.SrlProblem.newBuilder(defaultProblem.build())
                .setGradeWeight("NEW GRADE WEIGHT")
                .setProblemBankId(newBankProblemId)
                .build();

        institution.updateCourseProblem(ADMIN_USER, updatedProblem);

        verify(authenticationUpdater, atLeastOnce()).registerUser(eq(School.ItemType.BANK_PROBLEM), eq(newBankProblemId), eq(courseId),
                (String) isNotNull());
    }

    @Test(expected = DatabaseAccessException.class)
    public void updateCourseProblemAsInstructorFailsWithInvalidBankId() throws Exception {
        insertCourseAndAssignment();
        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.ASSIGNMENT, assignmentId, ADMIN_USER,
                null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        courseProblemId = institution.insertCourseProblem(ADMIN_USER, defaultProblem.build());
        defaultProblem.setId(courseProblemId);
        defaultProblem.setProblemInfo(bankProblem);

        AuthenticationHelper.setMockPermissions(authChecker, School.ItemType.COURSE_PROBLEM, courseProblemId, ADMIN_USER,
                null, Authentication.AuthResponse.PermissionLevel.TEACHER);

        School.SrlProblem problem = institution.getCourseProblem(Lists.newArrayList(courseProblemId), ADMIN_USER).get(0);
        new ProtobufComparisonBuilder()
                .build().equals(defaultProblem.build(), problem);

        School.SrlProblem updatedProblem = School.SrlProblem.newBuilder(defaultProblem.build())
                .setGradeWeight("NEW GRADE WEIGHT")
                .setProblemBankId(DatabaseHelper.createNonExistentObjectId(bankProblemId))
                .build();

        institution.updateCourseProblem(ADMIN_USER, updatedProblem);
    }

}
