validateFirstRun(document.currentScript);
/**
 * @namespace lecturePage
 */

(function() {
    CourseSketch.lecturePage = [];
    CourseSketch.lecturePage.waitScreenManager = new WaitScreenManager();
    CourseSketch.lecturePage.selectedSlideIndex = undefined;

    /**
     * Resizes the element that was affected by the event.
     *
     * @param {Event} event the event that contains the element needing a resize.
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.doResize = function(event) {
        var target = event.target;

        // Add the change in coords to the previous width of the target element
        var newWidth  = parseFloat($(target).width()) + event.dx;
        var newHeight = parseFloat($(target).height()) + event.dy;

        // Update the element's style
        target.style.width  = newWidth + 'px';
        target.style.height = newHeight + 'px';

        target.textContent = newWidth + '×' + newHeight;
    };

    /**
     * Creates a new text box and loads data into it.
     *
     * @param {TextBoxProto} textBox the data needed for the text box.
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.loadTextBox = function(textBox) {
        var elem = CourseSketch.lecturePage.newTextBox();
        elem.loadData(textBox);
    };

    /**
     * Creates a new question element and loads data into it.
     *
     * @param {QuestionProto} question the data needed for the question.
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.loadMultiChoiceQuestion = function(question) {
        var elem = CourseSketch.lecturePage.newMultiChoiceQuestion();
        elem.loadData(question);
    };

    /**
     * Creates a new imageBox element and loads data into it.
     *
     * @param {ImageProto} imageBox the data needed for the image.
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.loadImageBox = function(imageBox) {
        var elem = CourseSketch.lecturePage.newImage();
        elem.loadData(imageBox);
    };

    /**
     * Creates a new embeddedHtml element and loads data into it.
     *
     * @param {embeddedHtmlProto} embeddedHtml the data needed for the embedded html page.
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.loadEmbeddedHtml = function(embeddedHtml) {
        var elem = CourseSketch.lecturePage.newEmbeddedHtml();
        elem.loadData(embeddedHtml);
    };

    /**
     * Adds a new text box to the currently selected lecture slide.
     *
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.newTextBox = function() {
        var textBox = undefined;
        if (CourseSketch.connection.isInstructor) {
            textBox = document.createElement('text-box-creation');
        } else {
            textBox = document.createElement('text-box-viewing');
        }
        document.querySelector('#slide-content').appendChild(textBox);
        textBox.setFinishedListener(CourseSketch.lecturePage.saveTextBox);
        return textBox;
    };

    /**
     * Adds a new sketch content element to the currently selected slide.
     *
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.newSketchContent = function() {
        var sketchSurface = document.createElement('sketch-surface');
        document.querySelector('#slide-content').appendChild(sketchSurface);
        setTimeout(function() {
            sketchSurface.resizeSurface();
        }, 500);
        return sketchSurface;
    };

    /**
     * Adds a new image to the currently selected slide.
     *
     * @param {element} input the input element from the form specifying the image.
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.newImage = function(input) {
        var imagebox = document.createElement('image-box');
        document.querySelector('#slide-content').appendChild(imagebox);

        // TODO: Save resize info in DB; better to leave this disabled until that works
        // imagebox.className = 'resize';

        if (!isUndefined(input) && input !== null && input.files && input.files[0]) {
            var reader = new FileReader();
            /* jscs:disable jsDoc */
            reader.onload = function(e) {
                imagebox.setSrc(e.target.result);
            };
            reader.readAsDataURL(input.files[0]);
            /* jscs:enable jsDoc */
        }
        imagebox.setFinishedListener(CourseSketch.lecturePage.saveImageBox);
        return imagebox;
    };

    /**
     * Adds a new embedded HTML element to the currently selected slide.
     *
     * @param {element} form the form that contains the HTML element to be added.
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.newEmbeddedHtml = function(form) {
        var embeddedHtml = document.createElement('embedded-html');
        document.querySelector('#slide-content').appendChild(embeddedHtml);
        if (!isUndefined(form) && form !== null) {
            embeddedHtml.setHtml(form.html.value);
        }
        embeddedHtml.setFinishedListener(CourseSketch.lecturePage.saveEmbeddedHtml);
        return embeddedHtml;
    };

    /**
     * Adds a new multiple choice question to the currently selected slide.
     *
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.newMultiChoiceQuestion = function() {
        var question = document.createElement('question-element');
        var multiChoice = document.createElement('multi-choice');
        document.getElementById('slide-content').appendChild(question);
        question.addAnswerContent(multiChoice);
        question.setFinishedListener(CourseSketch.lecturePage.saveQuestion);
        return question;
    };

    /**
     * Renders a slide to the DOM.
     *
     * @param {protoObject} slide protobuf slide element to be rendered.
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.renderSlide = function(slide) {
        document.getElementById('slide-content').innerHTML = '';
        CourseSketch.lecturePage.currentSlide = slide;
        for (var i = 0; i < slide.elements.length; ++i) {
            var element = slide.elements[i];
            if (!isUndefined(element.textBox) && element.textBox !== null) {
                CourseSketch.lecturePage.loadTextBox(element.textBox);
            } else if (!isUndefined(element.question) && element.question !== null) {
                if (!isUndefined(element.question.multipleChoice) && element.question.multipleChoice !== null) {
                    CourseSketch.lecturePage.loadMultiChoiceQuestion(element.question);
                } else {
                    throw 'Sketch questions are not yet supported';
                }
            } else if (!isUndefined(element.image) && element.image !== null) {
                CourseSketch.lecturePage.loadImageBox(element.image);
            } else if (!isUndefined(element.embeddedHtml) && element.embeddedHtml !== null) {
                CourseSketch.lecturePage.loadEmbeddedHtml(element.embeddedHtml);
            } else {
                throw 'Tried to load invalid element';
            }
        }
    };

    /**
     * Adds a wait overlay, preventing the user from interacting with the page until it is removed.
     *
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.addWaitOverlay = function() {
        CourseSketch.lecturePage.waitScreenManager.buildOverlay(document.querySelector('body'));
        CourseSketch.lecturePage.waitScreenManager.buildWaitIcon(document.getElementById('overlay'));
        document.getElementById('overlay').querySelector('.waitingIcon').classList.add('centered');
    };

    /**
     * Removes the wait overlay from the DOM if it exists.
     *
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.removeWaitOverlay = function() {
        if (!isUndefined(document.getElementById('overlay')) && document.getElementById('overlay') !== null) {
            document.querySelector('body').removeChild(document.getElementById('overlay'));
        }
    };

    /**
     * Adds a slide thumbnail to the DOM.
     *
     * @param {Integer} slideIndex index of the slide in the current lecture's protobuf object.
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.addSlideToDom = function(slideIndex) {
        var slideThumb = document.createElement('span');
        slideThumb.id = slideIndex;
        slideThumb.className = 'slide-thumb';
        slideThumb.textContent = slideIndex + 1;
        /* jscs:disable jsDoc */
        slideThumb.onclick = function() {
            CourseSketch.lecturePage.selectSlide(slideIndex);
        };
        /* jscs:enable jsDoc */
        document.querySelector('#slides>.content').appendChild(slideThumb);
    };

    /**
     * Displays all of the slides for the current lecture.
     *
     * @memberof lecturePage
     */
    CourseSketch.lecturePage.displaySlides = function() {
        $('#lecture-title').text(CourseSketch.lecturePage.lecture.name);
        $('.slide-thumb:not("#add")').each(function() {
            $(this).remove();
        });
        for (var i = 0; i < CourseSketch.lecturePage.lecture.idList.length; ++i) {
            CourseSketch.lecturePage.addSlideToDom(i);
        }
        if (CourseSketch.lecturePage.lecture.idList.length > 0) {
            if (!isUndefined(CourseSketch.lecturePage.selectedSlideIndex)) {
                CourseSketch.lecturePage.selectSlide(CourseSketch.lecturePage.selectedSlideIndex);
            } else {
                CourseSketch.lecturePage.selectSlide(0);
            }
        } else {
            CourseSketch.lecturePage.newSlide();
        }
    };
})();
