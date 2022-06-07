package core;

import java.util.ArrayList;
import java.util.List;

public class Aggregate {

    public static List<int[]> aggregate(List<int[]> Loc, int[][] SkelImwithout1, int imgH, int imgW) {
        List<int[]> ALoc = new ArrayList<>(Loc);

        for (int[] ints : Loc) {
            int x = ints[0];
            int y = ints[1];
            int[][] L = new int[][]{{x - 1, y - 1}, {x - 1, y}, {x - 1, y + 1}, {x, y - 1}, {x, y + 1}, {x + 1, y - 1}, {x + 1, y}, {x + 1, y + 1}};
            for (int i = 0; i < 8; i++) {
                boolean[] jud = new boolean[ALoc.size()];
                for (int j = 0; j < jud.length; j++) {
                    jud[j] = (ALoc.get(j)[0] == L[i][0]) && (ALoc.get(j)[1] == L[i][1]);
                }

                if (L[i][0] >= 0 && L[i][0] < imgH && L[i][1] >= 0 && L[i][1] < imgW) {
                    if (SkelImwithout1[L[i][0]][L[i][1]] == 1) {
                        boolean flag = false;
                        for (boolean b : jud) {
                            flag |= b;
                        }
                        if (!flag) {
                            ALoc.add(L[i]);
                        }
                    }
                }
            }
        }
        return ALoc;
    }
}
