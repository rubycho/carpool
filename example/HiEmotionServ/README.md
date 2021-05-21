## HiEmotion Socket Server

* HiEmotion app collects user voices, and send to this socket server.
* The server loads emotion detection model, predicts emotion, and send result to HiEmotion app.
* The model (classifier, `text.pkl`) is a 2 hidden layer perceptron model, trained with RAVDESS dataset.
* The model has five classes: happy, angry, sad, fearful, calm.
* [Reference](https://towardsdatascience.com/building-a-speech-emotion-recognizer-using-python-4c1c7c89d713) for training.
