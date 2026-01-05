import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Profiler {

    public static int analyzeKeyBreaker(BiFunction<int[][], String, Integer> method, int[][] inputImageGL,String process) {
        long time = timestamp();

        int result = method.apply(inputImageGL,process);

        time = timestamp() - time;

        System.out.printf("Time elapsed : %.2f ms\n", time / 1e6);

        return result;
    }

    public static int analyzeFindOffest(BiFunction<int[][], Integer, Integer> method, int[][] imageGL, Integer jump) {
        long time = timestamp();

        int result = method.apply(imageGL,jump);

        time = timestamp() - time;

        System.out.printf("Time elapsed for R : %.2f ms\n", time / 1e6);

        return result;
    }

    public static int[] analyzeFindChunk(BiFunction<int[][], Integer, int[]> method, int[][] imageGL, int index){
        long time = timestamp();

        int result[] = method.apply(imageGL, index);

        time = timestamp() - time;

        System.out.printf("Time elapsed to find chunk : %.2f ms\n", time / 1e6);

        return result;
    }


    public static long timestamp() {
        return System.nanoTime();
    }
}
