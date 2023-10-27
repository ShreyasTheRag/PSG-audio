package audio;
/*
import javax.sound.sampled.*;
import static java.util.Arrays.fill;
import static javax.sound.sampled.AudioSystem.getSourceDataLine;
*/
public class FunkyWaveform<T extends PSG.Waveform> extends PSG.DynamicWaveform {
    private final T waveform;

    public FunkyWaveform(T wf) {
        waveform = wf;
    }

        /*
        final char SAMPLE_RATE = 48000;
        SourceDataLine channel = getSourceDataLine(new AudioFormat(SAMPLE_RATE, 8, 1, false, true));
        channel.open();
        channel.start();
        byte[] sample = new byte[6];
        int duration = (int) ((30.0 / sample.length) * SAMPLE_RATE);
        for (int t = 0; t < duration; t++) {
            fill(sample, (byte) ((t >> 1) & (t | 2)));
            channel.write(sample, 0, sample.length);
        }
        channel.drain();
        channel.stop();
        channel.close();*/

    @Override
    public double output(double x) {
        return waveform.output((1 - n()) * x);
    }
}