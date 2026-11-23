import cv2

from camera_utils import is_dark_frame, open_camera, show_preview


def faceDetect(camera_index=0):
    cascade_path = cv2.data.haarcascades + "haarcascade_frontalface_default.xml"
    face_cascade = cv2.CascadeClassifier(cascade_path)
    if face_cascade.empty():
        raise RuntimeError("OpenCV could not load its bundled face-detection cascade.")

    camera = open_camera(camera_index)

    try:
        samples = 0
        while samples < 20:
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
                warning = (
                    "The camera is returning a nearly black image. Open the Windows "
                    "Camera app to check the lens/privacy shutter, select the working "
                    "camera, and check Windows camera permissions."
                )
                status = "No usable camera image; face detection is paused."
            else:
                faces = face_cascade.detectMultiScale(
                    cv2.equalizeHist(gray), scaleFactor=1.1, minNeighbors=4
                )
                for x, y, width, height in faces:
                    cv2.rectangle(
                        image, (x, y), (x + width, y + height), (0, 255, 0), 2
                    )
                    samples += 1
                status = (
                    "Face detected. Press q to continue."
                    if len(faces)
                    else "Face the camera. Press q to continue."
                )

            show_preview(
                "Face detection",
                image,
                "Face detection - camera {}".format(camera_index),
                status,
                warning,
            )
            if cv2.waitKey(10) & 0xFF == ord("q"):
                break
    finally:
        camera.release()
        cv2.destroyAllWindows()
    return samples > 0


if __name__ == "__main__":
    faceDetect()
