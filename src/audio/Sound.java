package audio;

import java.util.*;

public class Sound {
    private Sound() {
    }
    public static void main(String[] args) throws Exception {
        final double PB = 1.075;
        List<PSG> music = new ArrayList<>();
/*
        music.add(new PSG("slz/perc1.txt").setPercussion(true));
        music.add(new PSG("slz/perc2"));
        music.add(new PSG("slz/bass", PSG.Waveform.combine(x -> Math.sin(x + Math.sin(x / 2)), PSG.Waveform.TRIANGLE)));
        music.add(new PSG("slz/harmony1", PSG.Waveform.SQUARE).setLoudness(1.5));
        music.add(new PSG("slz/harmony2", music.get(music.size() - 1).getWaveform()).setLoudness(music.get(music.size() - 1).getLoudness()));
        music.add(new PSG("slz/melody1", x -> Math.sin(x + 1.5 * Math.cos(5 * x))));
        music.add(new PSG("slz/melody2", music.get(music.size() - 1).getWaveform()));

/*
        music.add(new PSG("lofi/perc1.txt").setPercussion(true));
        music.add(new PSG("lofi/perc2").setPercussion(true));
        music.add(new PSG("lofi/harmony1", new PSG.DynamicWaveform() {
            @Override
            public double output(double x) {
                double n = 1 - n();
                return Math.sin(x + n * (Math.sin(3 * x) - Math.cos(x - 5 * n)));
            }
        }));
        music.add(new PSG("lofi/harmony2", music.get(music.size() - 1).getWaveform()));
        music.add(new PSG("lofi/harmony3", music.get(music.size() - 1).getWaveform()));
        music.add(new PSG("lofi/melody", x -> PSG.Waveform.TWO_OVER_PI * Math.atan(Math.tan(x / 2)) - 1.465 * Math.sin(x)).setLoudness(0.25));
*/
        PSG.Waveform piano = new PSG.DynamicWaveform() {
            public double output(double x) {
                double n = 1 - n();
                return Math.sin(x - Math.pow(Math.sin(x - Math.cos(x)), 2) + n * (Math.sin(3 * x) + Math.cos(x + n)));
            }
        };
        music.add(new PSG("dreamer/bass", PSG.Waveform.dissolve(x -> Math.sin(x + Math.sin((2 * Math.cos(x - 1)) - Math.PI)), Math::sin)));
        music.add(new PSG("dreamer/perc1").setPercussion(true).setLoudness(1.25));
        music.add(new PSG("dreamer/perc2").setPercussion(true).setLoudness(music.get(music.size() - 1).getLoudness()));
        music.add(new PSG("dreamer/melody1", PSG.Waveform.SQUARE, PSG.Waveform.combine(PSG.Waveform.SQUARE, x -> Math.signum(Math.sin(2 * x)))).setLoudness(1));
        music.add(new PSG("dreamer/melody2", PSG.Waveform.SQUARE, PSG.Waveform.SAWTOOTH).setLoudness(1));
        music.add(new PSG("dreamer/harmony1", piano));
        music.add(new PSG("dreamer/harmony2", piano));
        music.add(new PSG("dreamer/harmony3", piano));

        //music.add(new PSG("theheights/perc1").setPercussion(true).setLoudness(1));
        //music.add(new PSG("theheights/melody1", PSG.Waveform.SQUARE).setLoudness(1));

        for (PSG psg : music) psg.setPlaybackSpeed(PB * psg.getPlaybackSpeed());
        for (byte i = 0, len = (byte) (music.size()); i < len; i++) {
            for (PSG psg : music) psg.start();
            while (music.get(0).isRunning()) Thread.sleep(1);
        }
        for (PSG c : music) c.stop();
    }
}