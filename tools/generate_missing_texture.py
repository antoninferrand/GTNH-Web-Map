#!/usr/bin/env python3
"""Generate Minecraft's 16x16 black/magenta missing-sprite checkerboard."""
import pathlib
import struct
import zlib


def generate():
    def chunk(kind, value):
        return struct.pack('>I', len(value)) + kind + value + struct.pack('>I', zlib.crc32(kind + value))
    rows = []
    for y in range(16):
        row = bytearray([0])
        for x in range(16):
            row.extend((248, 0, 248, 255) if (x < 8) == (y < 8) else (0, 0, 0, 255))
        rows.append(bytes(row))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', 16, 16, 8, 6, 0, 0, 0)) +
            chunk(b'IDAT', zlib.compress(b''.join(rows))) + chunk(b'IEND', b''))


if __name__ == '__main__':
    target = pathlib.Path(__file__).resolve().parents[1] / (
        'src/main/resources/texturepacks/standard/assets/gtnhwebmap/textures/blocks/missing.png')
    pixels = generate()
    target.write_bytes(pixels)
    # Separate filename keeps its 4x4 model UV layout independent of 1x1 consumers.
    target.with_name('missing-model-atlas.png').write_bytes(pixels)
