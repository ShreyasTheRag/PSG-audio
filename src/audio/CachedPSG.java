package audio;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.SoftReference;
import java.util.*;
// PSG = Programmable sound generator
public class CachedPSG implements PSG {
    private static final float SAMPLE_RATE_KHZ = 44.1f; // 44.1 kHz sample rate (CD quality)
    private static final float SAMPLE_RATE_HZ = SAMPLE_RATE_KHZ * 1e3f;
    private static final AudioFormat FORMAT = new AudioFormat(SAMPLE_RATE_HZ, 8, 1, true, false); // 8-bit signed PCM, mono
    // NOTE: Must make into a Map<Waveform, Map<String[], byte[]>> to get rid of bugs
    private static final Map<PSG.Waveform, Map<Command, SoftReference<byte[]>>> cache = new HashMap<>();
    private boolean running, percussion;
    private double playbackSpeed, loudness;
    private byte wfPtr;
    private SourceDataLine channel;
    private List<Command> commands;
    private PSG.Waveform[] waveforms;

    private static Map<Command, SoftReference<byte[]>> createMap() {
        return new HashMap<>();
    }
    static {
        cache.put(null, createMap());
        for (PSG.Waveform pwf : PSG.PERCUSSION_WAVEFORMS) cache.put(pwf, createMap());
    }
    public CachedPSG(InputStream file, PSG.Waveform... waveforms) {
        this();
        this.waveforms = waveforms;
        for (PSG.Waveform wf : waveforms) if (!cache.containsKey(wf)) cache.put(wf, createMap());
        try (BufferedReader r = new BufferedReader(new InputStreamReader(Objects.requireNonNull(file)))) {
            commands = new LinkedList<>();
            for (String l = r.readLine(); l != null; l = r.readLine()) {
                if (!(l.isEmpty() || l.startsWith("//"))) {
                    l = l.toLowerCase().trim();
                    byte commentIndex = (byte) l.indexOf("//");
                    if (commentIndex != -1) l = l.substring(0, commentIndex);
                    commands.add(new Command(l.split(" ")));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public CachedPSG(InputStream file) {
        this(file, PSG.Waveform.SQUARE);
    }
    private CachedPSG() {
        running = false;
        wfPtr = 0;
        playbackSpeed = loudness = 1;
        try {
            channel = AudioSystem.getSourceDataLine(FORMAT);
            channel.open();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }
    public synchronized void start() {
        new Thread(this, toString()).start();
        running = true;
    }
    public void run() {
        try {
            channel.start();
            boolean hasOpening = false;
            for (Iterator<Command> it = commands.iterator(); it.hasNext() && !hasOpening; hasOpening = it.next().strings[0].equals("end"));
            if (hasOpening) {
                for (Command l = commands.remove(0); !l.strings[0].equals("end"); l = commands.remove(0)) {
                    process(l);
                }
            }
            for (Command l : commands) process(l);
            running = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private static byte parseSingleCharHex(char c) {
        if (c >= '0' && c <= '9') {
            return (byte) (c - '0');
        } else if (c >= 'A' && c <= 'F') {
            return (byte) (c - ('A' - 10));
        } else if (c >= 'a' && c <= 'f') {
            return (byte) (c - ('a' - 10));
        }
        return 0;
    }
    private void process(Command l) { // l is the split line that contains an instruction to play a tone/white noise
        if (l.strings[0].charAt(0) == 'c') { // A = 10, B = 11, C = 12, D = 13, E = 14, F = 15
            wfPtr = parseSingleCharHex(l.strings[1].charAt(0));
        } else {
            boolean noise = l.strings[0].charAt(0) == 'w';
            PSG.Waveform currentWF = null;
            if (percussion) {
                currentWF = PERCUSSION_WAVEFORMS[parseSingleCharHex(l.strings[0].charAt(0)) % PERCUSSION_WAVEFORMS.length];
            } else if (!noise) {
                currentWF = waveforms[wfPtr];
            }
            Map<Command, SoftReference<byte[]>> wfCache = cache.get(currentWF); // remember this is a POINTER to a map
            SoftReference<byte[]> sample = wfCache.getOrDefault(l, null); // Get the sample corresponding to l
            if (sample == null) { // If the sample is not found, create it
                double amp = Double.parseDouble(l.strings[1]);
                double timeMS = Double.parseDouble(l.strings[2]);
                byte[] s;
                if (noise) {
                    s = genWhiteNoise(l.strings[0].length() > 1 ? Integer.parseInt(l.strings[0].substring(1)) : 1, amp, timeMS, l.strings.length == 4);
                } else if (l.strings.length == 4) {
                    s = genTone(currentWF, Double.parseDouble(l.strings[0]), amp, timeMS, l.strings[3].contains("a"), l.strings[3].contains("v"));
                } else {
                    s = genTone(currentWF, percussion ? 440.0 : Double.parseDouble(l.strings[0]), amp, timeMS, false, false);
                }
                sample = new SoftReference<>(s);
                wfCache.put(l, sample);
            }
            channel.write(sample.get(), 0, Objects.requireNonNull(sample.get()).length); // Here, the sample is finally played back by the sound channel
        }
    }
    public void stop() {
        channel.drain();
        channel.stop();
        running = false;
    }
    public CachedPSG setPercussion(boolean percussion) {
        this.percussion = percussion;
        return this;
    }
    public CachedPSG setLoudness(double loudness) {
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
    public CachedPSG setPlaybackSpeed(double playbackSpeed) {
        if (playbackSpeed != 0)
            this.playbackSpeed = playbackSpeed;
        return this;
    }
    public double getPlaybackSpeed() {
        return playbackSpeed;
    }
    public PSG.Waveform getWaveform(int index) {
        return waveforms[index];
    }
    public CachedPSG setWaveform(int index, PSG.Waveform waveform) {
        if (index >= waveforms.length) waveforms = Arrays.copyOf(waveforms, index + 1);
        waveforms[index] = waveform;
        return this;
    }
    private byte[] genTone(PSG.Waveform wf, double freq, double amp, double ms, boolean attenuate, boolean vibrato) {
        amp = Math.min(1, Math.abs(amp * loudness));
        ms /= playbackSpeed;
        byte[] sample = emptySample(ms);
        double period = SAMPLE_RATE_HZ / freq;
        double b = TWO_PI / period;
        for (int i = 0; i < sample.length; i++) {
            double x = b * i, n = i / (double) sample.length;
            if (wf instanceof PSG.DynamicWaveform) ((PSG.DynamicWaveform) wf).setN(n);
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
    private static class Command {
        String[] strings;
        private int hashCode = 0;

        Command(String[] strings) {
            this.strings = strings;
            for (String s : strings) hashCode += s.hashCode();
        }
        public int hashCode() {
            return hashCode;
        }
        public boolean equals(Object o) {
            return this == o || (o != null && getClass() == o.getClass() && Arrays.equals(strings, ((Command) o).strings));
        }
    }
}