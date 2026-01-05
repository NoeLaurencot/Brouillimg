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

    public static int analyzeFindOffest(BiFunction<int[], Integer, Integer> method, int[] chunk, Integer jump) {
        long time = timestamp();

        int result = method.apply(chunk,jump);

        time = timestamp() - time;

        System.out.printf("Time elapsed : %.2f ms\n", time / 1e6);

        return result;
    }

    public static int analyzeSmallestModularDiff(BiFunction<int[],Integer,Integer> method,int[] chunk,int size){
        long time = timestamp();

        int result = method.apply(chunk,size);

        time = timestamp() - time;

        System.out.println("Time for Smallest Modular Diff : " + (time/1e6) + "ms");

        return result;
    }


    public static long timestamp() {
        return System.nanoTime();
    }
}
