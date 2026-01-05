import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Profiler {

    public static int analyzeKeyBreaker(BiFunction<int[][],String,Integer> method, int[][] inputImageGL,String process) {
        long time = timestamp();

        int result = method.apply(inputImageGL,process);

        time = timestamp() - time;

        System.out.println("Time elapsed : " + (time/1e6) + "ms");

        return result;
    }


    public static long timestamp() {
        return System.nanoTime();
    }
}
