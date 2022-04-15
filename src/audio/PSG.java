package audio;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import javax.sound.sampled.*;
// PSG = Programmable sound generator
public class PSG implements Runnable {
    private static final double TWO_PI = 2 * Math.PI;
    private static final float SAMPLE_RATE_KHZ = 44.1f; // 44.1 kHz sample rate (CD quality)
    private static final float SAMPLE_RATE_HZ = SAMPLE_RATE_KHZ * 1e3f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE_HZ, 8, 1, true, true); // 8-bit signed PCM, mono
    private static final Waveform[] PERCUSSION_WAVEFORMS = {Waveform.KICK, Waveform.SNARE};
    private static final Class<PSG> CLASS = PSG.class;
    private boolean running, percussion;
    private double playbackSpeed, loudness;
    private byte wf_ptr = 0;
    private final Map<String[], byte[]> cache; // to improve performance and save memory by avoiding unnecessarily creating samples that will hold the same data
    private SourceDataLine channel;
    private List<String[]> commands;
    private Waveform[] waveforms;
    private String file;

    public PSG(String file, Waveform... waveforms) {
        this();
        this.file = file;
        this.waveforms = waveforms;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(Objects.requireNonNull(CLASS.getResourceAsStream("/audio/" + file + ".txt"))))) {
            commands = new ArrayList<>();
            for (String l = r.readLine(); l != null; l = r.readLine()) {
                if (!(l.isEmpty() || l.startsWith("//"))) {
                    l = l.toLowerCase().trim();
                    byte commentIndex = (byte) l.indexOf("//");
                    if (commentIndex != -1) {
                        l = l.substring(0, commentIndex).toLowerCase();
                    }
                    commands.add(l.split(" "));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public PSG(String file) {
        this(file, Waveform.SQUARE);
    }
    public PSG() {
        running = false;
        playbackSpeed = loudness = 1;
        cache = new HashMap<>();
        try {
            channel = AudioSystem.getSourceDataLine(FORMAT);
            channel.open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
    public void start() {
        new Thread(this, file == null ? this.toString() : file).start();
        running = true;
    }
    public void run() {
        try {
            channel.start();
            boolean hasOpening = false;
            for (String[] l : commands) {
                if (l[0].equals("end")) {
                    hasOpening = true;
                    break;
                }
            }
            if (hasOpening) {
                for (String[] l = commands.remove(0); !l[0].equals("end"); l = commands.remove(0)) {
                    process(l);
                }
            }
            for (String[] l : commands) {
                process(l);
            }
            running = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public final void process(String[] l) {
        if (l[0].charAt(0) == 'c') {
            wf_ptr = (byte) Integer.parseInt(l[1], 16); // A = 10, B = 11, C = 12, D = 13, E = 14, F = 15
        } else {
            byte[] sample;
            if (cache.containsKey(l)) {
                sample = cache.get(l);
            } else {
                double amp = Double.parseDouble(l[1]);
                double timeMS = Double.parseDouble(l[2]);
                if (l[0].charAt(0) == 'w') {
                    sample = genWhiteNoise(l[0].length() > 1 ? Integer.parseInt(l[0].substring(1)) : 1, amp, timeMS, l.length == 4);
                } else if (l.length == 4) {
                    sample = genTone(waveforms[wf_ptr], Double.parseDouble(l[0]), amp, timeMS, l[3].contains("a"), l[3].contains("v"));
                } else {
                    double arg1 = Double.parseDouble(l[0]);
                    if (percussion) {
                        sample = genTone(PERCUSSION_WAVEFORMS[((int) arg1) % PERCUSSION_WAVEFORMS.length], 440, amp, timeMS, false, false);
                    } else {
                        sample = genTone(waveforms[wf_ptr], arg1, amp, timeMS, false, false);
                    }
                }
                cache.put(l, sample);
            }
            channel.write(sample, 0, sample.length); // Here, the sample is finally played back by the sound channel
        }
    }
    public void stop() {
        channel.drain();
        channel.stop();
        running = false;
    }
    public PSG setPercussion(boolean percussion) {
        this.percussion = percussion;
        return this;
    }
    public PSG setLoudness(double loudness) {
        this.loudness = Math.abs(loudness);
        return this;
    }
    public double getLoudness() {
        return loudness;
    }
    public boolean isRunning() {
        return running;
    }
    public boolean isPercussion() {
        return percussion;
    }
    public PSG setPlaybackSpeed(double playbackSpeed) {
        if (playbackSpeed != 0)
            this.playbackSpeed = playbackSpeed;
        return this;
    }
    public double getPlaybackSpeed() {
        return playbackSpeed;
    }
    public Waveform getWaveform(int index) {
        return waveforms[index];
    }
    public PSG setWaveform(int index, Waveform waveform) {
        this.waveforms[index] = waveform;
        return this;
    }
    private byte[] genTone(Waveform wf, double freq, double amp, double ms, boolean attenuate, boolean vibrato) {
        amp = Math.min(1, Math.abs(amp * loudness));
        ms /= playbackSpeed;
        byte[] sample = emptySample(ms);
        double period = SAMPLE_RATE_HZ / freq;
        double b = TWO_PI / period;
        for (int i = 0; i < sample.length; i++) {
            double x = b * i, n = i / (double) sample.length;
            if (wf instanceof DynamicWaveform) ((DynamicWaveform) wf).setN(n);
            if (vibrato) x += Math.sin(n * ms * 3e-2) * 1.75;
            double f = wf.output(x);
            if (attenuate) f /= Math.exp(x / (1.5 * ms > 1000 ? ms : 1000));
            sample[i] = (byte) (Byte.MAX_VALUE * Math.max(Math.min(f * amp, 1), -1));
        }
        return sample;
    } // -0.92375 for 12.5%, -0.5 for 33.3%, -Math.sqrt(0.5) for 25%
    private byte[] genWhiteNoise(int stepDown, double amp, double ms, boolean attenuate) {
        amp = Math.min(1, Math.abs(amp * loudness));
        stepDown = Math.max(1, Math.abs(stepDown));
        ms /= playbackSpeed;
        byte[] sample = emptySample(ms);
        double f = 0;
        for (int i = 0; i < sample.length; i++) {
            double n = i / (double) sample.length;
            if (i % stepDown == 0) f = Math.random();
            if (attenuate) f /= Math.exp(2 * n);
            sample[i] = (byte) (Byte.MAX_VALUE * Math.max(Math.min(f * amp, 1), -1));
        }
        return sample;
    }
    private static byte[] emptySample(double ms) {
        return new byte[(int) (ms * SAMPLE_RATE_KHZ)];
    }
    @FunctionalInterface
    public interface Waveform {
        double TWO_OVER_PI = 2 / Math.PI;
        Waveform SQUARE = x -> Math.signum(Math.sin(x));
        Waveform TRIANGLE = x -> TWO_OVER_PI * Math.asin(Math.sin(x));
        Waveform SAWTOOTH = x -> TWO_OVER_PI * Math.atan(Math.tan(x / 2));
        Waveform KICK = x -> Math.sin(25 * Math.log(x)) / Math.exp(x / 175);
        Waveform SNARE = x -> {
            double base = Math.cos(3.5 * Math.pow(Math.log(x), 2));
            for (byte i = 0; i < 4; i++) base += Math.random();
            return base / Math.exp(x / 175);
        };
        double output(double x);
        static Waveform combine(Waveform... waveforms) {
            return x -> {
                double sum = 0.0;
                for (Waveform w : waveforms) {
                    sum += w.output(x);
                }
                return sum / waveforms.length;
            };
        }
        static Waveform dissolve(Waveform from, Waveform to) {
            return new DynamicWaveform() {
                public double output(double x) {
                    return (1 - n()) * from.output(x) + n() * to.output(x);
                }
            };
        }
    }
    public static abstract class DynamicWaveform implements Waveform {
        private double n;

        protected final double n() {
            return n;
        }
        private void setN(double n) {
            this.n = n;
        }
    }
}