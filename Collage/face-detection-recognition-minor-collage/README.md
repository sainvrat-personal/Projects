# Face Detection and Recognition

A webcam-based face detection and recognition demo written in Python. It captures face images for a person, trains an OpenCV LBPH face recognizer, and labels matching faces from the webcam. Face images, the SQLite database, and the trained model are stored locally in this project folder.

> This is a learning/demo project, not a secure authentication system. Face recognition can be inaccurate; do not use it to control access to devices, accounts, or sensitive resources. Get permission before capturing or storing anyone's face images.

## Requirements

- Windows 10 or 11
- Python 3.10 or later (tested setup target: Python 3.13)
- A working webcam

The face detector's Haar cascade is included with OpenCV; no separate `Face.xml` download is required. SQLite is included with Python.

## Set up on Windows

Open PowerShell in this project folder and run:

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
python -m pip install -r requirements.txt
```

If PowerShell prevents environment activation, either allow it for the current terminal with `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` and activate again, or skip activation and use the environment's Python directly. The commands below work even if you have not activated the environment:

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
```

## Run

Run the complete workflow using the project environment's Python:

```powershell
.\.venv\Scripts\python.exe main.py
```

Using `python main.py` is also fine after activating `.venv`. If you run it without activating the environment, PowerShell may select a different Python installation that does not have the project dependencies.

The workflow first asks for a camera index; press Enter to use camera `0`, or enter another index if your PC has more than one webcam. Its preview has a white title/status panel around the live camera image. If the image is nearly black, the panel explains what to check. The program does not replace the camera image with a white image, because face detection needs the real camera picture.

After the camera check, the workflow asks for a numeric user ID and name, captures up to 30 face samples, and trains the recognizer. **Step 4 (face recognition) is the final step:** it runs for up to two minutes and then closes automatically. Press **q** in the recognition preview to quit sooner. The time limit still works if the webcam stops supplying frames.

You can also run individual stages from PowerShell using the same environment:

```powershell
.\.venv\Scripts\python.exe detect.py
.\.venv\Scripts\python.exe dataSetGenerator.py
.\.venv\Scripts\python.exe trainner.py
.\.venv\Scripts\python.exe Recognizer.py
```

Run `dataSetGenerator.py` once for each person you want to add, using a distinct numeric ID. After collecting samples for all people, run `trainner.py`, then `Recognizer.py`. If you add or recapture a person's images later, train the model again.

## Files created at runtime

- `dataSet/` — captured, resized face images
- `FaceBase.db` — local SQLite database of numeric IDs and names
- `trainner/trainningData.yml` — trained LBPH recognition model

These are generated locally and are excluded from version control.

## Troubleshooting

- **Webcam cannot be opened or the preview is nearly black:** first test the camera in the Windows **Camera** app. Check for a physical privacy shutter or lens cover, close other apps using the camera, and enable **Settings > Privacy & security > Camera > Let desktop apps access your camera**. When the program starts, try another camera index if your PC has multiple webcams. A white status panel around a black picture does not mean the live video is white; it reports the camera image separately.
- **No face samples captured:** follow the webcam preview's on-screen guidance: face the camera, improve lighting, move closer, and keep your face visible while capture runs. If you cancel with **q** before any face is detected, run `python dataSetGenerator.py` again.
- **`ModuleNotFoundError: No module named 'cv2'`:** run `.\.venv\Scripts\python.exe main.py` so the project environment is used. Do not run `pip install cv2`: `cv2` is the import name, not a pip package. The package is `opencv-contrib-python`; if needed, install it with `.\.venv\Scripts\python.exe -m pip install -r requirements.txt`.
- **LBPH face recognizer unavailable:** make sure you use the project's virtual environment's Python and install packages with `.\.venv\Scripts\python.exe -m pip install -r requirements.txt`. This project requires `opencv-contrib-python`, not `opencv-python`.
- **PowerShell says `python` is not recognized:** activate `.venv` as shown above, or use `.\.venv\Scripts\python.exe` in place of `python`.
