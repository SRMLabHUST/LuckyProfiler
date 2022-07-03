import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Adjangle {
    public static double adjangle1(Integer[] Locx, Integer[] Locy, int i, int[][] SkelImwithout1) {
        int G1 = SkelImwithout1[Locx[i]][Locy[i] + 1];
        int G2 = SkelImwithout1[Locx[i] - 1][Locy[i] + 1];
        int G3 = SkelImwithout1[Locx[i] - 1][Locy[i]];
        int G4 = SkelImwithout1[Locx[i] - 1][Locy[i] - 1];
        int G5 = SkelImwithout1[Locx[i]][Locy[i] - 1];
        int G6 = SkelImwithout1[Locx[i] + 1][Locy[i] - 1];
        int G7 = SkelImwithout1[Locx[i] + 1][Locy[i]];
        int G8 = SkelImwithout1[Locx[i] + 1][Locy[i] + 1];
        int G9 = SkelImwithout1[Locx[i] + 1][Locy[i] + 2];
        int G10 = SkelImwithout1[Locx[i]][Locy[i] + 2];
        int G11 = SkelImwithout1[Locx[i] - 1][Locy[i] + 2];
        int G12 = SkelImwithout1[Locx[i] - 2][Locy[i] + 2];
        int G13 = SkelImwithout1[Locx[i] - 2][Locy[i] + 1];
        int G14 = SkelImwithout1[Locx[i] - 2][Locy[i]];
        int G15 = SkelImwithout1[Locx[i] - 2][Locy[i] - 1];
        int G16 = SkelImwithout1[Locx[i] - 2][Locy[i] - 2];
        int G17 = SkelImwithout1[Locx[i] - 1][Locy[i] - 2];
        int G18 = SkelImwithout1[Locx[i]][Locy[i] - 2];
        int G19 = SkelImwithout1[Locx[i] + 1][Locy[i] - 2];
        int G20 = SkelImwithout1[Locx[i] + 2][Locy[i] - 2];
        int G21 = SkelImwithout1[Locx[i] + 2][Locy[i] - 1];
        int G22 = SkelImwithout1[Locx[i] + 2][Locy[i]];
        int G23 = SkelImwithout1[Locx[i] + 2][Locy[i] + 1];
        int G24 = SkelImwithout1[Locx[i] + 2][Locy[i] + 2];


        int[][] G = new int[][]{{G16, G15, G14, G13, G12}, {G17, G4, G3, G2, G11}, {G18, G5, 1, G1, G10}, {G19, G6, G7, G8, G9}, {G20, G21, G22, G23, G24}};

        List<Integer> x = new ArrayList<>();
        List<Integer> y = new ArrayList<>();
        for (int i1 = 0; i1 < G.length; i1++) {
            for (int i2 = 0; i2 < G[0].length; i2++) {
                if (G[i1][i2] == 1) {
                    x.add(i1);
                    y.add(i2);
                }
            }
        }

        int N = 0;
        for (int[] ints : G) {
            N += Arrays.stream(ints).sum();
        }

        int[] xy = new int[x.size()];
        for (int i1 = 0; i1 < x.size(); i1++) {
            xy[i1] = x.get(i1) * y.get(i1);
        }

        int temp1 = Arrays.stream(xy).sum();

        double meanx = mean(x);
        double meany = mean(y);

        double temp2 = N * meanx * meany;

        int[] xx = new int[x.size()];
        for (int i1 = 0; i1 < x.size(); i1++) {
            xx[i1] = x.get(i1) * x.get(i1);
        }

        int temp3 = Arrays.stream(xx).sum();

        double temp4 = N * meanx * meanx;

        double k = (temp1 - temp2) / (temp3 - temp4);

        double ang;

        if (Double.isNaN(k)) {
            ang = Math.PI / 2;
        } else {
            ang = Math.atan(k);
        }

        return ang;
    }

    private static double mean(List<Integer> list) {
        double sum = 0;
        for (Integer integer : list) {
            sum += integer;
        }

        return sum / list.size();
    }
}
