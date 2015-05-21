/**
 * An exception that is used to represent problems with the database.
 *
 * @class DatabaseException
 * @extends BaseException
 */
function DatabaseException(message, request) {
    if (message) {
        this.message = message;
    }
    if (request) {
        this.request = request;
    }
}

DatabaseException.prototype.message = 'Generic database message';
DatabaseException.prototype.request = 'Generic request';
DatabaseException.prototype.name = 'DatabaseException';
/**
 * @returns {String} representing the exception.
 */
DatabaseException.prototype.toString = function() {
    return this.name + ': [' + this.message  + '] for request [' + this.request + ']';
};
