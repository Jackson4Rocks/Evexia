#!/usr/bin/env python3
"""Render a 45-second 16:9 Evexia promo from the approved sample captures."""
from __future__ import annotations

import bisect
import json
import math
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageOps

ROOT = Path(__file__).resolve().parent.parent
SOURCE_DIR = ROOT / "SAMPLES" / "PROMO EXAMPLES"
RECORDING = SOURCE_DIR / "screen-20261023-222223.mp4"
AUDIO = ROOT / "SAMPLES" / "Infactuated.mp3"
FONT_DIR = ROOT / "app/src/main/res/font"
BEAT_FILE = ROOT / "promo/audio_beats.json"
APP_ICON = ROOT / "app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
OUTPUT = ROOT / "Evexia-promo-1920x1080.mp4"
STILLS_DIR = ROOT / "promo/landscape_stills"

WIDTH, HEIGHT = 1920, 1080
FPS = 30
DURATION = 45.0
BEAT_DROP = 8.2663
END_CARD_START = 42.2139
RECORDING_CUT = 24.7293
RECORDING_FRAMES = round(RECORDING_CUT * FPS)
SCREEN_SIZE = (450, 956)
DEVICE_SIZE = (506, 1010)
FRAME_COUNT = round(DURATION * FPS)

BG_TOP = (10, 12, 10)
BG_BOTTOM = (20, 22, 19)
CREAM = (244, 241, 232)
MUTED = (166, 167, 159)
DIM = (119, 122, 113)
CITRON = (216, 255, 99)
WHITE = (255, 255, 255)

SOURCES = {
    "student_id": "Screenshot_20261023-222014.png",
    "subjects": "Screenshot_20261023-222016.png",
    "timetable": "Screenshot_20261023-222018.png",
    "tasks": "Screenshot_20261023-222021.png",
    "flow_ai": "Screenshot_20261023-222023.png",
}

SCENE_TIMES = [0.0, BEAT_DROP, 15.1162, 23.3593, 30.2092, 38.429, END_CARD_START, DURATION]
SCENES = [
    {
        "questions": [
            "Still carrying your whole school day?",
            "Deadlines. Classes. Notes. All over the place?",
            "What if it could feel different?",
        ],
        "kicker": "THE QUIETER THE BETTER",
    },
    {
        "kicker": "ONE CALMER WAY",
        "lines": [("School.", False), ("In flow.", True)],
        "sub": "Meet the space that brings your day together.",
    },
    {
        "kicker": "MAKE ROOM TO THINK",
        "lines": [("Notes in.", False), ("Knowledge out.", True)],
        "sub": "Study tools that keep your learning moving.",
    },
    {
        "kicker": "YOUR DAY. YOUR RHYTHM.",
        "lines": [("Less to juggle.", False), ("More headspace.", True)],
        "sub": "Timetable, tasks and the little things—together.",
    },
    {
        "kicker": "MADE FOR YOUR WHOLE DAY",
        "lines": [("From your classes", False), ("to your next idea.", True)],
        "sub": "Subjects, timetables, tasks and study—all in one place.",
    },
    {
        "kicker": "YOUR MOVE",
        "lines": [("Find your", False), ("Evexia.", True)],
        "sub": "Free. Private. Offline-first.",
    },
    {"splash": True},
]

SCREENSHOT_CUTS = [
    (BEAT_DROP + RECORDING_CUT, "subjects"),
    (34.3191, "timetable"),
    (35.6891, "tasks"),
    (37.4073, "flow_ai"),
    (39.1256, "student_id"),
]

_fonts: dict[tuple[str, int], ImageFont.FreeTypeFont] = {}


def font(name: str, size: int) -> ImageFont.FreeTypeFont:
    key = (name, size)
    if key not in _fonts:
        _fonts[key] = ImageFont.truetype(str(FONT_DIR / name), size)
    return _fonts[key]


def clamp(value: float, low: float = 0.0, high: float = 1.0) -> float:
    return max(low, min(high, value))


def ease_out(value: float) -> float:
    value = clamp(value)
    return 1 - (1 - value) ** 3


def ease_in_out(value: float) -> float:
    value = clamp(value)
    return 4 * value**3 if value < 0.5 else 1 - ((-2 * value + 2) ** 3) / 2


