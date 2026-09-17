"""Extrai o componente periodico de uma textura, sem espelhar ou borrar bordas."""

from pathlib import Path
import sys

from PIL import Image
import numpy as np


def periodic_tile(source: Path, destination: Path) -> None:
    image = Image.open(source).convert("RGB")
    size = min(image.size)
    left = (image.width - size) // 2
    top = (image.height - size) // 2
    image = image.crop((left, top, left + size, top + size))

    source_pixels = np.asarray(image, dtype=np.float64)
    height, width = source_pixels.shape[:2]

    boundary = np.zeros_like(source_pixels)
    boundary[0, :, :] = source_pixels[-1, :, :] - source_pixels[0, :, :]
    boundary[-1, :, :] = source_pixels[0, :, :] - source_pixels[-1, :, :]
    boundary[:, 0, :] += source_pixels[:, -1, :] - source_pixels[:, 0, :]
    boundary[:, -1, :] += source_pixels[:, 0, :] - source_pixels[:, -1, :]

    yy = np.arange(height)[:, None]
    xx = np.arange(width)[None, :]
    denominator = (2.0 * np.cos(2.0 * np.pi * xx / width)
                   + 2.0 * np.cos(2.0 * np.pi * yy / height) - 4.0)
    denominator[0, 0] = 1.0

    smooth = np.empty_like(source_pixels)
    for channel in range(3):
        spectrum = np.fft.fft2(boundary[:, :, channel]) / denominator
        spectrum[0, 0] = 0.0
        smooth[:, :, channel] = np.fft.ifft2(spectrum).real

    periodic = source_pixels - smooth

    # Fecha o valor dos pixels opostos com uma correcao suave de baixa amplitude.
    # Diferente de copiar/espelhar faixas, isso conserva todo o detalhe local.
    margin = max(16, size // 26)
    horizontal_delta = periodic[:, -1, :] - periodic[:, 0, :]
    for distance in range(margin):
        weight = (1.0 - distance / margin) ** 2 * .5
        periodic[:, distance, :] += horizontal_delta * weight
        periodic[:, -1 - distance, :] -= horizontal_delta * weight

    vertical_delta = periodic[-1, :, :] - periodic[0, :, :]
    for distance in range(margin):
        weight = (1.0 - distance / margin) ** 2 * .5
        periodic[distance, :, :] += vertical_delta * weight
        periodic[-1 - distance, :, :] -= vertical_delta * weight

    periodic = np.clip(periodic, 0, 255).astype(np.uint8)
    Image.fromarray(periodic, "RGB").save(destination, optimize=True)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("uso: make_reflected_seamless.py origem.png destino.png")
    periodic_tile(Path(sys.argv[1]), Path(sys.argv[2]))
