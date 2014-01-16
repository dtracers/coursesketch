function CourseDataManager(parent, advanceDataListener, parentDatabase, sendData, builders, buffer) {
	const COURSE_LIST = "COURSE_LIST";
	var userCourses = {};
	var userCourseId = [];
	var userHasCourses = true;
	var dataListener = advanceDataListener;
	var database = parentDatabase;
	var sendDataRequest = sendData;
	var Request = builders[0];
	var QueryBuilder = builders[1];
	var SchoolBuilder = builders[2];
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
	function getCourse(courseId, courseCallback) {
		// quick and dirty this is in ram (not in local memory)
		if (!isUndefined(userCourses[courseId])) {
			if (userCourses[courseId] == nonExistantValue) {
				courseCallback(nonExistantValue);
				return;
			}
			var bytes = ByteBuffer.decode64(userCourses[courseId]);
			courseCallback(SrlCourse.decode(bytes));
			return;
		}
		database.getFromCourses(courseId, function(e, request, result) {
			if (isUndefined(result) || isUndefined(result.data)) {
				// the listener from the server of the request
				// it stores the course locally then cals the callback with the course
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
			} else if (result.data == nonExistantValue) {
				// the server holds this special value then it means the server does not have the value
				courseCallback(nonExistantValue);
				userCourses[courseId] = nonExistantValue;
			} else {
				// gets the data from the database and calls the callback
				userCourses[courseId] = result.data;
				var bytes = ByteBuffer.decode64(result.data);
				courseCallback(SrlCourse.decode(bytes));
			}
		});
	};
	parent.getCourse = getCourse;

	function setCourse(course, courseCallback) {
		database.putInCourses(course.id, course.toBase64(), function(e, request) {
			if (courseCallback) {
				courseCallback(e, request);
			}
		});
		userCourses[course.id] = course.toBase64(); // stored in memory
	};
	parent.setCourse = setCourse;

	function deleteCourse(courseId, couresCallback) {
		database.deleteFromCourses(courseId, function(e, request) {
			if (courseCallback) {
				courseCallback(e, request);
			}
		});
		userCourses[course.id] = undefined; // removing it from the local map
	};
	parent.deleteCourse = deleteCourse;
	
	function setCourseIdList(idList) {
		userCourseId = idList;
		database.putInCourses(COURSE_LIST, idList); // no call back needed!
	}

	/**
	 * Returns a list of all of the courses in database.
	 *
	 * This does attempt to pull courses from the server!
	 */
	function getAllCourses(courseCallback) {
		var localFunction = setCourseIdList;
		// there are no courses loaded onto this client!
		advanceDataListener.setListener(Request.MessageType.DATA_REQUEST, QueryBuilder.ItemQuery.SCHOOL, function(evt, item) {
			if (!isUndefined(item.returnText) && item.returnText != "" && item.returnText !="null" && item.returnText != null) {
				userHasCourses = false;
				console.log(item.returnText);
				alert(item.returnText);
				return;
			}
			var school = SchoolBuilder.SrlSchool.decode(item.data);
			var courseList = school.courses;
			var idList = [];
			for(var i = 0; i < courseList.length; i++) {
				var course = courseList[i];
				localScope.setCourse(course); // no callback is needed
				idList.push(course.id);
			}
			courseCallback(courseList);
			setCourseIdList(idList);
		});
		if (userCourseId.length == 0 && userHasCourses) {
			sendDataRequest(QueryBuilder.ItemQuery.SCHOOL, [""]);
			console.log("course list from server polled!");
		} else {
			// This calls the server for updates then creates a list from the local data to appear fast
			// then updates list after server polling and comparing the two list.
			console.log("course list from local place polled!");
			var barrier = userCourseId.length;
			var courseList = [];
			// need to create an update function!
			/*
			advanceDataListener.setListener(Request.MessageType.DATA_REQUEST, QueryBuilder.ItemQuery.COURSE_LIST, function(evt, item) {
				console.log("Course data!!!!!!!");
			});
			*/
			
			// ask server for course list
			sendDataRequest(QueryBuilder.ItemQuery.SCHOOL, [""]);

			// create local course list so everything appears really fast!
			for (var i = 0; i < userCourseId.length; i++) {
				this.getCourse(userCourseId[i], function(course) {
					courseList.push(course);
					barrier -= 1;
					if (barrier == 0) {
						courseCallback(courseList);
					}
				});
			}

			// we ask the program for the list of courses by id then we compare and update!
		}
	};
	parent.getAllCourses = getAllCourses;

	/*
	 * gets the id's of all of the courses in the user's local client.
	 */
	database.getFromCourses(COURSE_LIST, function(e, request, result) {
		if (isUndefined(result) || isUndefined(result.data)) {
			return;
		}
		userCourseId = result.data;
	});
}