def load_screenshots() -> dict[str, Image.Image]:
    images = {}
    for name, filename in SOURCES.items():
        path = SOURCE_DIR / filename
        if not path.is_file():
            raise FileNotFoundError(f"Missing approved promo screenshot: {path}")
        image = Image.open(path).convert("RGB")
        width, height = image.size
        image = image.crop((0, min(82, height // 12), width, height))
        images[name] = ImageOps.fit(
            image, SCREEN_SIZE, method=Image.Resampling.LANCZOS, centering=(0.5, 0.5)
        )
    if not RECORDING.is_file():
        raise FileNotFoundError(f"Missing approved screen recording: {RECORDING}")
    if not AUDIO.is_file():
        raise FileNotFoundError(f"Missing soundtrack: {AUDIO}")
    return images


def make_background() -> Image.Image:
    gradient = Image.new("RGBA", (1, HEIGHT))
    draw = ImageDraw.Draw(gradient)
    for y in range(HEIGHT):
        p = y / (HEIGHT - 1)
        color = tuple(round(BG_TOP[i] + (BG_BOTTOM[i] - BG_TOP[i]) * p) for i in range(3))
        draw.point((0, y), fill=(*color, 255))
    background = gradient.resize((WIDTH, HEIGHT), Image.Resampling.BILINEAR)

    glows = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glows)
    gd.ellipse((80, -300, 1030, 760), fill=(73, 82, 34, 92))
    gd.ellipse((1040, 100, 2010, 1240), fill=(57, 48, 92, 54))
    background.alpha_composite(glows.filter(ImageFilter.GaussianBlur(150)))

    return background


def make_device_shell() -> Image.Image:
    shell = Image.new("RGBA", DEVICE_SIZE, (0, 0, 0, 0))
    draw = ImageDraw.Draw(shell)
    draw.rounded_rectangle((0, 0, DEVICE_SIZE[0] - 1, DEVICE_SIZE[1] - 1), radius=48,
                           fill=(25, 27, 24, 255), outline=(255, 255, 255, 72), width=2)
    draw.rounded_rectangle((10, 10, DEVICE_SIZE[0] - 11, DEVICE_SIZE[1] - 11), radius=40,
                           outline=(0, 0, 0, 190), width=2)
    return shell


def make_device_shadow() -> Image.Image:
    shadow = Image.new("RGBA", (620, 1080), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((44, 38, 572, 1040), radius=76, fill=(0, 0, 0, 170))
    return shadow.filter(ImageFilter.GaussianBlur(34))


def make_device_glow() -> Image.Image:
    glow = Image.new("RGBA", (560, 770), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse((0, 0, 559, 769), fill=(*CITRON, 55))
    return glow.filter(ImageFilter.GaussianBlur(115))


def make_screen_mask() -> Image.Image:
    mask = Image.new("L", SCREEN_SIZE, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, SCREEN_SIZE[0] - 1, SCREEN_SIZE[1] - 1), radius=28, fill=255
    )
    return mask


def start_recording_decoder() -> subprocess.Popen:
    crop_height = 1530
    filter_chain = (
        f"crop=720:{crop_height}:0:66,"
        f"scale={SCREEN_SIZE[0]}:{SCREEN_SIZE[1]}:flags=lanczos,"
        f"fps={FPS},trim=end_frame={RECORDING_FRAMES},setpts=PTS-STARTPTS"
    )
    return subprocess.Popen(
        [
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-i", str(RECORDING),
            "-an", "-vf", filter_chain, "-f", "rawvideo", "-pix_fmt", "rgb24", "pipe:1",
        ],
        stdout=subprocess.PIPE,
    )


def load_app_icon() -> Image.Image:
    if not APP_ICON.is_file():
        raise FileNotFoundError(f"Missing app icon for the end card: {APP_ICON}")
    icon = Image.open(APP_ICON).convert("RGBA")
    return ImageOps.contain(icon, (240, 240), Image.Resampling.LANCZOS)


def screen_for_time(t: float, video_frame: Image.Image | None,
                    screenshots: dict[str, Image.Image]) -> tuple[str, Image.Image]:
    if t < BEAT_DROP:
        return "suspense", screenshots["student_id"]
    if t < BEAT_DROP + RECORDING_CUT:
        if video_frame is None:
            raise RuntimeError(f"Screen recording ended early at {t:.3f}s")
        return "recording", video_frame
    key = "student_id"
    for cut, image_key in SCREENSHOT_CUTS:
        if cut <= t:
            key = image_key
        else:
            break
    return key, screenshots[key]


def slide_screens(previous: Image.Image, current: Image.Image, progress: float) -> Image.Image:
    p = ease_in_out(progress)
    slide = Image.new("RGB", SCREEN_SIZE, (12, 14, 12))
    slide.paste(previous, (-round(p * SCREEN_SIZE[0]), 0))
    slide.paste(current, (round((1 - p) * SCREEN_SIZE[0]), 0))
    return slide


def wrap_lines(draw: ImageDraw.ImageDraw, text: str, f: ImageFont.FreeTypeFont,
               max_width: int) -> list[str]:
    lines: list[str] = []
    line = ""
    for word in text.split():
        candidate = word if not line else f"{line} {word}"
        if line and draw.textlength(candidate, font=f) > max_width:
            lines.append(line)
            line = word
        else:
            line = candidate
    if line:
        lines.append(line)
    return lines


PHONE_POSES = [
    (BEAT_DROP, 960, 750, 0.62, -2.0),
    (9.05, 1480, 520, 1.00, 2.0),
    (11.35, 1420, 620, 0.96, -1.0),
    (15.1162, 960, 750, 0.62, 1.8),
    (17.25, 440, 535, 0.99, -2.2),
    (20.35, 970, 680, 0.76, 2.1),
    (23.3593, 1490, 520, 1.00, -1.7),
    (26.3, 1440, 630, 0.96, 1.8),
    (28.4, 460, 500, 0.99, -2.0),
    (30.2092, 960, 750, 0.62, 2.0),
    (32.5, 1460, 520, 1.00, -1.8),
    (33.0, 460, 520, 0.99, 2.0),
    (34.3191, 1480, 465, 0.93, -2.0),
    (35.6891, 960, 750, 0.62, 2.0),
    (37.4073, 450, 540, 0.98, -1.8),
    (39.1256, 1470, 520, 0.98, 1.8),
    (40.4, 960, 750, 0.62, 0.0),
    (END_CARD_START, 960, 540, 0.01, 0.0),
]


def phone_pose(t: float) -> tuple[float, float, float, float]:
    if t <= PHONE_POSES[0][0]:
        return PHONE_POSES[0][1:]
    for left, right in zip(PHONE_POSES, PHONE_POSES[1:]):
        if t <= right[0]:
            progress = ease_in_out((t - left[0]) / max(0.001, right[0] - left[0]))
            return tuple(left[i] + (right[i] - left[i]) * progress for i in range(1, 5))
    return PHONE_POSES[-1][1:]


def draw_scene(frame: Image.Image, t: float, index: int) -> None:
    start, end = SCENE_TIMES[index], SCENE_TIMES[index + 1]
    if index == 0 or index == len(SCENES) - 1:
        return
    if t < start - 0.3 or t > end:
        return
    local = t - start
    enter = ease_out((local - 0.04) / 0.52)
    exit_amount = ease_in_out((t - (end - 0.38)) / 0.38)
    alpha = clamp(enter * (1 - exit_amount))
    if alpha <= 0.002:
        return

    phone_x, _, _, _ = phone_pose(t)
    middle = WIDTH * 0.39 < phone_x < WIDTH * 0.61
    on_right = phone_x < WIDTH * 0.39
    direction = -1 if index % 2 == 0 else 1
    dx = round(direction * (1 - enter) * 115 - direction * exit_amount * 75)
    dy = round((1 - enter) * 30)
    layer = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    scene = SCENES[index]

    if middle:
        anchor_x, anchor = WIDTH // 2 + dx, "ma"
        kicker_y, baseline, subtitle_y = 90 + dy, 172 + dy, 366 + dy
        draw.line((anchor_x - 220, kicker_y + 11, anchor_x - 184, kicker_y + 11),
                  fill=(*CITRON, int(230 * alpha)), width=2)
        draw.text((anchor_x, kicker_y), scene["kicker"], font=font("instrument_sans.ttf", 17),
                  fill=(*CITRON, int(238 * alpha)), anchor="ma")
    else:
        anchor = "ra" if on_right else "la"
        anchor_x = WIDTH - 125 + dx if on_right else 130 + dx
        kicker_y, baseline, subtitle_y = 204 + dy, 396 + dy, 630 + dy
        if on_right:
            draw.line((anchor_x - 34, kicker_y + 11, anchor_x, kicker_y + 11),
                      fill=(*CITRON, int(230 * alpha)), width=2)
            kicker_anchor_x = anchor_x - 48
            kicker_anchor = "ra"
        else:
            draw.line((anchor_x, kicker_y + 11, anchor_x + 34, kicker_y + 11),
                      fill=(*CITRON, int(230 * alpha)), width=2)
            kicker_anchor_x = anchor_x + 48
            kicker_anchor = "la"
        draw.text((kicker_anchor_x, kicker_y), scene["kicker"], font=font("instrument_sans.ttf", 19),
                  fill=(*CITRON, int(238 * alpha)), anchor=kicker_anchor)

    for line_index, (text, italic) in enumerate(scene["lines"]):
        face = "instrument_serif_italic.ttf" if italic else "instrument_serif.ttf"
        size = 68 if on_right else 78
        if middle:
            size = 62
        line_baseline = baseline + line_index * (82 if middle else 104)
        color = CITRON if italic else CREAM
        title_font = font(face, size)
        full_width = draw.textlength(text, font=title_font)
        visible = clamp((local - 0.16 - line_index * 0.23) / 0.38)
        visible = ease_out(visible)
        draw.text((anchor_x, line_baseline), text, font=title_font,
                  fill=(*color, int(255 * alpha * visible)), anchor="ms" if middle else anchor)
        if visible < 0.99:
            if middle:
                line_start = anchor_x - full_width / 2
                line_end = line_start + full_width * visible
            elif on_right:
                line_start = anchor_x - full_width
                line_end = line_start + full_width * visible
            else:
                line_start = anchor_x
                line_end = line_start + full_width * visible
            draw.line((line_start, line_baseline + 12, line_end, line_baseline + 12),
                      fill=(*CITRON, int(210 * alpha)), width=2)

    subtitle_font = font("instrument_sans.ttf", 28)
    lines = wrap_lines(draw, scene["sub"], subtitle_font, 800 if middle else 980)
    for line_index, line in enumerate(lines[:2]):
        subtitle_enter = ease_out((local - 0.65 - line_index * 0.08) / 0.32)
        draw.text((anchor_x, subtitle_y + line_index * 40), line, font=subtitle_font,
                  fill=(*MUTED, int(235 * alpha * subtitle_enter)),
                  anchor="ma" if middle else anchor)
    frame.alpha_composite(layer)


def draw_suspense(frame: Image.Image, t: float, beats: list[float]) -> None:
    if t >= BEAT_DROP:
        return
    questions = SCENES[0]["questions"]
    intervals = [(0.15, 2.25), (2.35, 4.55), (4.65, 6.55), (6.62, BEAT_DROP)]
    captions = questions + ["Ready for a calmer way?"]
    idx = next((i for i, (begin, finish) in enumerate(intervals) if begin <= t < finish), 0)
    start, finish = intervals[idx]
    local = t - start
    enter = ease_out((local - 0.04) / 0.44)
    leave = ease_in_out((t - (finish - 0.32)) / 0.32)
    alpha = clamp(enter * (1 - leave))
    if alpha < 0.002:
        return

    layer = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    title = captions[idx]
    size = 64 if idx in (1, 2) else 70
    title_font = font("instrument_serif_italic.ttf" if idx in (2, 3) else "instrument_serif.ttf", size)
    x = 180 + round((1 - enter) * -120)
    y = 510 + round((1 - enter) * 28)
    draw.text((x, y), title, font=title_font, fill=(*CREAM, int(255 * alpha)), anchor="lm")

    line_width = min(600, 80 + local * 250)
    draw.rounded_rectangle((180, 577, 180 + line_width, 580), radius=2,
                           fill=(*CITRON, int(190 * alpha)))
    draw.text((184, 633), "EVEXIA  /  A CALMER WAY TO STUDY",
              font=font("instrument_sans.ttf", 15), fill=(*MUTED, int(210 * alpha)))

    # Restrained floating light motes give the quiet opening a sense of movement.
    for mote in range(12):
        phase = (t * (0.06 + mote * 0.002) + mote * 0.071) % 1.0
        mx = int((mote * 139 + 240 + t * (18 + mote % 5 * 7)) % WIDTH)
        my = int(180 + phase * 640)
        radius = 1 + mote % 3
        draw.ellipse((mx - radius, my - radius, mx + radius, my + radius),
                     fill=(*CITRON, int((20 + 26 * math.sin(phase * math.pi)) * alpha)))
    frame.alpha_composite(layer)


def draw_drop_flash(frame: Image.Image, t: float) -> None:
    delta = t - BEAT_DROP
    if 0 <= delta < 0.32:
        strength = (1 - delta / 0.32) ** 2
        flash = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
        ImageDraw.Draw(flash).ellipse((1160, 130, 1840, 1010),
                                      fill=(*CITRON, int(84 * strength)))
        frame.alpha_composite(flash.filter(ImageFilter.GaussianBlur(105)))
        if delta < 0.10:
            frame.alpha_composite(Image.new("RGBA", (WIDTH, HEIGHT), (255, 250, 224, int(72 * (1 - delta / 0.10)))))
        return
    cut_times = [*SCENE_TIMES[2:-1], *(cut for cut, _ in SCREENSHOT_CUTS)]
    for cut in cut_times:
        delta = t - cut
        if 0 <= delta < 0.095:
            alpha = round(28 * (1 - delta / 0.095))
            frame.alpha_composite(Image.new("RGBA", (WIDTH, HEIGHT), (216, 255, 99, alpha)))
            break


def draw_end_card(frame: Image.Image, t: float, app_icon: Image.Image, beats: list[float]) -> None:
    local = t - END_CARD_START
    intro = ease_out(local / 0.48)
    pulse = beat_pulse(t, beats)

    # Pull the background into a clean, centered end card.
    overlay = Image.new("RGBA", (WIDTH, HEIGHT), (4, 5, 4, 224))
    frame.alpha_composite(overlay)
    draw = ImageDraw.Draw(frame)
    radius = int(208 * intro * (1 + pulse * 0.014))
    draw.ellipse((WIDTH // 2 - radius, 500 - radius, WIDTH // 2 + radius, 500 + radius),
                 outline=(216, 255, 99, int(15 + 34 * intro)), width=2)

    icon_size = round(210 * (0.72 + 0.28 * intro) * (1 + pulse * 0.025))
    icon = app_icon.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
    icon_alpha = icon.getchannel("A").point(lambda value: int(value * intro))
    icon.putalpha(icon_alpha)
    x, y = (WIDTH - icon_size) // 2, int(280 + (1 - intro) * 80)
    frame.alpha_composite(icon, (x, y))

    title_font = font("instrument_serif.ttf", 92)
    title = "Evexia"
    title_alpha = int(255 * ease_out((local - 0.24) / 0.42))
    draw.text((WIDTH // 2, 575), title, font=title_font, fill=(*CREAM, title_alpha), anchor="mm")

    tag_alpha = int(220 * ease_out((local - 0.58) / 0.38))
    tagline = "Free. Private. Offline-first."
    f = font("instrument_sans.ttf", 21)
    draw.text((WIDTH // 2, 666), tagline, font=f, fill=(*MUTED, tag_alpha), anchor="mm")
    if local > 0.75:
        line_progress = ease_out((local - 0.75) / 0.55)
        half_width = int(190 * line_progress)
        draw.line((WIDTH // 2 - half_width, 710, WIDTH // 2 + half_width, 710),
                  fill=(*CITRON, int(180 * intro)), width=2)


def beat_pulse(t: float, beats: list[float]) -> float:
    index = bisect.bisect_right(beats, t) - 1
    if index < 0:
        return 0.0
    elapsed = t - beats[index]
    return math.exp(-elapsed * 9.0) if elapsed < 0.42 else 0.0


def render_frame(t: float, source_screen: Image.Image, key: str, previous_screen: Image.Image,
                 transition_start: float, beats: list[float], background: Image.Image,
                 shell: Image.Image, shadow: Image.Image, glow: Image.Image,
                 screen_mask: Image.Image, app_icon: Image.Image) -> Image.Image:
    frame = background.copy()
    pulse = beat_pulse(t, beats)
    if t >= END_CARD_START:
        draw_end_card(frame, t, app_icon, beats)
        return frame.convert("RGB")
    draw_suspense(frame, t, beats)
    if t < BEAT_DROP:
        return frame.convert("RGB")

    draw_drop_flash(frame, t)
    drop_age = t - BEAT_DROP
    enter = ease_out(drop_age / 0.52)
    x_center, y_center, pose_scale, angle = phone_pose(t)
    x_center += math.sin(t * 0.7) * 10
    y_center += math.sin(t * 0.82) * 8 - 10 * (1 - enter)
    scale = pose_scale * (0.88 + 0.12 * enter) * (1 + pulse * 0.014)
    glow_layer = glow.copy()
    glow_layer.putalpha(glow_layer.getchannel("A").point(lambda value: int(value * (0.34 + pulse * 0.56))))
    frame.alpha_composite(glow_layer, (round(x_center - 280), round(y_center - 385)))

    scaled_shadow = shadow.resize((round(shadow.width * scale), round(shadow.height * scale)),
                                  Image.Resampling.BILINEAR)
    frame.alpha_composite(scaled_shadow, (round(x_center - scaled_shadow.width / 2 - 12),
                                          round(y_center - scaled_shadow.height / 2 + 30)))
    screen = source_screen
    if t - transition_start < 0.42:
        screen = slide_screens(previous_screen, source_screen, (t - transition_start) / 0.42)

    screen_layer = screen.convert("RGBA")
    screen_layer.putalpha(screen_mask)
    device = shell.copy()
    device.alpha_composite(screen_layer, (24, 24))
    draw_device = ImageDraw.Draw(device)
    draw_device.rounded_rectangle((23, 23, DEVICE_SIZE[0] - 24, DEVICE_SIZE[1] - 24),
                                  radius=30, outline=(255, 255, 255, 40), width=1)
    scaled_device = device.resize((round(DEVICE_SIZE[0] * scale), round(DEVICE_SIZE[1] * scale)),
                                  Image.Resampling.LANCZOS)
    scaled_device = scaled_device.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    frame.alpha_composite(scaled_device, (round(x_center - scaled_device.width / 2),
                                          round(y_center - scaled_device.height / 2)))

    for index in range(1, len(SCENES) - 1):
        draw_scene(frame, t, index)

    draw = ImageDraw.Draw(frame)
    draw.rectangle((0, 0, WIDTH, 25), fill=(3, 4, 3, 255))
    draw.rectangle((0, HEIGHT - 25, WIDTH, HEIGHT), fill=(3, 4, 3, 255))
    return frame.convert("RGB")


def load_beats() -> list[float]:
    return json.loads(BEAT_FILE.read_text())["beats"]


def render_video(screenshots: dict[str, Image.Image], beats: list[float]) -> None:
    decoder = start_recording_decoder()
    if decoder.stdout is None:
        raise RuntimeError("Could not open the screen-recording decoder")
    encoder = subprocess.Popen(
        [
            "ffmpeg", "-hide_banner", "-loglevel", "error", "-y",
            "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{WIDTH}x{HEIGHT}",
            "-r", str(FPS), "-i", "-", "-i", str(AUDIO),
            "-map", "0:v:0", "-map", "1:a:0", "-t", f"{DURATION:.3f}",
            "-af", "afade=t=in:st=0:d=0.18,afade=t=out:st=43.05:d=1.95,volume=0.92",
            "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-pix_fmt", "yuv420p",
            "-c:a", "aac", "-b:a", "192k", "-ar", "44100", "-shortest",
            "-movflags", "+faststart", str(OUTPUT),
        ],
        stdin=subprocess.PIPE,
    )
    if encoder.stdin is None:
        raise RuntimeError("Could not open the video encoder")

    background = make_background()
    shell = make_device_shell()
    shadow = make_device_shadow()
    glow = make_device_glow()
    screen_mask = make_screen_mask()
    app_icon = load_app_icon()
    last_screen = screenshots["student_id"]
    active_key = ""
    transition_start = -1.0
    frame_size = SCREEN_SIZE[0] * SCREEN_SIZE[1] * 3
    try:
        for index in range(FRAME_COUNT):
            t = index / FPS
            video_frame = None
            recording_t = t - BEAT_DROP
            if 0 <= recording_t < RECORDING_CUT:
                raw = decoder.stdout.read(frame_size)
                if len(raw) != frame_size:
                    raise RuntimeError(f"Screen recording ended early at frame {index}")
                video_frame = Image.frombytes("RGB", SCREEN_SIZE, raw)
            key, screen = screen_for_time(t, video_frame, screenshots)
            if key != active_key:
                last_screen = last_screen.copy()
                active_key = key
                transition_start = t - 0.42
            frame = render_frame(t, screen, key, last_screen, transition_start,
                                 beats, background, shell, shadow, glow, screen_mask, app_icon)
            encoder.stdin.write(frame.tobytes())
            last_screen = screen
            if index % (FPS * 5) == 0:
                print(f"Rendering {index}/{FRAME_COUNT} frames ({t:05.1f}s)", flush=True)
    finally:
        encoder.stdin.close()
        decoder.stdout.close()
    encode_status = encoder.wait()
    decode_status = decoder.wait()
    if encode_status:
        raise RuntimeError(f"ffmpeg video encoding failed with exit code {encode_status}")
    if decode_status:
        raise RuntimeError(f"ffmpeg screen decoding failed with exit code {decode_status}")
    print(f"Video: {OUTPUT}")


def render_stills(screenshots: dict[str, Image.Image], beats: list[float]) -> None:
    STILLS_DIR.mkdir(parents=True, exist_ok=True)
    times = [2.1, 7.7, 16.4, 25.1, 33.3, 40.2, 43.3]
    background = make_background()
    shell = make_device_shell()
    shadow = make_device_shadow()
    glow = make_device_glow()
    screen_mask = make_screen_mask()
    app_icon = load_app_icon()
    decoder = start_recording_decoder()
    if decoder.stdout is None:
        raise RuntimeError("Could not open the screen-recording decoder")
    frame_size = SCREEN_SIZE[0] * SCREEN_SIZE[1] * 3
    frame_index = -1
    current_video_frame = None
    previous_screen = screenshots["student_id"]
    active_key = ""
    transition_start = 0.0
    try:
        for still_index, t in enumerate(times):
            target_index = min(int(min(max(t - BEAT_DROP, 0), RECORDING_CUT) * FPS), RECORDING_FRAMES - 1)
            while frame_index < target_index:
                raw = decoder.stdout.read(frame_size)
                if len(raw) != frame_size:
                    raise RuntimeError(f"Screen recording ended early at frame {frame_index + 1}")
                frame_index += 1
                current_video_frame = Image.frombytes("RGB", SCREEN_SIZE, raw)
            video_frame = current_video_frame if BEAT_DROP <= t < BEAT_DROP + RECORDING_CUT else None
            key, screen = screen_for_time(t, video_frame, screenshots)
            if key != active_key:
                previous_screen = previous_screen.copy()
                active_key = key
                transition_start = t - 0.42
            frame = render_frame(t, screen, key, previous_screen, transition_start,
                                 beats, background, shell, shadow, glow, screen_mask, app_icon)
            frame.save(STILLS_DIR / f"landscape_{still_index:02d}_{t:.1f}s.png")
            previous_screen = screen
    finally:
        decoder.stdout.close()
    status = decoder.wait()
    if status:
        raise RuntimeError(f"ffmpeg screen decoding failed with exit code {status}")


def main() -> None:
    screenshots = load_screenshots()
    beats = load_beats()
    if "--video" in sys.argv or len(sys.argv) == 1:
        render_video(screenshots, beats)
    else:
        render_stills(screenshots, beats)


if __name__ == "__main__":
    main()
