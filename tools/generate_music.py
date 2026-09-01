"""Gera as camadas de musica adaptativa de Echoes em OGG Vorbis.

Todas as frequencias e LFOs sao multiplos exatos do fundamental do loop
(1 / LOOP_SECONDS). Isso torna cada arquivo perfeitamente ciclico: o ultimo
sample encosta no primeiro sem clique, condicao obrigatoria para as camadas
tocarem sincronizadas e trocarem por crossfade de volume.
"""
from __future__ import annotations

import argparse
from pathlib import Path

import numpy as np
import soundfile as sf

SAMPLE_RATE = 44100
LOOP_SECONDS = 32.0
BPM = 90.0
OUTPUT_DIR = Path("assets/music")

_FRAMES = int(SAMPLE_RATE * LOOP_SECONDS)
_TIME = np.arange(_FRAMES) / SAMPLE_RATE
_FUNDAMENTAL = 1.0 / LOOP_SECONDS


def _quantize(frequency: float) -> float:
    """Aproxima a frequencia ao multiplo mais proximo do fundamental do loop."""
    return max(1.0, round(frequency / _FUNDAMENTAL)) * _FUNDAMENTAL


def _sine(frequency: float, amplitude: float = 1.0, phase: float = 0.0) -> np.ndarray:
    return amplitude * np.sin(2 * np.pi * _quantize(frequency) * _TIME + phase)


def _lfo(frequency: float, depth: float, offset: float, phase: float = 0.0) -> np.ndarray:
    return offset + depth * np.sin(2 * np.pi * _quantize(frequency) * _TIME + phase)


def _pad(frequency: float, amplitude: float, partials: int = 5,
         detune: float = 0.004, seed: int = 0) -> np.ndarray:
    """Pad harmonico com parciais levemente desafinados e decaimento suave."""
    rng = np.random.default_rng(seed)
    voice = np.zeros(_FRAMES)
    for index in range(1, partials + 1):
        weight = 1.0 / (index ** 1.6)
        for offset in (-detune, detune):
            voice += weight * _sine(frequency * index * (1 + offset),
                                    phase=rng.uniform(0, 2 * np.pi))
    return amplitude * voice / (partials * 2)


