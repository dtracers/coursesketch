validateFirstRun(document.currentScript);

/**
 * @namespace courseManagement
 */

/**
 * The element that handles the waiting icon.
 *
 * [TODO change docs to custom element]
 * @memberof courseManagement
 */
CourseSketch.courseManagement.waitingIcon = (function() {
    var manage = new WaitScreenManager();
    manage.waitIconText = 'loading data';
    return manage.setWaitType(manage.TYPE_WAITING_ICON).build();
})();
(function() {

    var waitingIcon = CourseSketch.courseManagement.waitingIcon;
    var courseManagement = CourseSketch.courseManagement;

    /**
     * Polls for all updates to the user and then shows the courses.
     *
     * This will wait till the database is ready before it polls for updates and
     * shows the courses.
     * @name initializeCourseManagment
     * @memberof courseManagement
     */
    CourseSketch.courseManagement.initializeCourseManagment = function() {
        if (!document.querySelector('#class_list_column')) {
            return false;
        }
        document.querySelector('#class_list_column').appendChild(waitingIcon);
        CourseSketch.courseManagement.waitingIcon.startWaiting();

        /**
         * Helper function to stop the waiting icon and show the courses once that database is ready.
         * @memberof courseManagement
         */
        var loadCourses = function(courseList) {
            if (waitingIcon.isRunning()) {
                waitingIcon.finishWaiting();
            }
            courseManagement.showCourses(courseList);
        };
        if (CourseSketch.dataManager.isDatabaseReady()) {
            CourseSketch.dataManager.getAllCourses(loadCourses);
        } else {
            var intervalVar = setInterval(function() {
                if (CourseSketch.dataManager.isDatabaseReady()) {
                    clearInterval(intervalVar);
                    CourseSketch.dataManager.getAllCourses(loadCourses);
                }
            }, 100);
        }
    };

    /**
     * Given a list of {@link SrlCourse} a bunch of school items are built then added to the class_list_column div.
     *
     * @memberof courseManagement
     */
    courseManagement.showCourses = function showCourses(courseList) {
        if (CourseSketch.isException(courseList)) {
            CourseSketch.clientException(courseList);
        }
        var builder = new SchoolItemBuilder();
        if (CourseSketch.connection.isInstructor === true) {
            builder.setInstructorCard(true);
        }
        builder.showImage = false;
        builder.setBoxClickFunction(function(course) {
            courseManagement.courseClicked(course);
        });

        if (courseList instanceof CourseSketch.DatabaseException || courseList.length === 0) {
            if (!isUndefined(courseList.getCause()) && courseList.getCause() instanceof CourseSketch.AdvanceListenerException) {
                CourseSketch.clientException(courseList);
                builder.setEmptyListMessage('An exception occurred while getting the course. Please try again later.');
            } else if (CourseSketch.connection.isInstructor) {
                builder.setEmptyListMessage('Please create a new course to get started!');
            } else {
                builder.setEmptyListMessage('Please add a new course to get started.');
            }
            courseList = [];
        }

        builder.setList(courseList);
        builder.build(document.querySelector('#class_list_column'));
        setNotSelectedMessage(2);
    };

    /**
     * Called when a user clicks on a course school item.
     *
     * This loads the assignments from the database then calls 'showAssignments' to display them.
     * @memberof courseManagement
     */
    courseManagement.courseClicked = function(course) {
        var classColumn = document.querySelector('#class_list_column');
        setNotSelectedMessage(2);

        // note that query selector does not work on ids that start with a number.
        changeSelection(classColumn.querySelector(cssEscapeId(course.id)), courseSelectionManager);
        assignmentSelectionManager.clearAllSelectedItems();
        problemSelectionManager.clearAllSelectedItems();

        // waiting icon
        document.querySelector('#assignment_list_column').appendChild(waitingIcon);
        waitingIcon.startWaiting();

        // we can make this faster because we have the list of assignments
        CourseSketch.dataManager.getAssignments(course.assignmentList, function(assignmentList) {
            courseManagement.showAssignments(assignmentList, course);
        }, function(assignmentList) {
            courseManagement.showAssignments(assignmentList, course);
            if (waitingIcon.isRunning()) {
                waitingIcon.finishWaiting(); // stops the
                // waiting icon
            }
        });
    };

    /**
     * Called to show a specific set of assignments with the given list.
     *
     * @memberof courseManagement
     */
    courseManagement.showAssignments = function(assignmentList, course) {
        if (CourseSketch.isException(assignmentList)) {
            CourseSketch.clientException(assignmentList);
        }
        var builder = new SchoolItemBuilder();
        if (CourseSketch.connection.isInstructor === true) {
            builder.setInstructorCard(true);
        }
        builder.setEmptyListMessage('There are no assignments for this course!');
        if (assignmentList instanceof CourseSketch.DatabaseException) {
            builder.setEmptyListMessage('An exception was thrown, so assignments can not be loaded.');
            if (!isUndefined(course) && course.getState() !== null && !(course.getState().accessible)) {
                builder.setEmptyListMessage('This course is currently not available. Please contact the instructor to let you view the assignments');
            }
            assignmentList = [];
        }

        builder.setList(assignmentList);
        builder.showImage = false;

        builder.setBoxClickFunction(function(assignment) {
            courseManagement.assignmentClicked(assignment);
        });
        builder.build(document.querySelector('#assignment_list_column'));
        document.querySelector('#assignment_list_column').appendChild(waitingIcon); // because it was probably removed
    };


    /**
     * Called when an assignment is clicked.
     *
     * @memberof courseManagement
     */
    courseManagement.assignmentClicked = function(assignment) {
        var assignmentColumn = document.querySelector('#assignment_list_column');
        changeSelection(assignmentColumn.querySelector(cssEscapeId(assignment.id)), assignmentSelectionManager);
        problemSelectionManager.clearAllSelectedItems();

        // waiting icon
        document.getElementById('problem_list_column').appendChild(waitingIcon);
        waitingIcon.startWaiting();
        CourseSketch.dataManager.getCourseProblems(assignment.problemList, function(problemList) {
            courseManagement.showProblems(problemList, assignment);
        }, function(problemList) {
            courseManagement.showProblems(problemList, assignment);
            if (waitingIcon.isRunning()) {
                waitingIcon.finishWaiting(); // stops the
                // waiting icon
            }
        });
    };

    /**
     * Displays the list of problems for the user to pick from.
     *
     * @param {list} problemList The list of problems that are wanting to be showed
     * @param {assignment} assignment (optional) The assignment that created this problem list
     * @memberof courseManagement
     */
    courseManagement.showProblems = function(problemList, assignment) {
        if (CourseSketch.isException(problemList)) {
            CourseSketch.clientException(problemList);
        }
        var builder = new SchoolItemBuilder();
        if (CourseSketch.connection.isInstructor === true) {
            builder.setInstructorCard(true);
        }
        builder.setEmptyListMessage('There are no problems for this assignment!');
        if (problemList instanceof CourseSketch.DatabaseException) {
            builder.setEmptyListMessage('An exception was thrown so problems can not be loaded.');
            problemList = [];
            if (!isUndefined(assignment) && assignment.getState() !== null && !assignment.getState().accessible) {
                builder.setEmptyListMessage('This assignment is currently not available. ' +
                        'Please contact the instructor to let you view the problems');
            }
        }
        builder.setList(problemList);
        builder.showImage = false;
        builder.setBoxClickFunction(courseManagement.problemClicked);
        builder.build('problem_list_column');
    };

    /**
     * Called when a problem is displayed.
     *
     * @memberof courseManagement
     */
    courseManagement.problemClicked = function(problem) {
        var problemColumn = document.querySelector('#problem_list_column');
        var clickedElement = problemColumn.querySelector(cssEscapeId(problem.id));

        if (problemSelectionManager.isItemSelected(clickedElement)) {
            var itemNumber = clickedElement.dataset.item_number;
            CourseSketch.dataManager.addState('currentProblemIndex', itemNumber);
            CourseSketch.dataManager.addState('currentAssignment', problem.assignmentId);
            CourseSketch.dataManager.addState('CURRENT_QUESTION', problem.id);
            // change source to the problem page! and load problem
            if (CourseSketch.connection.isInstructor) {
                // solution editor page!
                CourseSketch.redirectContent('/src/instructor/review/multiviewGrading.html', 'Grading problems!');
            } else {
                CourseSketch.redirectContent('/src/student/experiment/experiment.html', 'Starting Problem');
            }
        } else {
            // TODO: find a more lightweight popup library
            /*
            var element = document.getElementById(id);
            var myOpenTip = new Opentip(element, {
                target : element,
                tipJoint : 'bottom'
            });
            myOpenTip.prepareToShow(); // Shows the tooltip after the given
            // delays. This could get interrupted

            if (CourseSketch.dataManager.getState('isInstructor')) {
                myOpenTip.setContent('Click again to edit the solution'); // Updates
                // Opentips
                // content
            } else {
                myOpenTip.setContent('Click again to open up a problem'); // Updates
                // Opentips
                // content
            }

            var pastToolTip = problemSelectionManager['currentToolTip'];
            if (pastToolTip) {
                pastToolTip.deactivate();
            }
            problemSelectionManager['currentToolTip'] = myOpenTip;
            */
            // note that queryselector is not allowed on these types of ids
            changeSelection(clickedElement, problemSelectionManager);
        }
    };

    /**
     * Sets the message to hint that the previous column is selectable and gives prompts to action.
     *
     * @memberof courseManagement
     */
    function setNotSelectedMessage(number) {
        var builder = new SchoolItemBuilder();

        if (number > 0) {
            builder.setEmptyListMessage('Please select an assignment to see the list of problems.');
            builder.build(document.querySelector('#problem_list_column'));
        }

        if (number > 1) {
            builder.setEmptyListMessage('Please select a course to see the list of assignments.');
            builder.build(document.querySelector('#assignment_list_column'));
        }
    }

    /**
     * A helper method to simplify the code for changing the selection.
     *
     * Clears the existing selection then selects the given id.
     * @memberof courseManagement
     */
    function changeSelection(id, selectionManager) {
        selectionManager.clearAllSelectedItems();
        selectionManager.addSelectedItem(id);
    }

    var courseSelectionManager = new ClickSelectionManager();
    var assignmentSelectionManager = new ClickSelectionManager();
    var problemSelectionManager = new ClickSelectionManager();
})();
