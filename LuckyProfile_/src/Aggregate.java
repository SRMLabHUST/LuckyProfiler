import java.util.List;

public class Aggregate {

    public static void aggregate(List<Integer> Loc, int[] SkelImwithout1, int start, int end, int imgW) {
        for (int i = start; i < end; i++) {
            int l = Loc.get(i);
            int[] neighbors = { l - imgW - 1, l - imgW, l - imgW + 1 , l - 1, l + 1, l + imgW - 1, l + imgW , l + imgW + 1 };
            for (int neighbor : neighbors) {
                if (SkelImwithout1[neighbor] == 1 && !Loc.contains(neighbor)) {
                    Loc.add(neighbor);
                }
            }
        }
    }
}
