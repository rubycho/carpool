import os
import time
import socket
import threading

HOST = '0.0.0.0'
PORT = 9998

###########################
# audio preprocessing code from
# https://towardsdatascience.com/building-a-speech-emotion-recognizer-using-python-4c1c7c89d713
import librosa as lb
import numpy as np
import soundfile as sf

import joblib
loaded = joblib.load('text.pkl')

def audio_features(wav_path, mfcc=True, chroma=True, mel=True):
    with sf.SoundFile(wav_path) as audio_recording:
        audio = audio_recording.read(dtype="float32")        
        sample_rate = audio_recording.samplerate
        
        if chroma:
            stft=np.abs(lb.stft(audio))
            result=np.array([])
        if mfcc:
            mfccs=np.mean(lb.feature.mfcc(y=audio, sr=sample_rate, n_mfcc=40).T, axis=0) # n_mfcc = 40
            result=np.hstack((result, mfccs))
        if chroma:
            chroma=np.mean(lb.feature.chroma_stft(S=stft, sr=sample_rate).T,axis=0) # n_chroma = 12
            result=np.hstack((result, chroma))
        if mel:
            mel=np.mean(lb.feature.melspectrogram(audio, sr=sample_rate).T,axis=0) # n_mels = 128
            result=np.hstack((result, mel))
        return result
###########################

def handle_cli(conn, addr):
    print("Thread initiated")

    ct = time.time()
    p = "./%f.wav" % (ct,)
    f = open(p, "wb")

    # TODO: iterate until 4 byte
    bsize = 4
    blimit = []

    while (bsize > 0):
        tmp = conn.recv(1)
        if len(tmp) > 0:
            blimit.append(tmp[0])
            bsize -= len(tmp)
    limit = int.from_bytes(blimit, "little")

    recved = 0
    while True:
        data = conn.recv(1024)
        if not data:
            break

        recved += len(data)
        print(len(data))
        f.write(data)

        if (recved >= limit):
            break
    f.close()

    feature = audio_features(p)
    y_pred = loaded.predict(feature.reshape(1, -1))

    conn.send(y_pred[0].encode())
    print(y_pred[0])
    os.rename(p, "./%f_%s.wav" % (ct, y_pred[0]))
    conn.close()


def run_serv():
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    s.bind((HOST, PORT))
    s.listen()

    while True:
        conn, addr = s.accept()
        threading.Thread(target=handle_cli, args=(conn, addr)).start()


if __name__ == '__main__':
    run_serv()
