import java.util.List;

public class Arithmetic {

    public static float[][] horizontalGradient(float[][] input) {
        int r = input.length;
        int c = input[0].length;
        float[][] output = new float[r][c];

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                if (j == 0) {
                    output[i][j] = input[i][j + 1] - input[i][j];
                } else if (j == c - 1) {
                    output[i][j] = input[i][j] - input[i][j - 1];
                } else {
                    output[i][j] = (input[i][j + 1] - input[i][j - 1]) / 2;
                }
            }
        }

        return output;
    }

    public static double[][] Normalized(double[][] I) {
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;

        for (double[] i : I) {
            for (double f : i) {
                max = Math.max(max, f);
                min = Math.min(min, f);
            }
        }

        double[][] res = new double[I.length][I[0].length];
        for (int i = 0; i < res.length; i++) {
            for (int j = 0; j < res[0].length; j++) {
                res[i][j] = (I[i][j] - min) / (max - min);
            }
        }
        return res;
    }

    public static double[] Normalized(double[] I) {
        double max = -Double.MAX_VALUE;
        double min = Double.MAX_VALUE;

        for (double f : I) {
            max = Math.max(max, f);
            min = Math.min(min, f);
        }

        double[] res = new double[I.length];
        for (int i = 0; i < res.length; i++) {
                res[i] = (I[i] - min) / (max - min);
        }
        return res;
    }

    private static double mean(List<Integer> list) {
        double sum = 0;
        for (Integer integer : list) {
            sum += integer;
        }

        return sum / list.size();
    }

    public static float[][] verticalGradient(float[][] input) {
        int r = input.length;
        int c = input[0].length;
        float[][] output = new float[r][c];

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                if (i == 0) {
                    output[i][j] = input[i + 1][j] - input[i][j];
                } else if (i == r - 1) {
                    output[i][j] = input[i][j] - input[i - 1][j];
                } else {
                    output[i][j] = (input[i + 1][j] - input[i - 1][j]) / 2;
                }
            }
        }

        return output;
    }

    public static float[][] dotPow(float[][] input, int n) {
        int r = input.length;
        int c = input[0].length;

        float[][] output = new float[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                output[i][j] = (float) Math.pow(input[i][j], n);
            }
        }

        return output;
    }

    public static float[][] sumOfMatrices(float[][] a, float[][] b) {
        int r = a.length;
        int c = a[0].length;

        float[][] output = new float[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                output[i][j] = a[i][j] + b[i][j];
            }
        }

        return output;
    }

    public static float[][] sqrt(float[][] input) {
        int r = input.length;
        int c = input[0].length;

        float[][] output = new float[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                output[i][j] = (float) Math.sqrt(input[i][j]);
            }
        }

        return output;
    }

    public static float mean(float[][] input) {
        float val = 0;
        int cnt = input.length * input[0].length;

        for (float[] F : input) {
            for (float f : F) {
                val += f;
            }
        }

        return val / cnt;
    }

    public static float std(float[][] input) {
        float val = 0;
        int cnt = input.length * input[0].length;
        float mean = mean(input);

        for (float[] F : input) {
            for (float f : F) {
                val += (f - mean) * (f - mean);
            }
        }

        return (float) Math.sqrt(val / (cnt - 1));
    }

    public static boolean[][] matrixCompareWithANumber(float[][] mat, float num) {
        int r = mat.length;
        int c = mat[0].length;
        boolean[][] flags = new boolean[r][c];

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                flags[i][j] = mat[i][j] > num;
            }
        }

        return flags;
    }

    public static boolean[][] AND(boolean[][] a, boolean[][] b) {
        int r = a.length;
        int c = a[0].length;
        boolean[][] mat = new boolean[r][c];

        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                mat[i][j] = a[i][j] && b[i][j];
            }
        }

        return mat;
    }

    public static float stdOfARow(float[] input) {
        float val = 0;
        int cnt = input.length;

        float sum = 0;
        for (float f : input) {
            sum += f;
        }

        float mean = sum / cnt;

        for (float f : input) {
            val += (f - mean) * (f - mean);
        }

        return (float) Math.sqrt(val / (cnt - 1));
    }

    public static float[] stdOfColumns(float[][] input) {
        int r = input.length;
        int c = input[0].length;
        float[] res = new float[c];

        for (int i = 0; i < c; i++) {
            float val = 0;
            float sum = 0;
            for (float[] floats : input) {
                sum += floats[i];
            }
            float mean = sum / r;

            for (float[] f : input) {
                val += (f[i] - mean) * (f[i] - mean);
            }

            res[i] = (float) Math.sqrt(val / (r - 1));
        }
        return res;
    }
}
