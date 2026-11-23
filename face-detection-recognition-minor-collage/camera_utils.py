import textwrap

import cv2
import numpy as np


def open_camera(camera_index=0):
    camera = cv2.VideoCapture(camera_index, cv2.CAP_MSMF)
    if not camera.isOpened():
        camera.release()
        camera = cv2.VideoCapture(camera_index)
    if not camera.isOpened():
        camera.release()
        raise RuntimeError(
            "Could not open camera {}. Check that it is connected, not in use by "
            "another app, and allowed in Windows camera privacy settings.".format(
                camera_index
            )
        )
    return camera


def is_dark_frame(image):
    return float(np.mean(image)) < 12 and float(np.percentile(image, 95)) < 32


def show_preview(window_name, image, title, status, warning=None):
    top = 48
    bottom = 105 if warning else 72
    preview = cv2.copyMakeBorder(
        image,
        top,
        bottom,
        0,
        0,
        cv2.BORDER_CONSTANT,
        value=(255, 255, 255),
    )
    cv2.putText(
        preview,
        title,
        (12, 32),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.7,
        (25, 25, 25),
        2,
    )

    status_y = top + image.shape[0] + 30
    cv2.putText(
        preview,
        status,
        (12, status_y),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.55,
        (25, 25, 25),
        1,
    )
    if warning:
        for line_number, line in enumerate(textwrap.wrap(warning, width=82)[:2]):
            cv2.putText(
                preview,
                line,
                (12, status_y + 29 + line_number * 25),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.5,
                (0, 0, 190),
                1,
            )

    cv2.imshow(window_name, preview)
