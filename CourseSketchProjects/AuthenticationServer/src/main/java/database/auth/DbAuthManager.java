package database.auth;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import database.DatabaseAccessException;
import database.DatabaseStringConstants;
import org.bson.types.ObjectId;
import protobuf.srl.school.School;
import protobuf.srl.services.authentication.Authentication;

import java.util.List;

import static database.DbSchoolUtility.getCollectionFromType;
import static database.DbSchoolUtility.getParentItemType;

/**
 * Created by dtracers on 10/7/2015.
 */
public class DbAuthManager {

    /**
     * The database that the auth checker grabs data from.
     */
    private final DB database;

    public DbAuthManager(final DB database) {
        this.database = database;
    }

    public void insertNewItem(String authId, String itemId, School.ItemType itemType, String parentId, DbAuthChecker authChecker)
            throws DatabaseAccessException, AuthenticationException {
        // Alternative: getParentItemType(itemType) != itemType
        if (!getParentItemType(itemType).equals(itemType)) {
            final Authentication.AuthResponse response = authChecker.isAuthenticated(getParentItemType(itemType), parentId, authId,
                    Authentication.AuthType.newBuilder().setCheckingAdmin(true).build());
            final AuthenticationResponder responder = new AuthenticationResponder(response);
            if (!responder.hasModeratorPermission()) {
                throw new AuthenticationException("User does not have permission to insert new items for id: " + parentId,
                        AuthenticationException.INVALID_PERMISSION);
            }
        }

        final BasicDBObject insertQuery = createInsertQuery(itemId, itemType, authId);
        if (!getParentItemType(itemType).equals(itemType)) {
            copyParentDetails(insertQuery, itemId, itemType, parentId);
        }
        final DBCollection collection = database.getCollection(getCollectionFromType(itemType));
        collection.insert(insertQuery);
    }

    private void copyParentDetails(final BasicDBObject insertQuery, final String itemId, final School.ItemType itemType, final String parentId)
            throws DatabaseAccessException {
        final School.ItemType collectionType = getParentItemType(itemType);
        final DBCollection collection = database.getCollection(getCollectionFromType(collectionType));
        final DBObject result = collection.findOne(new ObjectId(itemId),
                new BasicDBObject(DatabaseStringConstants.USER_LIST, true)
                        .append(DatabaseStringConstants.COURSE_ID, true)
                        .append(DatabaseStringConstants.OWNER_ID, true));
        if (result == null) {
            throw new DatabaseAccessException("The item with the id " + itemId + " Was not found in the database");
        }
        insertQuery.putAll(result);
    }

    private BasicDBObject createInsertQuery(final String itemId, final School.ItemType itemType, final String authId) {
        final BasicDBObject query = new BasicDBObject(DatabaseStringConstants.SELF_ID, itemId);
        if (School.ItemType.COURSE.equals(itemType)) {
            query.append(DatabaseStringConstants.COURSE_ID, itemId)
                    .append(DatabaseStringConstants.OWNER_ID, authId);
        }
        if (School.ItemType.BANK_PROBLEM.equals(itemType)) {
            query.append(DatabaseStringConstants.PROBLEM_BANK_ID, itemId)
                    .append(DatabaseStringConstants.OWNER_ID, authId);
        }
        return query;
    }
}