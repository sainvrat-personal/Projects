from contextlib import closing
import sqlite3
from pathlib import Path

import cv2

from camera_utils import is_dark_frame, open_camera, show_preview


PROJECT_DIR = Path(__file__).resolve().parent
DATASET_DIR = PROJECT_DIR / "dataSet"
DATABASE_PATH = PROJECT_DIR / "FaceBase.db"
FACE_SIZE = (200, 200)


def insertOrUpdate(person_id, name):
    with closing(sqlite3.connect(DATABASE_PATH)) as connection:
        with connection:
            connection.execute(
                "CREATE TABLE IF NOT EXISTS People "
                "(ID INTEGER PRIMARY KEY, Name TEXT NOT NULL)"
            )
            connection.execute(
                "INSERT INTO People (ID, Name) VALUES (?, ?) "
                "ON CONFLICT(ID) DO UPDATE SET Name = excluded.Name",
                (person_id, name),
            )


def generateDataset(camera_index=0):
    raw_id = input("Enter a numeric user ID: ").strip()
    if not raw_id.isdecimal():
        raise ValueError("User ID must be a positive whole number.")
    person_id = int(raw_id)
    if person_id < 1:
        raise ValueError("User ID must be a positive whole number.")

    name = input("Enter the user's name: ").strip()
    if not name:
        raise ValueError("Name cannot be empty.")

    DATASET_DIR.mkdir(parents=True, exist_ok=True)
    insertOrUpdate(person_id, name)

    cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
    face_cascade = cv2.CascadeClassifier(cascade_path)
    if face_cascade.empty():
        raise RuntimeError("OpenCV could not load its bundled face-detection cascade.")

    camera = open_camera(camera_index)

    sample_number = 0
    try:
        print("Look at the camera. Capturing up to 30 face images; press q to stop.")
        while sample_number < 30:
            success, image = camera.read()
            if not success:
                raise RuntimeError(
                    "Camera {} opened but did not return a frame. Check camera "
                    "permissions, close other camera apps, and try again.".format(
                        camera_index
                    )
                )

            gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)
            warning = None
            if is_dark_frame(image):
                status = "Camera is returning a nearly black image."
                warning = (
                    "Check the camera lens/privacy shutter, Windows Camera app, and "
                    "camera permissions. You can choose another camera index when "
                    "starting the program."
                )
            else:
                faces = face_cascade.detectMultiScale(
                    cv2.equalizeHist(gray), scaleFactor=1.1, minNeighbors=4
                )
                if len(faces):
                    x, y, width, height = max(
                        faces, key=lambda face: face[2] * face[3]
                    )
                    face = cv2.resize(gray[y:y + height, x:x + width], FACE_SIZE)
                    sample_number += 1
                    image_path = DATASET_DIR / (
                        "User.{}.{}.jpg".format(person_id, sample_number)
                    )
                    if not cv2.imwrite(str(image_path), face):
                        raise OSError("Could not save face image: {}".format(image_path))
                    cv2.rectangle(
                        image, (x, y), (x + width, y + height), (0, 180, 0), 2
                    )
                    status = "Captured samples: {}/30".format(sample_number)
                else:
                    status = "No face detected. Face the camera and improve lighting."

            show_preview(
                "Dataset capture",
                image,
                "Face dataset - camera {}".format(camera_index),
                status + " Press q to cancel.",
                warning,
            )
            if cv2.waitKey(10) & 0xFF == ord("q"):
                break
    finally:
        camera.release()
        cv2.destroyAllWindows()

    if sample_number == 0:
        print(
            "No face images were captured. Check the white status panel in the "
            "camera preview, then retry with the working camera."
        )
        return False
    print("Saved {} face images for {}.".format(sample_number, name))
    return True


if __name__ == "__main__":
    generateDataset()
