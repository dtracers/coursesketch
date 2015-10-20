package coursesketch.database;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import connection.LoginServerWebSocketHandler;
import coursesketch.database.auth.AuthenticationException;
import coursesketch.server.authentication.HashManager;
import coursesketch.server.interfaces.AbstractServerWebSocketHandler;
import database.DatabaseStringConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utilities.LoggingConstants;

import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import static database.DatabaseStringConstants.EMAIL;
import static database.DatabaseStringConstants.INSTRUCTOR_CLIENT_ID;
import static database.DatabaseStringConstants.INSTRUCTOR_ID;
import static database.DatabaseStringConstants.IS_DEFAULT_INSTRUCTOR;
import static database.DatabaseStringConstants.LOGIN_COLLECTION;
import static database.DatabaseStringConstants.LOGIN_DATABASE;
import static database.DatabaseStringConstants.PASSWORD;
import static database.DatabaseStringConstants.STUDENT_CLIENT_ID;
import static database.DatabaseStringConstants.STUDENT_ID;
import static database.DatabaseStringConstants.USER_NAME;

/**
 * A client for the login database.
 */
public class DatabaseClient {

    /**
     * Declaration and Definition of Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(DatabaseClient.class);

    /**
     * A single instance of the database client.
     */
    @SuppressWarnings("PMD.AssignmentToNonFinalStatic")
    private static volatile DatabaseClient instance;

    /**
     * a private database.
     */
    @SuppressWarnings("PMD.UnusedPrivateField")
    private DB database = null;

    /**
     * @param url
     *            the location at which the database is created.
     */
    private DatabaseClient(final String url) {
        MongoClient mongoClient = null;
        try {
            mongoClient = new MongoClient(url);
        } catch (UnknownHostException e) {
            LOG.error(LoggingConstants.EXCEPTION_MESSAGE, e);
        }
        if (mongoClient == null) {
            return;
        }
        database = mongoClient.getDB(LOGIN_DATABASE);
    }

