import java.util.HashMap;
import java.util.Map;

public class WithWidth1D {
    private double sigma;
    private double rBead;
    private double b;
    private double A;
    private final double[] x;
    private final Map<Double, Double> f;

    public WithWidth1D(double[] x) {
        this.x = x;
        f = new HashMap<>();
    }

    public WithWidth1D(double sigma, double rBead, double b, double A, double[] x) {
        setPara(sigma, rBead, b, A);
        f = new HashMap<>();
        this.x = x;
        upgrade();
    }

    public double getY(double x) {
        return f.get(x);
    }

    public double[] getAllX() {
        return x;
    }

    public void upgrade() {
        double[] y = withWidth(this.sigma, this.rBead, this.b, this.A, this.x);
        for (int i = 0; i < x.length; i++) {
            f.put(x[i], y[i]);
        }
    }

    public void setPara(double sigma, double rBead, double b, double A) {
        this.sigma = sigma;
        this.rBead = rBead;
        this.b = b;
        this.A = A;
    }

    public boolean samePara(double sigma, double rBead, double b, double A) {
        return this.sigma == sigma && this.rBead == rBead && this.b == b && this.A == A;
    }

    public static double[] withWidth(double sigma, double rBead, double b, double A, double[] x) {
        double[] g1 = new double[x.length];
        double[] g2 = new double[x.length];
        double[] g; //conv[g2,g1,'same']
        double[] TEM1 = new double[x.length];//x+rBead
        double[] TEM2 = new double[x.length];//x-rBead
        double[] res1; //sigmoid[x+rBead]
        double[] res2; //sigmoid[x-rBead]
        double[] res = new double[x.length]; // g+b

        for (int i = 0; i < x.length; i++) {
            g1[i] = A * Math.exp(-x[i] * x[i] / (2 * sigma * sigma));
        }
        for (int i = 0; i < x.length; i++) {
            TEM1[i] = x[i] + rBead;
            TEM2[i] = x[i] - rBead;
        }
        res1 = sigmoid(TEM1);
        res2 = sigmoid(TEM2);
        for (int i = 0; i < x.length; i++) {
            g2[i] = res1[i] - res2[i];
        }
        g = conv(g1, g2);
        for (int i = 0; i < g.length; i++) {
            res[i] = g[i] + b;
        }
        return res;
    }

    public static double[] sigmoid(double[] x) {
        int w = 10000000;
        double[] fenmu = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            fenmu[i] = 1 / (1 + Math.exp(-w * x[i]));
        }
        return fenmu;
    }

    public static double[] conv(double[] con1, double[] con2) {
        double[] res1 = new double[con1.length + con2.length - 1];
        double[] res = new double[con1.length];

        for (int i = 0; i < con1.length + con2.length - 1; i++) {
            double temp = 0;
            for (int k = 0; k < con1.length; k++) {
                if ((i - k) >= 0 && (i - k) < con2.length) {
                    temp += con1[k] * con2[i - k];
                }
                res1[i] = temp;
            }
        }
        System.arraycopy(res1, con2.length / 2, res, 0, con1.length);
        return res;
    }
}
