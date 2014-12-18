function SubmissionDataManager(parent, advanceDataListener, parentDatabase, sendData, request, buffer) {
	var userCourses = {};
	var userCourseId = [];
	var userHasCourses = true;
	var dataListener = advanceDataListener;
	var database = parentDatabase;
	var Request = request;
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
			if (isUndefined(result) || isUndefined(result.data) || true) {

				// the listener from the server of the request
				// it stores the course locally then cals the callback with the course
				advanceDataListener.setListener(Request.MessageType.DATA_REQUEST, CourseSketch.PROTOBUF_UTIL.ItemQuery.EXPERIMENT, function(evt, item) {
					var experiment = CourseSketch.PROTOBUF_UTIL.getSrlExperimentClass().decode(item.data);
					var sub = experiment.submission;
					localScope.setSubmission(problemId, sub);
					submissionCallback(sub);
					sub = undefined;
					experiment = undefined;
					advanceDataListener.removeListener(Request.MessageType.DATA_REQUEST, CourseSketch.PROTOBUF_UTIL.ItemQuery.EXPERIMENT);
				});
				// creates a request that is then sent to the server
				sendData.sendDataRequest(CourseSketch.PROTOBUF_UTIL.ItemQuery.EXPERIMENT, [problemId]);
			} else if (result.data == nonExistantValue) {
				// the server holds this special value then it means the server does not have the value
				submissionCallback(nonExistantValue);
			} else {
				// gets the data from the database and calls the callback
				var bytes = ByteBuffer.fromBase64(result.data);
				submissionCallback(CourseSketch.PROTOBUF_UTIL.getSrlSubmissionClass().decode(bytes));
				bytes = null;
			}
		});
	};
	parent.getSubmission = getSubmission;

	function getAllExperiments(problemId, submissionCallback) {
		advanceDataListener.setListener(Request.MessageType.DATA_REQUEST, CourseSketch.PROTOBUF_UTIL.ItemQuery.EXPERIMENT, function(evt, item) {
			if (isUndefined(item.data)) {
				submissionCallback("Undefined");
				return;
			}
			var list;
			try {
				list = CourseSketch.PROTOBUF_UTIL.getSrlExperimentListClass().decode(item.data);
			} catch(exception) {
				return;
			}
			console.log(list.experiments);
			submissionCallback(list.experiments);
			list = null;
		});

		// creates a request that is then sent to the server
		var advanceQuery = CourseSketch.PROTOBUF_UTIL.ExperimentReview();
		advanceQuery.allowEditing = true;
		advanceQuery.showUserNames = false;
		sendData.sendDataRequest(CourseSketch.PROTOBUF_UTIL.ItemQuery.EXPERIMENT, [problemId], advanceQuery);
	}
	parent.getAllExperiments = getAllExperiments;

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