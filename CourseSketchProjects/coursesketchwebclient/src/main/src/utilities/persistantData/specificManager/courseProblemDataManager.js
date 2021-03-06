/**
 * A manager for assignments that talks with the remote server.
 *
 * @param {CourseSketchDatabase} parent The database that will hold the methods of this instance.
 * @param {AdvanceDataListener} advanceDataListener A listener for the database.
 * @param {IndexedDB} parentDatabase  The local database
 * @param {SrlRequest} Request A shortcut to a request
 * @param {ByteBuffer} ByteBuffer Used in the case of longs for javascript.
 * @constructor
 */
function CourseProblemDataManager(parent, advanceDataListener, parentDatabase, Request, ByteBuffer) {

    /**
     * Sets a courseProblem in local database.
     *
     * @param {SrlCourseProblem} courseProblem
     *                courseproblem object to set
     * @param {Function} courseProblemCallback
     *                function to be called after the courseProblem setting is done
     */
    function setCourseProblem(courseProblem, courseProblemCallback) {
        parentDatabase.putInCourseProblems(courseProblem.id, courseProblem.toBase64(), function(e, request) {
            if (courseProblemCallback) {
                courseProblemCallback(e, request);
            }
        });
    }
    parent.setCourseProblem = setCourseProblem;

    /**
     * Deletes a courseProblem from local database.
     * This does not delete the id pointing to this item in the respective course.
     *
     * @param {String} courseProblemId
     *                ID of the lecture to delete
     * @param {Function} courseProblemCallback
     *                function to be called after the deletion is done
     */
    function deleteCourseProblem(courseProblemId, courseProblemCallback) {
        parentDatabase.deleteFromCourseProblems(courseProblemId, function(e, request) {
            if (courseProblemCallback) {
                courseProblemCallback(e, request);
            }
        });
    }
    parent.deleteCourseProblem = deleteCourseProblem;

    /**
     * Gets a courseProblem from the local database.
     *
     * @param {String} courseProblemId
     *                ID of the courseProblem to get
     * @param {Function} courseProblemCallback
     *                function to be called after getting is complete, parameter
     *                is the courseProblem object, can be called with {@link DatabaseException} if an exception occurred getting the data.
     */
    function getCourseProblemLocal(courseProblemId, courseProblemCallback) {
        if (isUndefined(courseProblemId) || courseProblemId === null) {
            courseProblemCallback(new DatabaseException('The given id is not assigned', 'getting CourseProblem: ' + courseProblemId));
        }
        parentDatabase.getFromCourseProblems(courseProblemId, function(e, request, result) {
            if (isUndefined(result) || isUndefined(result.data)) {
                courseProblemCallback(new DatabaseException('The result is undefined', 'getting CourseProblem: ' + courseProblemId));
            } else {
                // gets the data from the database and calls the callback
                var bytes = ByteBuffer.fromBase64(result.data);
                courseProblemCallback(CourseSketch.prutil.getSrlProblemClass().decode(bytes));
            }
        });
    }
    parent.getCourseProblemLocal = getCourseProblemLocal;

    /**
     * Sets a courseProblem in server database.
     *
     * @param {SrlCourseProblem} courseProblem
     *                CourseProblem object to set.
     * @param {Function} courseProblemCallback
     *                Function to be called after courseProblem setting is done.
     */
    function insertCourseProblemServer(courseProblem, courseProblemCallback) {
        advanceDataListener.sendDataInsert(CourseSketch.prutil.ItemQuery.COURSE_PROBLEM, courseProblem.toArrayBuffer(), function(event, item) {
            if (isException(item)) {
                courseProblemCallback(new DatabaseException('exception thrown while waiting for response from sever',
                    'Inserting course problem ' + courseProblem,  item));
                return;
            }
            var resultArray = item.getReturnText().split(':');
            var oldId = resultArray[1].trim();
            var newId = resultArray[0].trim();
            // we want to get the current course in the local database in case
            // it has changed while the server was processing.
            getCourseProblemLocal(oldId, function(courseProblem2) {
                deleteCourseProblem(oldId);
                if (!isUndefined(courseProblem2) && !(courseProblem2 instanceof DatabaseException)) {
                    courseProblem2.id = newId;
                    setCourseProblem(courseProblem2, function() {
                        courseProblemCallback(courseProblem2);
                    });
                } else {
                    courseProblem.id = newId;
                    setCourseProblem(courseProblem, function() {
                        courseProblemCallback(courseProblem);
                    });
                }
            });
        });
    }

    /**
     * updates a course problem in both local and server databases.
     * Updates an existing course problem into the database. This courseProblem must already
     * exist.
     *
     * @param {SrlCourseProblem} courseProblem
     *                courseProblem object to set
     * @param {Function} localCallback
     *                function to be called after local courseProblem setting is done
     * @param {Function} serverCallback
     *                function to be called after server courseProblem setting is done
     */
    function updateCourseProblem(courseProblem, localCallback, serverCallback) {
        setCourseProblem(courseProblem, function() {
            if (!isUndefined(localCallback)) {
                localCallback();
            }
            advanceDataListener.sendDataUpdate(CourseSketch.prutil.ItemQuery.COURSE_PROBLEM, courseProblem.toArrayBuffer(), function(evt, item) {
                if (isException(item)) {
                    serverCallback(new DatabaseException('exception thrown while waiting for response from sever',
                        'updating course problem ' + courseProblem,  item));
                    return;
                }
                // We do not need to make server changes we just need to make sure it was successful.
                if (!isUndefined(serverCallback)) {
                    serverCallback(item);
                }
            });
        });
    }
    parent.updateCourseProblem = updateCourseProblem;

    /**
     * updates a bankProblem in both local and server databases.
     * Updates an existing bankProblem into the database. This bankProblem must already
     * exist.
     *
     * @param {SrlBankProblem} bankProblem
     *                bankProblem object to set
     * @param {Function} localCallback
     *                function to be called after local bankProblem setting is done
     * @param {Function} serverCallback
     *                function to be called after server bankProblem setting is done
     */
    function updateBankProblem(bankProblem, localCallback, serverCallback) {
        if (!isUndefined(localCallback)) {
            localCallback(bankProblem);
        }
        advanceDataListener.sendDataUpdate(CourseSketch.prutil.ItemQuery.BANK_PROBLEM, bankProblem.toArrayBuffer(), function(evt, item) {
            if (isException(item)) {
                serverCallback(new DatabaseException('exception thrown while waiting for response from sever',
                    'updating bank problem ' + bankProblem,  item));
                return;
            }
             // we do not need to make server changes we just need to make sure it was successful.
            if (!isUndefined(serverCallback)) {
                serverCallback(item);
            }
        });
    }
    parent.updateBankProblem = updateBankProblem;

    /**
     * Adds a new bankProblem to the server databases.
     *
     * @param {SrlBankProblem} bankProblem
     *                bankProblem object to insert.
     * @param {Function} serverCallback
     *                function to be called after server insert is done.  Called with the new updateId of the bank problem.
     */
    function insertBankProblemServer(bankProblem, serverCallback) {
        if (isUndefined(bankProblem.id) || bankProblem.id === null) {
            bankProblem.id = generateUUID();
        }
        advanceDataListener.sendDataInsert(CourseSketch.prutil.ItemQuery.BANK_PROBLEM, bankProblem.toArrayBuffer(), function(evt, item) {
            if (isException(item)) {
                serverCallback(new DatabaseException('exception thrown while waiting for response from sever',
                    'inesrting bank problem ' + bankProblem,  item));
                return;
            }
            var resultArray = item.getReturnText().split(':');
            // We do not need the old id as it is never stored in a way that we need to delete.
            var newId = resultArray[0].trim();
            // we return the new id knowing it was inserted in the database correctly.
            serverCallback(newId);
        });
    }

    /**
     * Adds a new courseProblem to both local and server databases. Also updates the
     * corresponding course given by the courseProblem's courseId.
     *
     * Inserts the bank problem in the case that the course problem does not exist.
     * This is detirmined if the courseproblem does not have a bankproblem id but does have the actual data for a bank problem.
     *
     * @param {SrlCourseProblem} courseProblem
     *                courseProblem object to insert
     * @param {Function} localCallback
     *                function to be called after local insert is done
     * @param {Function} serverCallback
     *                function to be called after server insert is done
     */
    function insertCourseProblem(courseProblem, localCallback, serverCallback) {
        if (isUndefined(courseProblem.id) || courseProblem.id === null) {
            courseProblem.id = generateUUID();
        }

        /**
         * This function is called after the bank problem is inserted if the course problem does not have a bank problem id.
         * Otherwise it is called immediately.
         */
        function insertingCourseProblem() {
            setCourseProblem(courseProblem, function() {
                console.log('inserted locally :' + courseProblem.id);
                if (!isUndefined(localCallback)) {
                    localCallback(courseProblem);
                }
                insertCourseProblemServer(courseProblem, function(courseProblemUpdated) {
                    parent.getAssignment(courseProblem.assignmentId, function(assignment) {
                        var courseProblemList = assignment.problemList;

                        // remove old Id (if it exists)
                        if (courseProblemList.indexOf(courseProblem.id) >= 0) {
                            removeObjectFromArray(courseProblemList, courseProblem.id);
                        }
                        courseProblemList.push(courseProblemUpdated.id);
                        parent.setAssignment(assignment, function() {
                            if (!isUndefined(serverCallback)) {
                                serverCallback(courseProblemUpdated);
                            }
                        });
                        // Assignment is set with its new courseProblem
                    });
                    // Finished with the course
                });
                // Finished with setting courseProblem
            });
        } // insertingCourseProblem

        // Inserts the bank problem first!
        if ((isUndefined(courseProblem.problemBankId) || courseProblem.problemBankId === null) &&
                (!isUndefined(courseProblem.problemInfo) && courseProblem.problemInfo !== null)) {
            insertBankProblemServer(courseProblem.problemInfo, function(updateId) {
                courseProblem.problemBankId = updateId;
                insertingCourseProblem();
            });
        } else {
            insertingCourseProblem();
        }

        // Finished with local courseProblem
    }
    parent.insertCourseProblem = insertCourseProblem;

    /**
     * Returns a list of all of the course problems from the local and server database for the given list
     * of Ids.
     *
     * This does attempt to pull course problems from the server!
     *
     * @param {List<String>} courseProblemIdList
     *            list of IDs of the courseproblems to get
     * @param {Function} courseProblemCallbackPartial
     *            {Function} called when course problems are grabbed from the local
     *            database only. This list may not be complete. This may also
     *            not get called if there are no local course problems.
     * @param {Function} courseProblemCallbackComplete
     *            {Function} called when the complete list of course problems are
     *            grabbed.
     */
    function getCourseProblems(courseProblemIdList, courseProblemCallbackPartial, courseProblemCallbackComplete) {
        /*
         * So what happens here might be a bit confusing to some new people so let me explain it.
         * #1 there is a loop that goes through every item in the courseProblemIdList (which is a list of courseProblem ids)
         *
         * #2 there is a function declaration inside the loop the reason for this is so that the courseProblemId is not overwritten
         * when the callback is called.
         *
         * #3 we call getCourseProblemLocal which then calls a callback about if it got an courseProblem or not if it didnt we add the id to a
         * list of Id we need to get from the server
         *
         * #4 after the entire list has been gone through (which terminates in the callback with barrier = 0)
         * if there are course problems that need to be pulled from the server then that happens.
         *
         * #5 after talking to the server we get a response with a list of courseProblems,
         * these are combined with the local courseProblems then the original callback is called.
         *
         * #6 the function pattern terminates.
         */

        // standard preventative checking
        if (isUndefined(courseProblemIdList) || courseProblemIdList === null || courseProblemIdList.length === 0) {
            courseProblemCallbackPartial(new DatabaseException('The given id is not assigned', 'getting CourseProblem: ' + courseProblemIdList));
            if (courseProblemCallbackComplete) {
                courseProblemCallbackComplete(new DatabaseException('The given id is not assigned', 'getting CourseProblem: ' + courseProblemIdList));
            }
        }

        var barrier = courseProblemIdList.length;
        var courseProblemList = [];
        var leftOverId = [];

        // create local courseProblem list so everything appears really fast!
        for (var i = 0; i < courseProblemIdList.length; i++) {
            var courseProblemIdLoop = courseProblemIdList[i];
            /**
             * The purpose of this function is purely to scope the courseProblemId so that it changes.
             *
             * @param {String} courseProblemId The id of a single courseProblem.
             */
            function loopContainer(courseProblemId) {
                getCourseProblemLocal(courseProblemId, function(courseProblem) {
                    if (!isUndefined(courseProblem) && !(courseProblem instanceof DatabaseException)) {
                        courseProblemList.push(courseProblem);
                    } else {
                        leftOverId.push(courseProblemId);
                    }
                    barrier -= 1;
                    if (barrier === 0) {
                        // after the entire list has been gone through pull the leftovers from the server
                        if (leftOverId.length >= 1) {
                            var itemRequest = CourseSketch.prutil.createItemRequest(CourseSketch.prutil.ItemQuery.COURSE_PROBLEM, leftOverId);
                            advanceDataListener.sendDataRequest(itemRequest, function(evt, item) {
                                if (isException(item)) {
                                    courseProblemCallbackComplete(new DatabaseException('exception thrown while waiting for response from sever',
                                        'getting course problem ' + leftOverId,  item));
                                    return;
                                }
                                if (isUndefined(item.data) || item.data === null || item.data.length <= 0) {
                                    courseProblemCallbackComplete(new DatabaseException('The data sent back from the server does not exist: ' +
                                            leftOverId));
                                    return;
                                }

                                for (var i = 0; i < item.data.length; i++) {
                                    var decodedCourseProblem = CourseSketch.prutil.getSrlProblemClass().decode(item.data[i]);
                                    parent.setCourseProblem(decodedCourseProblem);
                                    courseProblemList.push(decodedCourseProblem);
                                }
                                courseProblemCallbackComplete(courseProblemList);
                            });
                        }

                        // this calls actually before the response from the server is received!
                        if (courseProblemList.length > 0) {
                            courseProblemCallbackPartial(courseProblemList);
                        } else {
                            courseProblemCallbackPartial(new DatabaseException('Nothing is in the the local database!',
                                'Grabbing courseProblem from server: ' + leftOverId));
                        }
                    } // end of if(barrier === 0)
                }); // end of getting local courseProblem
            } // end of loopContainer
            loopContainer(courseProblemIdLoop);
        } // end of loop
    }
    parent.getCourseProblems = getCourseProblems;

    /**
     * Returns a courseProblem with the given courseId; this function will ask the server if it does not exist locally.
     *
     * If the server is polled and the courseProblem still does not exist the function will call the callback with an exception.
     *
     * @param {String} courseProblemId The id of the courseProblem we want to find.
     * @param {Function} courseProblemLocalCallback
     *            called when course problems are grabbed from the local
     *            database only. This list may not be complete. This may also
     *            not get called if there are no local course problems.
     * @param {Function} courseProblemServerCallback
     *            called when the complete list of course problems are
     *            grabbed.
     */
    function getCourseProblem(courseProblemId, courseProblemLocalCallback, courseProblemServerCallback) {
        getCourseProblems([ courseProblemId ], function(courseProblemList) {
            if (!isUndefined(courseProblemLocalCallback) && courseProblemList instanceof CourseSketch.DatabaseException) {
                courseProblemLocalCallback(new DatabaseException('Error with grabbing local course problem', courseProblemList));
                return;
            }
            if (courseProblemLocalCallback) {
                courseProblemLocalCallback(courseProblemList[0]);
            }
        }, function(courseProblemList) {
            if (!isUndefined(courseProblemServerCallback) && courseProblemList instanceof CourseSketch.DatabaseException) {
                courseProblemServerCallback(new DatabaseException('Error with grabbing remote course problem', courseProblemList));
                return;
            }
            if (courseProblemServerCallback) {
                courseProblemServerCallback(courseProblemList[0]);
            }
        });
    }
    parent.getCourseProblem = getCourseProblem;
}
