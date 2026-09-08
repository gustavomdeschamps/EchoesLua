"""Deterministic original campaign Foley and synthesized diegetic cues."""
from pathlib import Path
import numpy as np
import soundfile as sf

RATE = 44100
OUT = Path(__file__).resolve().parents[1] / 'assets' / 'sounds'
def cue(name, duration, frequency, noise_gain, decay, peak, seed):
    t = np.arange(int(RATE * duration)) / RATE
    rng = np.random.default_rng(seed)
    noise = rng.normal(0, 1, t.size)
    noise = np.convolve(noise, np.ones(18) / 18, mode='same')
    env = np.minimum(t / .015, 1) * np.exp(-decay*t)
    env *= np.minimum((duration-t)/.05, 1)
    tone = np.sin(2*np.pi*(frequency*t + .1*frequency*t*t))
    signal = (tone*.35 + noise*noise_gain)*env
    signal *= peak/max(np.max(np.abs(signal)), 1e-6)
    sf.write(OUT / (name+'.ogg'), signal, RATE, format='OGG', subtype='VORBIS')

if __name__ == '__main__':
    for row in [
        ('passo_marte', .24, 130, 2.6, 20, .28, 1),
        ('passo_tita', .35, 68, 1.8, 13, .28, 2),
        ('dialogo', .09, 720, .12, 30, .18, 3),
        ('portal_ativar', 1.5, 95, .6, .8, .48, 4),
        ('boss_rugido', 1.15, 44, 3, 2, .5, 5),
        ('boss_ataque', .65, 58, 4, 7, .6, 6),
        ('boss_morte', 1.8, 32, 4, 1.7, .5, 7),
        ('impacto_hostil', .2, 180, 3.5, 22, .4, 8),
    ]:
        cue(*row)
        print(row[0])
