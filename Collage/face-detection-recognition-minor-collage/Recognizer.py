from contextlib import closing
from queue import Empty, Full, Queue
import sqlite3
from pathlib import Path
from threading import Event, Thread
from time import monotonic

import cv2

from camera_utils import is_dark_frame, open_camera, show_preview


PROJECT_DIR = Path(__file__).resolve().parent
DATABASE_PATH = PROJECT_DIR / "FaceBase.db"
MODEL_PATH = PROJECT_DIR / "trainner" / "trainningData.yml"
FACE_SIZE = (200, 200)
CONFIDENCE_THRESHOLD = 70
RECOGNITION_DURATION_SECONDS = 120


def getProfile(person_id):
    if not DATABASE_PATH.is_file():
        return None
    with closing(sqlite3.connect(DATABASE_PATH)) as connection:
        return connection.execute(
            "SELECT ID, Name FROM People WHERE ID = ?", (int(person_id),)
        ).fetchone()


def recognize(camera_index=0, duration_seconds=RECOGNITION_DURATION_SECONDS):
    if not MODEL_PATH.is_file():
        raise FileNotFoundError(
            "No trained model found. Capture face images and train the model first."
        )
    if duration_seconds <= 0:
        raise ValueError("Recognition duration must be greater than zero.")

    recognizer = cv2.face.LBPHFaceRecognizer_create()
    recognizer.read(str(MODEL_PATH))

    cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
    face_cascade = cv2.CascadeClassifier(cascade_path)
    if face_cascade.empty():
        raise RuntimeError("OpenCV could not load its bundled face-detection cascade.")

    camera = open_camera(camera_index)
    frames = Queue(maxsize=1)
    stop_reader = Event()

    def publish_frame(result):
        try:
            frames.put_nowait(result)
        except Full:
            try:
                frames.get_nowait()
            except Empty:
                pass
            try:
                frames.put_nowait(result)
            except Full:
                pass

    def read_frames():
        while not stop_reader.is_set():
            try:
                success, image = camera.read()
            except Exception as error:
                publish_frame((False, error))
                return
            if not success:
                publish_frame(
                    (
                        False,
                        RuntimeError(
                            "Camera {} stopped returning frames.".format(camera_index)
                        ),
                    )
                )
                return
            publish_frame((True, image))

    reader = Thread(target=read_frames, name="webcam-frame-reader", daemon=True)
    reader.start()
    deadline = monotonic() + duration_seconds

    try:
        print(
            "Recognition runs for up to {} seconds. Press q to quit early.".format(
                duration_seconds
            )
        )
        while True:
            remaining = deadline - monotonic()
            if remaining <= 0:
                print("Recognition time limit reached. Closing the camera preview.")
                break

            try:
                success, result = frames.get(timeout=min(remaining, 0.5))
            except Empty:
                continue
            if not success:
                raise result
            image = result

            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            warning = None
            if is_dark_frame(image):
                status = "Camera is returning a nearly black image."
                warning = (
                    "Check the camera lens/privacy shutter, Windows Camera app, and "
                    "camera permissions. Press q to close this window."
                )
            else:
                faces = face_cascade.detectMultiScale(
                    cv2.equalizeHist(gray), scaleFactor=1.1, minNeighbors=4
                )
                for x, y, width, height in faces:
                    face = cv2.resize(gray[y:y + height, x:x + width], FACE_SIZE)
                    person_id, distance = recognizer.predict(face)
                    profile = getProfile(person_id)

                    if profile is not None and distance < CONFIDENCE_THRESHOLD:
                        label = "{} ({:.0f}% match)".format(
                            profile[1], max(0, 100 - distance)
                        )
                        color = (0, 150, 0)
                    else:
                        label = "Unknown"
                        color = (0, 0, 220)

                    cv2.rectangle(
                        image, (x, y), (x + width, y + height), color, 2
                    )
                    cv2.putText(
                        image,
                        label,
                        (x, max(y - 10, 20)),
                        cv2.FONT_HERSHEY_SIMPLEX,
                        0.7,
                        color,
                        2,
                    )
                status = (
                    "Face detected." if len(faces) else "No face detected."
                )

            show_preview(
                "Face recognition",
                image,
                "Face recognition - camera {}".format(camera_index),
                "{} Auto-closes in {}s; press q to quit.".format(
                    status, max(0, int(remaining))
                ),
                warning,
            )
            if cv2.waitKey(10) & 0xFF == ord("q"):
                break
    finally:
        stop_reader.set()
        camera.release()
        reader.join(timeout=1)
        cv2.destroyAllWindows()


if __name__ == "__main__":
    recognize()
