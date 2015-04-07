function VoicePlayback() {
    this.initializeElement = function(templateClone) {
        var localScope = this;
        shadowRoot = this.createShadowRoot();
        shadowRoot.appendChild(templateClone);

        this.shadowRoot.querySelector('#play-btn').onclick = function() {
            localScope.shadowRoot.querySelector('#pause-btn').style.display = 'block';
            localScope.shadowRoot.querySelector('#play-btn').style.display = 'none';
            playMe();
            playVoice();
        };
        this.shadowRoot.querySelector('#pause-btn').onclick = function() {
            localScope.shadowRoot.querySelector('#play-btn').style.display = 'block';
            localScope.shadowRoot.querySelector('#pause-btn').style.display = 'none';
            pauseVoice();
        };

        var surface = document.body.querySelector('sketch-surface');
        var graphics = surface.graphics;
        var updateManager = surface.getUpdateManager();

        localScope.shadowRoot.querySelector('#slider').slider({
            value: 100,
            min: 0,
            max: 500,
            step: 50,
            slide: function(event, ui) {
                localScope.shadowRoot.querySelector('#slider-value').html(ui.value);
            }
        });

        function playMe() {
            var graphics = surface.graphics;
            var updateList = surface.getUpdateList();
            var copyList = [];
            for (var i = 0; i < updateList.length; i++) {
                copyList.push(updateList[i]);
            }
            var updateManager = surface.getUpdateManager();
            updateManager.clearUpdates(false);

            var playBack = new Playback(copyList, updateManager, graphics);
            updateManager.addPlugin(playBack);
            playBack.playNext();
            //localScope.shadowRoot.querySelector("#play-btn").style.display = "block";
            //localScope.shadowRoot.querySelector("#pause-btn").style.display = "none";
        }

        function playVoice() {
            localScope.audio.play();
        }

        function pauseVoice() {
            localScope.audio.pause();
        }

        function init() {
            localScope.audio = document.createElement('audio');
            localScope.audio.src = '/src/utilities/templates/voicePlayback/TestRecording/mp3test.mp3';
        }
    };

}

VoicePlayback.prototype = Object.create(HTMLElement.prototype);

