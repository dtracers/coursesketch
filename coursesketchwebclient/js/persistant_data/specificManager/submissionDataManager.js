function SubmissionDataManager(parent, advanceDataListener, parentDatabase, sendData, builders, buffer) {
	var userCourses = {};
	var userCourseId = [];
	var userHasCourses = true;
	var dataListener = advanceDataListener;
	var database = parentDatabase;
	var sendDataRequest = sendData;
	var Request = builders[0];
	var QueryBuilder = builders[1];
	var SubmissionBuilder = builders[2];
	var localScope = parent;
	var ByteBuffer = buffer;

	/**
	 * Returns a course with the given couresId will ask the server if it does not exist locally
	 *
	 * If the server is pulled and the course still does not exist the Id is set with nonExistantValue
	 * and the database is never polled for this item for the life of the program again.
	 *
	 * @param courseId The id of the course we want to find.
	 * @param courseCallback The method to call when the course has been found. (this is asynchronous)
	 */
	function getSubmission(problemId, submissionCallback) {
		database.getFromSubmissions(problemId, function(e, request, result) {
			if (isUndefined(result) || isUndefined(result.data)) {
				submissionCallback(undefined);
				return;
				// the listener from the server of the request
				// it stores the course locally then cals the callback with the course
				/*
				advanceDataListener.setListener(Request.MessageType.DATA_REQUEST, QueryBuilder.ItemQuery.COURSE, function(evt, item) {
					var school = SchoolBuilder.SrlSchool.decode(item.data);
					var course = school.courses[0];
					if (isUndefined(course)) {
						userCourses[courseId] = nonExistantValue;
						courseCallback(nonExistantValue);
						return;
					}
					localScope.setCourse(course);
					courseCallback(course);
				});
				// creates a request that is then sent to the server
				var dataSend = new QueryBuilder.DataRequest();
				dataSend.items = new Array();
				dataSend.items.push(new QueryBuilder.ItemRequest([courseId], QueryBuilder.ItemQuery.COURSE));
				serverConnection.sendRequest(serverConnection.createRequestFromData(dataSend, Request.MessageType.DATA_REQUEST));
				*/
			} else if (result.data == nonExistantValue) {
				// the server holds this special value then it means the server does not have the value
				submissionCallback(nonExistantValue);
			} else {
				// gets the data from the database and calls the callback
				var bytes = ByteBuffer.decode64(result.data);
				submissionCallback(SubmissionBuilder.SrlSubmission.decode(bytes));
			}
		});
	};
	parent.getSubmission = getSubmission;

	/**
	 * @param submission the submission that is being added
	 * @param id the id to which this submission is being added.
	 */
	function setSubmission(problemId, submission, submissionCallback) {
		database.putInSubmissions(problemId, submission.toBase64(), function(e, request) {
			if (submissionCallback) {
				submissionCallback(e, request);
			}
		});
	};
	parent.setSubmission = setSubmission;

	function deleteSubmission(problemId, couresCallback) {
		database.deleteFromSubmissions(problemId, function(e, request) {
			if (courseCallback) {
				courseCallback(e, request);
			}
		});
	};
	parent.deleteSubmission = deleteSubmission;
}