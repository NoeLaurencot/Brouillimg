import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Profiler {

    static long globalTime = 0;
    static int counter = 0;

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

        System.out.printf("Time elapsed for offest : %.2f ms\n", time / 1e6);

        return result;
    }

    public static int analyzeFindNeighbor(BiFunction<int[][], Integer, Integer> method, int[][] imageGL, int index){
        long time = timestamp();

        int result = method.apply(imageGL, index);

        time = timestamp() - time;

        System.out.printf("Time elapsed to find neighbor : %.2f ms\n", time / 1e6);

        return result;
    }

    public static  int analyzeFindStep(Function<int[][], Integer> method, int[][] imageGL){
        long time = timestamp();

        int result = method.apply(imageGL);

        time = timestamp() - time;

        System.out.printf("Time elapsed for step : %.2f ms\n", time / 1e6);

        return result;
    }

    public static int analyzeKeyBreakProcess(Function<int[][], Integer> method, int[][] inputImageGL) {
        long time = timestamp();

        int result = method.apply(inputImageGL);

        globalTime += timestamp() - time;
        counter++;

        return result;
    }

    public static void resetCounter() {
        counter = 0;
    }

    public static void resetTime() {
        globalTime = 0;
    }

    public static double getTimeAvg() {
        return ((double) globalTime / counter) / 1e6;
    }



    public static long timestamp() {
        return System.nanoTime();
    }
}