    /**
     * Creates the database at a specific url.
     */
    private DatabaseClient() {
        this("localhost");
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
    public DatabaseClient(final boolean testOnly, final DB fakeDB) {
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
                database = mongoClient.getDB(LOGIN_DATABASE);
            }
        }
        instance = this;
    }

    /**
     * @return An instance of the mongo client. Creates it if it does not exist.
     */
    @SuppressWarnings("checkstyle:innerassignment")
    public static DatabaseClient getInstance() {
        DatabaseClient result = instance;
        if (result == null) {
            synchronized (DatabaseClient.class) {
                if (result == null) {
                    result = instance;
                    instance = result = new DatabaseClient();
                }
            }
        }
        return result;
    }

    /**
     * Logs in the user. Attempts to log in as the default account if that
     * option is specified otherwise it will login as the type that is
     * specified.
     *
     * @param user
     *            the user name that is attempting to login.
     * @param password
     *            the password of the user that is attempting to log in.
     * @param loginAsDefault
     *            true if the system will log in as the default account.
     * @param loginAsInstructor
     *            true if the system will log in as the instructor (not used if
     *            loginAsDefault is true).
     * @return The server side userid : the client side user id.
     * @throws LoginException
     *             thrown if there is a problem loggin in.
     */
    public static final String mongoIdentify(final String user, final String password, final boolean loginAsDefault, final boolean loginAsInstructor)
            throws LoginException {
        final DBCollection table = getInstance().database.getCollection(LOGIN_COLLECTION);
        final BasicDBObject query = new BasicDBObject(USER_NAME, user);

        final DBObject cursor = table.findOne(query);

        if (cursor == null) {
            throw new LoginException(LoginServerWebSocketHandler.INCORRECT_LOGIN_MESSAGE);
        }
        try {
            final String hash = cursor.get(PASSWORD).toString();
            if (HashManager.validateHash(password, hash)) {
                return getUserInfo(cursor, loginAsDefault, loginAsInstructor);
            } else {
                if (!HashManager.CURRENT_HASH.equals(HashManager.getAlgorithmFromHash(hash))) {
                    final String newHash = HashManager.upgradeHash(password, hash);
                    if (newHash != null) {
                        updatePassword(table, cursor, password);
                        return getUserInfo(cursor, loginAsDefault, loginAsInstructor);
                    } else {
                        throw new LoginException(LoginServerWebSocketHandler.INCORRECT_LOGIN_MESSAGE);
                    }
                } else {
                    throw new LoginException(LoginServerWebSocketHandler.INCORRECT_LOGIN_MESSAGE);
                }
            }
        } catch (GeneralSecurityException e) {
            LOG.error(LoggingConstants.EXCEPTION_MESSAGE, e);
            throw new LoginException("An error occured while comparing passwords", e);
        }
    }

    /**
     * Updates the password to the new value.
     *
     * This method is private on purpose please leave it that way.
     * @param table The collection that the password is being updated in.
     * @param query The user that the password is being updated for.
     * @param newPassword The new password.
     * @throws AuthenticationException Thrown if an invalid key is set
     * @throws NoSuchAlgorithmException Thrown if the specified algorithm does not exist.
     */
    private static void updatePassword(final DBCollection table, final DBObject query, final String newPassword)
            throws AuthenticationException, NoSuchAlgorithmException {
        final String newHash = HashManager.createHash(newPassword);
        table.update(query, new BasicDBObject(DatabaseStringConstants.SET_COMMAND, new BasicDBObject(DatabaseStringConstants.PASSWORD, newHash)));
    }

    /**
     * Gets the user information. This assumes that the user was able to log in
     * correctly.
     *
     * @param cursor
     *            a pointer to the database object.
     * @param loginAsDefault
     *            true if the system will log in as the default account.
     * @param loginAsInstructor
     *            true if the system will log in as the instructor (not used if
     *            loginAsDefault is true).
     * @return A string representing the user id.
     * @throws LoginException
     *             Thrown if the user ids are not able to be grabbed.
     */
    @SuppressWarnings("PMD.UselessParentheses")
    private static String getUserInfo(final DBObject cursor, final boolean loginAsDefault, final boolean loginAsInstructor) throws LoginException {
        String result;
        final boolean defaultAccountIsInstructor = (Boolean) cursor.get(IS_DEFAULT_INSTRUCTOR);
        if ((loginAsDefault && defaultAccountIsInstructor) || (!loginAsDefault && loginAsInstructor)) {
            result = cursor.get(INSTRUCTOR_ID) + ":" + cursor.get(INSTRUCTOR_CLIENT_ID);
        } else if ((loginAsDefault && !defaultAccountIsInstructor) || (!loginAsDefault && !loginAsInstructor)) {
            result = cursor.get(STUDENT_ID) + ":" + cursor.get(STUDENT_CLIENT_ID);
        } else {
            throw new LoginException(LoginServerWebSocketHandler.PERMISSION_ERROR_MESSAGE);
        }
        return result;
    }

    /**
     * Adds a new user to the database.
     *
     * @param user
     *            The user name to be added.
     * @param password
     *            the password of the user to be added to the DB.
     * @param email
     *            The email of the user.
     * @param isInstructor
     *            If the default account is an instructor
     * @throws GeneralSecurityException
     *             Thrown if there are problems creating the hash for the
     *             password.
     * @throws RegistrationException
     *             Thrown if the user already exist in the system.
     */
    public static final void createUser(final String user, final String password, final String email, final boolean isInstructor)
            throws GeneralSecurityException, RegistrationException {
        final DBCollection loginCollection = getInstance().database.getCollection(LOGIN_COLLECTION);
        BasicDBObject query = new BasicDBObject(USER_NAME, user);
        final DBObject cursor = loginCollection.findOne(query);
        if (cursor == null) {
            query = new BasicDBObject(USER_NAME, user).append(PASSWORD, HashManager.createHash(password)).append(EMAIL, email)
                    .append(IS_DEFAULT_INSTRUCTOR, isInstructor).append(INSTRUCTOR_ID, FancyEncoder.fancyID())
                    .append(STUDENT_ID, FancyEncoder.fancyID()).append(STUDENT_CLIENT_ID, AbstractServerWebSocketHandler.Encoder.nextID().toString())
                    .append(INSTRUCTOR_CLIENT_ID, AbstractServerWebSocketHandler.Encoder.nextID().toString());
            loginCollection.insert(query);
        } else {
            throw new RegistrationException(LoginServerWebSocketHandler.REGISTRATION_ERROR_MESSAGE);
        }
    }

    /**
     * @param user
     *            the username of the account that is being checked.
     * @return true if the default account for the user is an instructor
     *         account.
     */
    public static final boolean defaultIsInstructor(final String user) {
        final DBCollection table = getInstance().database.getCollection(LOGIN_COLLECTION);
        final BasicDBObject query = new BasicDBObject(USER_NAME, user);

        final DBObject cursor = table.findOne(query);
        if (cursor == null) {
            LOG.info("Unable to find user!");
            return false;
        }
        return (Boolean) cursor.get(IS_DEFAULT_INSTRUCTOR);
    }

}