package database.institution.sql;

import database.DatabaseAccessException;
import database.DatabaseStringConstants;
import protobuf.srl.school.School.SrlGrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The state managers handles the students states. States are stored by UserId
 * then type appended by the actual Id
 *
 * @author gigemjt
 */
public final class SqlGradeManager {

    /**
     * Private constructor.
     */
    private SqlGradeManager() {
    }

    /**
     * Returns the state for a given school item.
     *
     * Right now only the completed/started state is applied
     *
     * @param conn
     *         the sql connection. Must point to proper database.
     * @param userId
     *         the id of the user asking for the state.
     * @param classification
     *         if it is a course, assignment, ...
     * @param itemId
     *         the id of the related state (assignmentId, courseId, ...)
     * @return the sate of the assignment.
     * @throws DatabaseAccessException
     *         thrown if connecting to sql database cause an error.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static SrlGrade getGrade(final Connection conn, final String userId, final String classification, final String itemId)
            throws DatabaseAccessException {
        final SrlGrade.Builder grade = SrlGrade.newBuilder();
        final String query = "SELECT * FROM Grades WHERE UserID=? AND SchoolItemType=? AND SchoolItemID=?;";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, userId);
            stmt.setString(2, classification);
            stmt.setString(3, itemId);
            try (final ResultSet rst = stmt.executeQuery()) {
                grade.setId("");
                grade.setProblemId("");
                grade.setGrade(rst.getFloat(DatabaseStringConstants.GRADE));
                grade.setComment(rst.getString(DatabaseStringConstants.COMMENTS));
            } catch (SQLException e) {
                throw new DatabaseAccessException(e, false);
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException(e, false);
        }
        return grade.build();
    }

    /**
     * Creates the state if it does not exist otherwise it updates the old state.
     *
     * @param conn
     *         the database that contains the state. Must point to proper database.
     * @param userId
     *         the id of the user asking for the state.
     * @param classification
     *         if it is a course, assignment, ...
     * @param itemId
     *         the id of the related state (assignmentId, courseId, ...)
     * @param grade
     *         what the grade is being set to.
     * @return result of set: "SET", "INSERT", "ERROR"
     * @throws DatabaseAccessException
     *         thrown if connecting to sql database cause an error.
     */
    @SuppressWarnings("checkstyle:magicnumber")
    public static String setGrade(final Connection conn, final String userId, final String classification, final String itemId, final SrlGrade grade)
            throws DatabaseAccessException {
        String result;
        final String query = "SELECT * FROM Grades WHERE UserID=? AND SchoolItemType=? AND SchoolItemID=?;";
        try (PreparedStatement stmt = conn.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            stmt.setString(1, userId);
            stmt.setString(2, classification);
            stmt.setString(3, itemId);
            try (final ResultSet rst = stmt.executeQuery()) {
                if (rst.next()) {
                    rst.updateFloat(DatabaseStringConstants.GRADE, grade.getGrade());
                    rst.updateString(DatabaseStringConstants.COMMENTS, grade.getComment());
                    rst.updateRow();
                    result = "SET";
                } else {
                    rst.moveToInsertRow();
                    rst.updateString(DatabaseStringConstants.USER_ID, userId);
                    rst.updateString(DatabaseStringConstants.SCHOOLITEMTYPE, classification);
                    rst.updateString(DatabaseStringConstants.SCHOOLITEMID, itemId);
                    rst.updateFloat(DatabaseStringConstants.GRADE, grade.getGrade());
                    rst.updateString(DatabaseStringConstants.COMMENTS, grade.getComment());
                    rst.insertRow();
                    rst.moveToCurrentRow();
                    result = "INSERT";
                }
            } catch (SQLException e) {
                throw new DatabaseAccessException(e, false);
            }
        } catch (SQLException e) {
            throw new DatabaseAccessException(e, false);
        }
        return result;
    }
}
