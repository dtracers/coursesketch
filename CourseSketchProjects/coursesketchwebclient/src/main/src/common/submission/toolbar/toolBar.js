/**
 * A toolbar that is used in problems.
 * This element appears over everything else
 */
function ProblemToolBar() {
    /**
     * @param {element} templateClone
     *            An element representing the data inside tag, its
     *            content has already been imported and then added to this
     *            element.
     */
    this.initializeElement = function(templateClone) {
        this.createShadowRoot();
        this.shadowRoot.appendChild(templateClone);
        this.initializeFixedActionButton();
    };

    /**
     * Sets the event listeners for the toolbar fixed action button
     */
    this.initializeFixedActionButton = function() {
        var fab = this.shadowRoot.querySelector('#toolbarFAB');
        fab.addEventListener('click', function(event) {
            if (this.classList.contains('active')) {
                $(this).closeFAB();
            } else {
                $(this).openFAB();
            }
        });
    };

    /**
     * Sets the callback for the submit button.
     */
    this.setSubmitCallback = function(submitCallback) {
        this.shadowRoot.querySelector('#submission').onclick = submitCallback;
    };

    /**
     * Sets the callback for the save button.
     */
    this.setSaveCallback = function(saveCallback) {
        this.shadowRoot.querySelector('#save').onclick = saveCallback;
    };

    /**
     * Sets the callback for the undo button.
     */
    this.setUndoCallback = function(undoCallback) {
        /**
         * Wraps the undo callback so that the panel does not close when it is clicked.
         *
         * @param {Event} event The click event
         */
        this.shadowRoot.querySelector('#undo').onclick = function(event) {
            event.stopPropagation();
            (undoCallback.bind(this))(event);
        };
    };

    /**
     * Sets the callback for the redo button.
     */
    this.setRedoCallback = function(redoCallback) {
        /**
         * Wraps the redo callback so that the panel does not close when it is clicked.
         *
         * @param {Event} event The click event
         */
        this.shadowRoot.querySelector('#redo').onclick = function(event) {
            event.stopPropagation();
            (redoCallback.bind(this))(event);
        };
    };

    /**
     * removes all of the previous callbacks making them undefined.
     */
    this.clearCallbacks = function() {
        this.setSubmitCallback(undefined);
        this.setSaveCallback(undefined);
        this.setUndoCallback(undefined);
        this.setRedoCallback(undefined);
    };

    /**
     * Returns an image element that can be added to the tool bar.
     * It will have the custom load functions and click functions
     */
    this.createButton = function(imgLocation, onclickFunction, onloadFunction) {
        var element = document.createElement('img');
        element.src = imgLocation;
        element.onclick = onclickFunction;
        element.onload = onloadFunction;
        element.className = 'specific_button';
        return element;
    };

}
ProblemToolBar.prototype = Object.create(HTMLDialogElement.prototype);
