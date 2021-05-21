# carpool

## Commit Description

* \[VA\]: Voice Assistant
* \[HE\]: Hi Emotion

### Initial commit (2021/04/05)

* Added maintenance related files.

### [VA] feat: aimybox (2021/04/15)

* Applied Aimybox Framework (framework for voice assistant).

### [VA] docs: add readme (2021/04/15)

### [VA] feat: add kaldi plugin (2021/04/16)

* Added kaldi plugin for voice trigger. But not working.

### [VA] feat: start with recognition mode (2021/04/16)

* Made voice assistant to speak first on activity start.

### [VA] feat: read notifications (2021/04/19)

* Added `NotificationListener` service for reading notifications.

### [VA] feat: add NotificationProxy (2021/04/21)

* Added utility function related for pusing notifications.

### [VA] refactor: NotificationListener (2021/04/21)

* Moved notification-read related codes on `MainActivity` to `NotificationListener`.

### [VA] feat: new version (without aimybox) (2021/04/29)

* Implemented new VA without Aimybox Framework. Now uses `SpeechRecognizer` and `TextToSpeech`.

### [VA] chore: remove old version (2021/04/29)

### [VA] add README (2021/04/29)

### [VA] feat: vibrate before alerting user (2021/05/04)

* Make phone vibrate before alerting user for unreplied notification.

### [VA] feat: add application state (2021/05/16)

* Roughly implemented state machine of application.

### [HE] feat: add WavRecorder (2021/05/16)

* Initialized project HiEmotion.
* Added `WavRecorder` class that records and (will) detects human voice.
* Roughly using ZCR (Zero Crossing Rate) and Energy (sum of `data[i]**2`) for detection.

### [HE] feat: add FFT library (2021/05/19)

* Applied JTransform (FFT) library to eliminate noises on audio for better detection.
* Need more **discovery** about library & FFT.

### update README (2021/05/19)

### [HE] feat: replace FFT(DSP) library (2021/05/21)

* Remove JTransform, Applied TarsosDSP which has pitch detection.

### [HE] feat: add socket server & emotion model (2021/05/21)

* Add python socket server & MLP model where input=audio features, output=emotion (softmax).
