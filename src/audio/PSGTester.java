package audio;

import java.io.InputStream;
import java.util.*;

public class PSGTester {
    private PSGTester() {}
    private static InputStream istream(String file) {
        return PSGTester.class.getResourceAsStream("/audio/" + file + ".txt");
    }
    public static void main(String[] args) throws Exception {
        final double PB = 1.0;
        List<PSG> music = new ArrayList<>();
/*
        music.add(new audio.PSG("slz/perc1").setPercussion(true));
        music.add(new audio.PSG("slz/perc2"));
        music.add(new audio.PSG("slz/bass", audio.PSG.Waveform.combine(x -> Math.sin(x + Math.sin(x / 2)), audio.PSG.Waveform.TRIANGLE)));
        music.add(new audio.PSG("slz/harmony1", audio.PSG.Waveform.SQUARE).setLoudness(1.5));
        music.add(new audio.PSG("slz/harmony2", music.get(music.size() - 1).getWaveform(0)).setLoudness(music.get(music.size() - 1).getLoudness()));
        music.add(new audio.PSG("slz/melody1", x -> Math.sin(x + 1.5 * Math.cos(5 * x))));
        music.add(new audio.PSG("slz/melody2", music.get(music.size() - 1).getWaveform(0)));

/*
        music.add(new audio.PSG("lofi/perc1").setPercussion(true));
        music.add(new audio.PSG("lofi/perc2").setPercussion(true));
        music.add(new audio.PSG("lofi/harmony1", new audio.PSG.DynamicWaveform() {
            @Override
            public double output(double x) {
                double n = 1 - n();
                return Math.sin(x + n * (Math.sin(3 * x) - Math.cos(x - 5 * n)));
            }
        }));
        music.add(new audio.PSG("lofi/harmony2", music.get(music.size() - 1).getWaveform(0)));
        music.add(new audio.PSG("lofi/harmony3", music.get(music.size() - 1).getWaveform(0)));
        music.add(new audio.PSG("lofi/melody", x -> audio.PSG.Waveform.TWO_OVER_PI * Math.atan(Math.tan(x / 2)) - 1.465 * Math.sin(x)).setLoudness(0.25));
*//*
        audio.PSG.Waveform piano = new audio.PSG.DynamicWaveform() {
            public double output(double x) {
                double n = 1 - n();
                return Math.sin(x - Math.pow(Math.sin(x - Math.cos(x)), 2) + n * (Math.sin(3 * x) + Math.cos(x + n)));
            }
        };
        music.add(new audio.PSG(istream("dreamer/bass"), audio.PSG.Waveform.dissolve(x -> Math.sin(x + Math.sin((2 * Math.cos(x - 1)) - Math.PI)), Math::sin)));
        music.add(new audio.PSG(istream("dreamer/perc1")).setPercussion(true).setLoudness(1.25));
        music.add(new audio.PSG(istream("dreamer/perc2")).setPercussion(true).setLoudness(1.25));
        music.add(new audio.PSG(istream("dreamer/melody1"), audio.PSG.Waveform.SQUARE, x -> Math.signum(Math.sin(x) - Math.sqrt(0.5))).setLoudness(1));
        music.add(new audio.PSG(istream("dreamer/melody2"), audio.PSG.Waveform.SQUARE, audio.PSG.Waveform.SAWTOOTH).setLoudness(1));
        music.add(new audio.PSG(istream("dreamer/harmony1"), piano, Math::sin));
        music.add(new audio.PSG(istream("dreamer/harmony2"), piano));
        music.add(new audio.PSG(istream("dreamer/harmony3"), piano));*/
/*
        PSG.Waveform w1 = PSG.Waveform.dissolve(x -> Math.sin(x + 1.5 * Math.cos(5 * x)), Math::sin);
        music.add(new CachedPSG(istream("fbz/melody1"), PSG.Waveform.SQUARE, w1));
        music.add(new CachedPSG(istream("fbz/bass"), PSG.Waveform.dissolve(PSG.Waveform.SAWTOOTH, Math::sin)));
        music.add(new CachedPSG(istream("fbz/perc1")).setPercussion(true));
        music.add(new CachedPSG(istream("fbz/harmony1"), PSG.Waveform.SAWTOOTH, PSG.Waveform.SQUARE).setLoudness(0.75));
        music.add(new CachedPSG(istream("fbz/harmony2"), PSG.Waveform.SAWTOOTH, PSG.Waveform.SQUARE).setLoudness(0.75));
        music.add(new CachedPSG(istream("fbz/perc2")).setPercussion(true).setLoudness(1.5));
        music.add(new CachedPSG(istream("fbz/melody2"), PSG.Waveform.SQUARE, w1));
     */
        music.add(new CachedPSG(istream("test"), Math::sin, (PSG.Waveform.SQUARE), PSG.Waveform.TRIANGLE, (PSG.Waveform.SAWTOOTH)));

        for (PSG psg : music) psg.setPlaybackSpeed(PB * psg.getPlaybackSpeed());

        for (byte i = 0, len = (byte) music.size(); i < len; i++) {
            for (PSG psg : music) psg.start();
            while (music.get(0).isRunning()) Thread.sleep(1);
        }
        for (PSG c : music) c.stop();
    }
}