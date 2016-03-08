/**
 * The MvSketch function handles all the action that can take place in the multiview units.
 *
 * Attributes:
 * data-binary: if set then the button will be disabled.
 * data-max_points: if set then this is the max number of points that can be input
 * data-max_percent: if set then this is the max percent of score that can be used
 * @class MvSketch
 */
function MvSketch() {
    this.maxValue = 100;
    this.gradeValue = undefined;

    /**
     * Sets the update list.
     *
     * After the update list is done loading this attempts to resize the sketch surface so that it fills the canvas correctly.
     * @param {SrlUpdateList} updateList a list that contains all the changes made in sketch.
     * @instance
     * @memberof MvSketch
     */
    this.setUpdateList = function(updateList)  {
        this.shadowRoot.querySelector('sketch-surface').loadUpdateList(updateList, undefined, function() {
            console.log('Resizing the canvas');
            this.shadowRoot.querySelector('sketch-surface').fillCanvas();
        }.bind(this));
    };

    /**
     * Sets the user id for display purposes.
     *
     * @param {String} userId the name that shows up for instructors to find the user name of.
     */
    this.setUserId = function(userId) {
        // Uses only the first 7 characters to ensure that they are easy to read by a human and do not take up much space on the screen.
        // But is long enough to ensure that there is high probability of uniqueness between all students in the class.
        this.shadowRoot.querySelector('#userName').textContent = userId.substring(0, 7);
        this.userId = userId;
    };

    this.setGrade = function(grade) {
        this.gradeValue = grade;
        this.shadowRoot.querySelector('#gradeInput').value = grade;
    };

    this.setComment = function(comment) {
        this.comment = comment;
        this.shadowRoot.querySelector('#comment').value = comment;
    };

    /**
     * This creates the shadow root and attaches it to the object in question.
     *
     * @param {Element} templateClone A clone of the shadow dom.
     * @instance
     * @memberof MvSketch
     */
    this.initializeElement = function(templateClone) {
        this.createShadowRoot();
        this.shadowRoot.appendChild(templateClone);

        var correctButton = this.shadowRoot.querySelector('.correctButton');
        var wrongButton = this.shadowRoot.querySelector('.wrongButton');
        var submitButton = this.shadowRoot.querySelector('#submitButton');
        correctButton.onclick = correct.bind(this);
        wrongButton.onclick = wrong.bind(this);
        submitButton.onclick = saveGrade.bind(this);

        if (Waves) {
            Waves.attach(correctButton, 'waves-effect waves-light');
            Waves.attach(wrongButton, 'waves-effect waves-light');
            Waves.attach(submitButton, null);
        }

        this.shadowRoot.querySelector('input').addEventListener('click',
            function(event) {
                event.stopPropagation();
            }, false);
        this.setupAttributes();

    };

    /**
     * Looks at the data attributes of this element and configures the element appropriately.
     *
     * @instance
     * @memberof MvSketch
     */
    this.setupAttributes = function() {
        if (!isUndefined(this.dataset) && this.dataset.binary === 'true' || this.dataset.binary === '') {
            this.shadowRoot.querySelector('#gradeInput').disabled = true;
        }
        if (!isUndefined(this.dataset) && !isUndefined(this.dataset.max_points) && this.dataset.max_points !== '') {
            this.shadowRoot.querySelector('#gradeInput').max = this.dataset.max_points;
            this.shadowRoot.querySelector('#gradeInput').className = 'point';
            this.maxValue = parseFloat(this.dataset.max_points);
        }
        if (!isUndefined(this.dataset) && !isUndefined(this.dataset.max_percent) && this.dataset.max_percent !== '') {
            this.shadowRoot.querySelector('#gradeInput').max = this.dataset.max_percent;
            this.shadowRoot.querySelector('#gradeInput').className = 'percent';
        }
    };

    /**
     * Marks the sketch at correct and changes the background to outercorrect.
     *
     * @param {Event} event The event propagation is stopped.
     * @instance
     * @memberof MvSketch
     * @access private
     */
    function correct(event) {
        event.stopPropagation();
        this.gradeValue = this.maxValue;
        this.shadowRoot.querySelector('#outline').className = 'green accent-1';
        this.shadowRoot.querySelector('#outer').className = 'outerCorrect';
        this.shadowRoot.querySelector('#gradeInput').value = parseFloat(this.gradeValue);
    }

    /**
     * Marks the sketch as wrong and changes the background to outerwrong.
     *
     * @param {Event} event The event propagation is stopped.
     * @instance
     * @memberof MvSketch
     * @access private
     */
    function wrong(event) {
        event.stopPropagation();
        this.gradeValue = 0;
        this.shadowRoot.querySelector('#outline').className = 'red accent-1';
        this.shadowRoot.querySelector('#outer').className = 'outerWrong';
        this.shadowRoot.querySelector('#gradeInput').value = parseFloat(this.gradeValue);
    }

    function saveGrade(event) {
        event.stopPropagation();
        var protoGrade = CourseSketch.PROTOBUF_UTIL.ProtoGrade();
        if (!isUndefined(this.courseId)) {
            protoGrade.courseId = this.courseId;
        }
        if (!isUndefined(this.assignmentId)) {
            protoGrade.assignmentId = this.assignmentId;
        }
        if (!isUndefined(this.problemId)) {
            protoGrade.problemId = this.problemId;
        }
        if (!isUndefined(this.userId)) {
            protoGrade.userId = this.userId;
        }
        var grade = parseFloat(this.shadowRoot.querySelector('#gradeInput').value);
        var comment = this.shadowRoot.querySelector('#comment').value;

        var gradeHistory = CourseSketch.PROTOBUF_UTIL.GradeHistory();
        if (!isNaN(grade)) {
            gradeHistory.setGradeValue(grade);
        }
        if (!isUndefined(comment)) {

            gradeHistory.setComment(comment);
        }
        protoGrade.setGradeHistory(gradeHistory); // Don't need to add to list since there is only one gradeHistory value
        CourseSketch.dataManager.setGrade(protoGrade, function() {
            console.log('submission successful!');
        });
    }

    /**
     * Sets the callback that is called when the sketch is clicked.
     *
     * @param {Function} sketchClickedFunction A function called when the sketch is clicked.
     * @instance
     * @memberof MvSketch
     */
    this.setSketchClickedFunction = function(sketchClickedFunction) {
        this.shadowRoot.querySelector('sketch-surface').onclick = sketchClickedFunction;
    };

    this.setAssignmentId = function(assignmentId) {
        this.assigmentId = asignmentId;
    };

    this.setCourseId = function(courseId) {
        this.courseId = courseId;
    };

    this.setProblemId = function(problemId) {
        this.problemId = problemId;
    };
}

MvSketch.prototype = Object.create(HTMLElement.prototype);