def _periodic_noise(low_hz: float, high_hz: float, amplitude: float,
                    seed: int, tilt: float = 1.0) -> np.ndarray:
    """Ruido colorido construido no dominio da frequencia, logo ciclico por definicao."""
    rng = np.random.default_rng(seed)
    spectrum = np.zeros(_FRAMES // 2 + 1, dtype=complex)
    bins = np.arange(spectrum.size) * _FUNDAMENTAL
    band = (bins >= low_hz) & (bins <= high_hz)
    magnitude = np.zeros_like(bins)
    magnitude[band] = (bins[band] / max(low_hz, 1.0)) ** (-tilt)
    phases = rng.uniform(0, 2 * np.pi, spectrum.size)
    spectrum[band] = magnitude[band] * np.exp(1j * phases[band])
    signal = np.fft.irfft(spectrum, n=_FRAMES)
    peak = np.max(np.abs(signal))
    return amplitude * signal / peak if peak > 0 else signal


def _pulse_envelope(divisions: int, attack: float, decay: float,
                    swing: float = 0.0) -> np.ndarray:
    """Envelope percussivo repetido `divisions` vezes dentro do loop."""
    envelope = np.zeros(_FRAMES)
    step = _FRAMES / divisions
    for index in range(divisions):
        start = int(index * step + (swing * step if index % 2 else 0.0))
        attack_frames = max(1, int(attack * SAMPLE_RATE))
        decay_frames = max(1, int(decay * SAMPLE_RATE))
        rise = np.linspace(0.0, 1.0, attack_frames)
        fall = np.exp(-np.linspace(0.0, 6.0, decay_frames))
        shape = np.concatenate([rise, fall])
        end = min(_FRAMES, start + shape.size)
        envelope[start:end] = np.maximum(envelope[start:end], shape[: end - start])
    return envelope


def _stereo(left: np.ndarray, right: np.ndarray, peak: float) -> np.ndarray:
    stack = np.stack([left, right], axis=1)
    highest = np.max(np.abs(stack))
    if highest > 0:
        stack = stack * (peak / highest)
    return stack.astype(np.float32)


# ==========================================================
# LUA — vazio, frio, sem atmosfera
# ==========================================================

def lunar_base() -> np.ndarray:
    breath = _lfo(1 / 16.0, 0.35, 0.65)
    voice = (_pad(55.0, 0.55, partials=3, seed=1)
             + _pad(110.0, 0.40, seed=2) * breath
             + _pad(164.81, 0.26, seed=3) * _lfo(1 / 10.66, 0.30, 0.70, np.pi / 3)
             + _pad(261.63, 0.18, seed=4) * _lfo(1 / 8.0, 0.35, 0.55, np.pi / 2))
    shimmer = _periodic_noise(2000, 9000, 0.05, seed=11, tilt=1.4) * _lfo(1 / 32.0, 0.5, 0.5)
    left = voice + shimmer
    right = voice * 0.96 + np.roll(shimmer, 311)
    return _stereo(left, right, 0.42)


def lunar_tension() -> np.ndarray:
    beat = _pulse_envelope(int(LOOP_SECONDS * BPM / 60.0), 0.004, 0.34)
    heart = (_sine(82.41, 0.9) + _sine(41.20, 0.6)) * beat
    unease = (_pad(233.08, 0.30, partials=3, seed=21)
              + _pad(246.94, 0.24, partials=3, seed=22))
    unease *= _lfo(1 / 5.33, 0.45, 0.55)
    left = heart + unease
    right = heart * 0.94 + np.roll(unease, 673)
    return _stereo(left, right, 0.34)


def lunar_urgency() -> np.ndarray:
    arpeggio = np.zeros(_FRAMES)
    notes = [440.00, 523.25, 659.25, 523.25]
    steps = int(LOOP_SECONDS * BPM / 60.0) * 2
    envelope = _pulse_envelope(steps, 0.002, 0.16)
    for index, note in enumerate(notes):
        mask = np.zeros(_FRAMES)
        step = _FRAMES / steps
        for slot in range(index, steps, len(notes)):
            start, end = int(slot * step), int((slot + 1) * step)
            mask[start:end] = 1.0
        arpeggio += _sine(note, 0.6) * mask
    arpeggio *= envelope
    alarm = _sine(880.0, 0.22) * _lfo(2.0, 0.5, 0.5) * _lfo(1 / 4.0, 0.4, 0.6)
    left = arpeggio + alarm
    right = np.roll(arpeggio, 421) + alarm * 0.9
    return _stereo(left, right, 0.33)


# ==========================================================
# MARTE — poeira, vento, horizonte largo
# ==========================================================

def mars_base() -> np.ndarray:
    wind = _periodic_noise(120, 2600, 0.30, seed=31, tilt=0.9)
    wind *= _lfo(1 / 32.0, 0.35, 0.55) * _lfo(1 / 21.33, 0.25, 0.75, np.pi / 4)
    voice = (_pad(73.42, 0.50, partials=3, seed=32)
             + _pad(146.83, 0.34, seed=33) * _lfo(1 / 12.8, 0.30, 0.70)
             + _pad(220.00, 0.20, seed=34) * _lfo(1 / 9.14, 0.35, 0.55, np.pi / 5))
    left = voice + wind
    right = voice * 0.95 + np.roll(wind, 907)
    return _stereo(left, right, 0.42)


def mars_tension() -> np.ndarray:
    beat = _pulse_envelope(int(LOOP_SECONDS * BPM / 60.0), 0.003, 0.28, swing=0.06)
    drum = (_sine(61.74, 0.9) + _periodic_noise(200, 1800, 0.25, seed=41)) * beat
    drone = _pad(92.50, 0.32, partials=4, seed=42) * _lfo(1 / 6.4, 0.40, 0.60)
    left = drum + drone
    right = drum * 0.92 + np.roll(drone, 557)
    return _stereo(left, right, 0.34)


def mars_urgency() -> np.ndarray:
    steps = int(LOOP_SECONDS * BPM / 60.0) * 2
    envelope = _pulse_envelope(steps, 0.002, 0.14)
    riff = np.zeros(_FRAMES)
    for index, note in enumerate([293.66, 349.23, 440.00, 349.23]):
        mask = np.zeros(_FRAMES)
        step = _FRAMES / steps
        for slot in range(index, steps, 4):
            start, end = int(slot * step), int((slot + 1) * step)
            mask[start:end] = 1.0
        riff += (_sine(note, 0.5) + _sine(note * 2, 0.15)) * mask
    riff *= envelope
    siren = _sine(660.0, 0.18) * _lfo(1.5, 0.5, 0.5)
    left = riff + siren
    right = np.roll(riff, 389) + siren * 0.88
    return _stereo(left, right, 0.33)


LAYERS = {
    "lunar_base": lunar_base,
    "lunar_tension": lunar_tension,
    "lunar_urgency": lunar_urgency,
    "mars_base": mars_base,
    "mars_tension": mars_tension,
    "mars_urgency": mars_urgency,
}


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", default=str(OUTPUT_DIR))
    arguments = parser.parse_args()
    destination = Path(arguments.output)
    destination.mkdir(parents=True, exist_ok=True)
    for name, builder in LAYERS.items():
        path = destination / f"{name}.ogg"
        sf.write(path, builder(), SAMPLE_RATE, format="OGG", subtype="VORBIS")
        print(f"{path}  {path.stat().st_size // 1024} KB")


if __name__ == "__main__":
    main()
