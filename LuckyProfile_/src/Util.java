import java.util.ArrayList;
import java.util.List;

public class Util {
    public static double[] getEdges(List<Integer> norrzero, int minx, int maxx) {
        int xrange = maxx - minx;
        if (xrange <= 50) return integerrule(minx, maxx);
        else {
            float[][] f = new float[1][norrzero.size()];
            for (int i = 0; i < norrzero.size(); i++) {
                f[0][i] = norrzero.get(i);
            }
            float std = Arithmetic.std(f);
            return scottsrule(std, minx, maxx, norrzero.size());
        }
    }

    private static double[] scottsrule(float std, int minx, int maxx, int size) {
        double rawBinWidth = 3.5 * std / Math.pow(size, 1.0 / 3.0);
        double powOfTen = Math.pow(10, Math.floor(Math.log10(rawBinWidth)));
        double relSize = rawBinWidth / powOfTen;
        double binWidth;
        if (relSize < 1.5)
            binWidth = 1 * powOfTen;
        else if (relSize < 2.5)
            binWidth = 2 * powOfTen;
        else if (relSize < 4)
            binWidth = 3 * powOfTen;
        else if (relSize < 7.5)
            binWidth = 5 * powOfTen;
        else
            binWidth = 10 * powOfTen;

        double leftEdge = Math.min(binWidth * Math.floor(minx / binWidth), minx);
        double nbinsActual = Math.max(1, Math.ceil((maxx - leftEdge) / binWidth));
        double rightEdge = Math.max(leftEdge + nbinsActual * binWidth, maxx);

        List<Double> edges = new ArrayList<>();
        if (nbinsActual == 1) {
            return new double[]{leftEdge, rightEdge};
        } else {
            edges.add(leftEdge);
            int i = 1;
            while (i < nbinsActual) {
                edges.add(leftEdge + i++ * binWidth);
            }
            edges.add(rightEdge);
        }

        return edges.stream().mapToDouble(t -> t).toArray();
    }

    private static double[] integerrule(int minx, int maxx) {
        double edge = minx - 0.5;
        List<Double> edges = new ArrayList<>();
        while (edge <= maxx + 0.5) {
            edges.add(edge++);
        }
        return edges.stream().mapToDouble(t -> t).toArray();
    }
}
