/**
 * Creates a database with a specific name, and has a callback after being
 * opened.
 *
 * It will also create all the functions needed for the specific database.
 * @class ProtoDatabase
 */
function ProtoDatabase(databaseName, version, openCallback) {
    CourseSketch = CourseSketch || {};
    // Right now this is permanently set to true.
    // This is because caching is causing lots of problems with developing
    // when the website is stable we can start to optimize and turn off caching.
    CourseSketch.noCache = true;
    var databaseSupported = true;
    if (!window.indexedDB || typeof window.indexedDB === 'undefined') {
        databaseSupported = false;
        console.log('Your browser does not support a stable version of IndexedDB. So storing your data will not be possible');
    }
    var localScope = this;
    var dbNameSpace = {};
    if (databaseSupported) {
        dbNameSpace.indexedDB = window.indexedDB;
    } else {
        dbNameSpace.indexedDB = undefined;
    }

    var upgradeTables = null;
    this.setTables = function(tables) {
        upgradeTables = tables;
    };

    /**
     * Returns an object that can be converted to a table.
     *
     * The table contains an addingFunction (other) and a generic callback.<br>
     * example addingFunction<br>
     * <code>
     * function adding(store, todoText) {
     *         return store.put({
     *             'text': todoText,
     *             'timeStamp' : new Date().getTime()
     *         });
     * }
     * </code>
     *
     * @param {String} tableName
     *            The name of the specific table to be created.
     * @param {String} keyValue
     *            This is the key for that specific table
     * @param {Function} addingFunction
     *            Takes in a store and then creates and returns a request see
     *            sample above.
     */
    this.createTable = function(tableName, keyValue, addingFunction) {
        return {
            name: tableName,
            key: keyValue,
            add: addingFunction
        };
    };

    this.open = function() {
        var tableCreationCalled = false;
        try {
            // Lets do browser checking for compatability.
            var request = indexedDB.open(databaseName, version);

            // We can only create Object stores in a version change transaction.
            request.onupgradeneeded = function(e) {
                var db = e.target.result;
                // A versionchange transaction is started automatically.
                e.target.transaction.onerror = dbNameSpace.indexedDB.onerror;
                for (var i = 0; i < upgradeTables.length; i++) {
                    table = upgradeTables[i];
                    // delete existing table
                    if (db.objectStoreNames.contains(table.name)) {
                        db.deleteObjectStore(table.name);
                    }
                    var store = db.createObjectStore(table.name, { keyPath: table.key });
                }
            };
            request.onsuccess = function(e) {
                console.log('Database has opened');
                dbNameSpace.indexedDB.db = e.target.result;
                if (!tableCreationCalled) {
                    tableCreationCalled = true;
                    createTableFunctions();
                }
            };
            request.onerror = function(e) {
                console.log(e);
                console.log('Exception has occured when getting data');
                // if there is an exception then we should continue
                dbNameSpace.indexedDB = null;
                if (!tableCreationCalled) {
                    tableCreationCalled = true;
                    createTableFunctions();
                }
            };
        } catch (exception) {
            console.error(exception);
            // if there is an exception then we should continue
            dbNameSpace.indexedDB = null;
            if (!tableCreationCalled) {
                tableCreationCalled = true;
                createTableFunctions();
            }
        }
    };

    /**
     * creates a bunch of functions for the table which are created upon
     * successful database creation.
     */
    function createTableFunctions() {
        if (upgradeTables === null) {
            if (openCallback) {
                openCallback();
            }
            return;
        }
        for (var i = 0; i < upgradeTables.length; i++) {
            table = upgradeTables[i];
            (function(localTable) {
                var dataMap = {};
                /**
                 * Creates a function for adding items to the database.
                 *
                 * @param {String} objectId the Key of the object when added to the database.
                 * @param {String} objectToAdd A string representing the object in the database.
                 * @param callback Called when the object is successfully added to the database.
                 */
                localScope[ 'putIn' + localTable.name ] = function(objectId, objectToAdd, callback) {
                    if (!databaseSupported || !dbNameSpace.indexedDB || !dbNameSpace.indexedDB.db || CourseSketch.noCache) {
                        dataMap[objectId] = objectToAdd;
                        if (!isUndefined(callback)) {
                            callback({}, {});
                        }
                        return;
                    }

                    var db = dbNameSpace.indexedDB.db;
                    var trans = db.transaction([ localTable.name ], 'readwrite');
                    var store = trans.objectStore(localTable.name);
                    var request = localTable.add(store, objectId, objectToAdd);
                    trans.oncomplete = function(e) {
                        if (!isUndefined(callback)) {
                            callback(e, request);
                        }
                    };

                    request.onerror = function(e) {
                        console.log(e.value);
                    };
                };

                /**
                 * Creates a function for deleting items from the database.
                 *
                 * @param {String} objectId The id of the object we are trying to delete from the database.
                 * @param {Function} callback The function that is called after deleting the item.
                 */
                localScope[ 'deleteFrom' + localTable.name ] = function(objectId, callback) {
                    if (!databaseSupported || !dbNameSpace.indexedDB || !dbNameSpace.indexedDB.db || CourseSketch.noCache) {
                        dataMap[objectId] = undefined;
                        callback(undefined, undefined);
                        return;
                    }

                    var db = dbNameSpace.indexedDB.db;
                    var trans = db.transaction([ localTable.name ], 'readwrite');
                    var store = trans.objectStore(localTable.name);
                    var request = store.delete(objectId);
                    trans.oncomplete = function(e) {
                        if (!isUndefined(callback)) {
                            callback(e, request);
                        }
                    };

                    request.onerror = function(e) {
                        console.log(e.value);
                    };
                };

                /**
                 * Creates a function for deleting items from the database.
                 *
                 * @param {String} objectId The id of the object we are trying to get from the database.
                 * @param {Function} callback The function that is called after retrieving the item.
                 */
                localScope[ 'getFrom' + localTable.name ] = function(objectId, callback) {
                    if (!databaseSupported || !dbNameSpace.indexedDB || !dbNameSpace.indexedDB.db || CourseSketch.noCache) {
                        var request = {
                            result: {
                                id: objectId,
                                data: dataMap[objectId]
                            }
                        };
                        callback(undefined, request, request.result);
                        return;
                    }

                    var db = dbNameSpace.indexedDB.db;
                    var trans = db.transaction([ localTable.name ]);
                    var store = trans.objectStore(localTable.name);
                    var request = store.get(objectId);
                    request.onsuccess = function(e) {
                        if (callback) {
                            callback(e, request, request.result);
                        }
                    };

                    request.onerror = function(e) {
                        console.log(e.value);
                    };
                };
            })(table);
        }
        if (openCallback) {
            openCallback();
        }
    }

    this.emptySelf = function() {
        emptyDB(databaseName);
    };

    function emptyDB(databaseName) {
        try {
            var result = confirm('Do you want to empty all of the local data?');
            if (result === true) {
                var dbreq = dbNameSpace.indexedDB.deleteDatabase(databaseName);
                dbreq.onsuccess = function(event) {
                    output_trace('indexedDB: ' + databaseName + ' deleted');
                };
                dbreq.onerror = function(event) {
                    output_trace('indexedDB.delete Error: ' + event.message);
                };
            } else {
                alert('The local data was not emptied');
            }
        } catch (e) {
            output_trace('Error: ' + e.message);
        }
    }
}
