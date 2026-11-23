import re
from pathlib import Path

import cv2
import numpy as np


PROJECT_DIR = Path(__file__).resolve().parent
DATASET_DIR = PROJECT_DIR / "dataSet"
MODEL_DIR = PROJECT_DIR / "trainner"
MODEL_PATH = MODEL_DIR / "trainningData.yml"
FACE_SIZE = (200, 200)


def train():
    if not hasattr(cv2, "face") or not hasattr(cv2.face, "LBPHFaceRecognizer_create"):
        raise RuntimeError(
            "LBPH face recognition is unavailable. Install the opencv-contrib-python "
            "package in the active environment."
        )
    if not DATASET_DIR.is_dir():
        raise FileNotFoundError(
            "No dataset directory found. Capture face images first with "
            "'python dataSetGenerator.py'."
        )

    images = []
    person_ids = []
    for image_path in sorted(DATASET_DIR.glob("User.*.*.jpg")):
        match = re.fullmatch(r"User\.(\d+)\.\d+\.jpg", image_path.name)
        if match is None:
            continue
        image = cv2.imread(str(image_path), cv2.IMREAD_GRAYSCALE)
        if image is None:
            raise RuntimeError("Could not read dataset image: {}".format(image_path))
        images.append(cv2.resize(image, FACE_SIZE))
        person_ids.append(int(match.group(1)))

    if not images:
        raise RuntimeError(
            "No usable face images found. Capture images first with "
            "'python dataSetGenerator.py'."
        )

    recognizer = cv2.face.LBPHFaceRecognizer_create()
    recognizer.train(images, np.asarray(person_ids, dtype=np.int32))
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    recognizer.save(str(MODEL_PATH))
    print("Trained on {} images. Model saved to {}.".format(len(images), MODEL_PATH))


if __name__ == "__main__":
    train()
