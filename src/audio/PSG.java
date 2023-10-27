package audio;
// PSG = Programmable sound generator
public interface PSG extends Runnable {
    double TWO_PI = 2 * Math.PI;
    Waveform[] PERCUSSION_WAVEFORMS = {Waveform.KICK, Waveform.SNARE};
    void start();
    void stop();
    PSG setPercussion(boolean b);
    PSG setLoudness(double d);
    double getLoudness();
    boolean isRunning();
    boolean isPercussion();
    PSG setPlaybackSpeed(double d);
    double getPlaybackSpeed();

    @FunctionalInterface
    interface Waveform {
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
                for (Waveform w : waveforms) sum += w.output(x);
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
    abstract class DynamicWaveform implements Waveform {
        private double n;

        protected final double n() {
            return n;
        }
        public final void setN(double n) {
            if (n >= 0.0 && n <= 1.0) this.n = n;
        }
    }
}