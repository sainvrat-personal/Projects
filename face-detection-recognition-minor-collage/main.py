import dataSetGenerator
import detect
import Recognizer
import trainner


def choose_camera_index():
    raw_index = input("Camera index [0]: ").strip()
    if not raw_index:
        return 0
    if not raw_index.isdecimal():
        raise ValueError("Camera index must be a non-negative whole number.")
    return int(raw_index)


def main():
    camera_index = choose_camera_index()

    print("Step 1: Face detection")
    if not detect.faceDetect(camera_index):
        print(
            "No face was detected. Check the camera preview's white status panel, "
            "then run the program again with the correct camera index."
        )
        return

    print("Step 2: Create a face dataset")
    if not dataSetGenerator.generateDataset(camera_index):
        return

    print("Step 3: Train the face recognizer")
    trainner.train()

    print("Step 4: Recognize faces")
    Recognizer.recognize(camera_index)


if __name__ == "__main__":
    main()
