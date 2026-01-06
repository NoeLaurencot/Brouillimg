// LAMOUR Pierre, LAURENÇOT Noé, S1B2, n°12

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class Brouillimg {
    static final Scanner input = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println(
                    "Usage: java Brouillimg <process> <image_claire> <clé> [image_sortie]");
            System.exit(1);
        }

        String process = args[0];
        String inPath = args[1];
        String outPath = (args.length >= 4) ? args[3] : "out.png";

        // Masque 0x7FFF pour garantir que la clé ne dépasse pas les 15 bits

        int key = Integer.parseInt(args[2]) & 0x7FFF;

        BufferedImage inputImage = ImageIO.read(new File(inPath));

        if (inputImage == null) {
            throw new IOException("Format d’image non reconnu: " + inPath);
        }

        final int height = inputImage.getHeight();

        final int width = inputImage.getWidth();

        System.out.println("Dimensions de l'image : " + width + "x" + height);

        // Pré‑calcul des lignes en niveaux de gris pour accélérer le calcul du critère
        int[][] inputImageGL = rgb2gl(inputImage);

        int[] perm;
        BufferedImage scrambledImage;

        switch (process) {
            case "scramble":
                perm = generatePermutation(height, key);
                scrambledImage = scrambleLines(inputImage, perm);
                ImageIO.write(scrambledImage, "png", new File(outPath));
                System.out.println("Image écrite: " + outPath);
                break;
            case "unscramble":
                perm = generatePermutation(height, key);
                BufferedImage unScrambledImage = unScrambleLines(inputImage, perm);
                System.out.println("Image écrite: " + outPath);
                ImageIO.write(unScrambledImage, "png", new File(outPath));
                break;
            case "euclidean",
                 "pearson",
                 "neighbor":
                key = Profiler.analyzeKeyBreaker(Brouillimg::breakKey, inputImageGL, process);
                System.out.println("Clé trouvée: " + key);
                perm = generatePermutation(height, key);
                unScrambledImage = unScrambleLines(inputImage, perm);
                System.out.println("Image écrite: " + outPath);
                ImageIO.write(unScrambledImage, "png", new File(outPath));
                break;
            case "profile":
                int choice;
                do {
                    System.out.println("Avec quels process vouslez-vous tester?");
                    System.out.println("1) Euclidean");
                    System.out.println("2) Pearson");
                    System.out.println("3) Neighbor");
                    choice = input.nextInt();
                } while (!(choice == 1 || choice == 2 || choice == 3));
                profileBreakKey(inputImage, choice);
                break;
            default:
                throw new IOException("Process non renseigné");
        }

    }

    /**
     * Convertit une image RGB en niveaux de gris (GL).
     *
     * @param inputRGB image d'entrée en RGB
     * @return tableau 2D des niveaux de gris (0-255)
     */
    public static int[][] rgb2gl(BufferedImage inputRGB) {

        final int height = inputRGB.getHeight();

        final int width = inputRGB.getWidth();

        int[][] outGL = new int[height][width];

        for (int y = 0; y < height; y++) {

            for (int x = 0; x < width; x++) {

                int argb = inputRGB.getRGB(x, y);

                int r = (argb >> 16) & 0xFF;

                int g = (argb >> 8) & 0xFF;

                int b = argb & 0xFF;

                // luminance simple (évite float)

                int gray = (r * 299 + g * 587 + b * 114) / 1000;

                outGL[y][x] = gray;

            }

        }

        return outGL;

    }

    /**
     * Génère une permutation des entiers 0..size-1 en fonction d'une clé.
     *
     * @param size taille de la permutation
     * @param key  clé de génération (15 bits)
     * @return tableau de taille 'size' contenant une permutation des entiers
     * 0..size-1
     */

    public static int[] generatePermutation(int size, int key) {
        int[] scrambleTable = new int[size];
        int offset = getOffest(key);
        int step = getStep(key);
        for (int i = 0; i < size; i++) {
            scrambleTable[scrambleId(i, size, offset, step)] = i;
        }
        return scrambleTable;
    }

    /**
     * Génère l'inverse du tableau de permutation
     *
     * @param perm tableau de permutation
     * @param size taille du tableau de permutation
     * @return l'inverse du tableau de permutation
     */
    public static int[] generateInvertPermutation(int[] perm, int size) {
        int[] invPerm = new int[size];
        for (int i = 0; i < size; i++) {
            invPerm[perm[i]] = i;
        }
        return invPerm;
    }

    /**
     * Retourne l'offset (r) de la clé
     *
     * @param key clé de génération (15 bits)
     * @return l'offset de la clé
     */
    public static int getOffest(int key) {
        return key >> 7 & 0xFF;
    }

    /**
     * Retourne les step (s) de la clé
     *
     * @param key clé de génération (15 bits)
     * @return Step de la clé de génération
     */
    public static int getStep(int key) {
        // On applique le masque 0x7F pour garder que les 7 bits du step
        return key & 0x7F;
    }

    /**
     * Mélange les lignes d'une image selon une permutation donnée.
     *
     * @param inputImg image d'entrée
     * @param perm     permutation des lignes (taille = hauteur de l'image)
     * @return image de sortie avec les lignes mélangées
     */

    public static BufferedImage scrambleLines(BufferedImage inputImg, int[] perm) {

        int width = inputImg.getWidth();

        int height = inputImg.getHeight();

        if (perm.length != height)
            throw new IllegalArgumentException("Taille d'image <> taille permutation");

        BufferedImage out = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);

        int[] rgb = new int[width];
        for (int y = 0; y < height; y++) {
            inputImg.getRGB(0, perm[y], width, 1, rgb, 0, width);
            out.setRGB(0, y, width, 1, rgb, 0, width);
        }
        return out;

    }

    /**
     * Démélange les lignes selon un tableau de permutation donné.
     *
     * @param inputImg image d'entrée
     * @param perm     tableau permutation de la clé de déchifrement
     * @return image de sortie déchiffré
     */
    public static BufferedImage unScrambleLines(BufferedImage inputImg, int[] perm) {
        int width = inputImg.getWidth();
        int height = inputImg.getHeight();

        BufferedImage out = new BufferedImage(width,
                height, BufferedImage.TYPE_INT_ARGB);

        int[] rgb = new int[width];
        int[] invPerm = generateInvertPermutation(perm, height);

        for (int y = 0; y < height; y++) {
            inputImg.getRGB(0, invPerm[y], width, 1, rgb, 0, width);
            out.setRGB(0, y, width, 1, rgb, 0, width);
        }

        return out;

    }

    /**
     * Démélange les lignes d'une image en niveaux de gris en tableau 2D
     *
     * @param inputImageGL image d'entrée en tableau 2D
     * @param perm         tableau de permutation
     * @return image en niveaux de gris démélangé, en tableau 2D
     */
    public static int[][] unScrambleGL(int[][] inputImageGL, int[] perm) {
        int width = inputImageGL[0].length;
        int height = inputImageGL.length;
        int[][] out = new int[height][width];
        int[] invPerm = generateInvertPermutation(perm, height);

        for (int y = 0; y < height; y++) {
            out[y] = inputImageGL[invPerm[y]];
        }
        return out;
    }

    /**
     * Renvoie la position de la ligne id dans l'image brouillée.
     *
     * @param id     indice de la ligne dans l'image claire (0..size-1)
     * @param size   nombre total de lignes dans l'image
     * @param offset Offset de la clé
     * @param step   Step de la clé
     * @return indice de la ligne dans l'image brouillée (0..size-1)
     */
    public static int scrambleId(int id, int size, int offset, int step) {
        return ((offset + (2 * step + 1) * id) % size);

    }

    /**
     * Calcule la distance euclidienne entre deux lignes
     * d'une image en niveaux de gris
     *
     * @param inputImageGL image d'entrée en tableau 2D
     * @param row          le numéro de ligne de l'image (size - 1)
     * @param rowOffset    à combien d'index est la deuxième ligne à comparer
     * @return distance euclidienne
     */
    public static double euclideanDistance(int[][] inputImageGL, int row,
                                           int rowOffset) {
        int height = inputImageGL.length;
        int width = inputImageGL[0].length;
        final int STEP = width / 256 + 1; // Combien de colonnes sont sautés
        double distance = 0;

        for (int col = 0; col < width; col += STEP) {
            double x = inputImageGL[row % height][col];
            double y = inputImageGL[(row + rowOffset + height) % height][col];

            distance += ( x - y) * ( x - y);
        }

        return Math.sqrt(distance);
    }

    /**
     * Calcule la somme des niveaux de gris d'une ligne de l'image
     *
     * @param inputImageGL image d'entrée
     * @param row          numéro de ligne de l'image
     * @param width        largeur de l'image
     * @return la somme des niveaux de gris de la ligne
     */
    public static int getTotalGL(int[][] inputImageGL, int row, int width) {
        int result = 0;
        for (int i = 0; i < width; i++) {
            result += inputImageGL[row][i];
        }
        return result;
    }

    /**
     * Calcule le score de la différence euclidienne
     * <br>
     * petit score = meilleur
     *
     * @param inputImageGL image d'entrée en tableau 2D
     * @return score de différence euclidienne
     */
    public static double scoreEuclidean(int[][] inputImageGL) {
        // Combien de lignes sont sautés
        final int LINE_JUMP = 10;
        int size = inputImageGL.length;
        double score = 0;
        for (int row = 0; row < size - 1; row += LINE_JUMP) {
            score += euclideanDistance(inputImageGL, row, 1);
        }
        return score;

    }

    /**
     * Calcule la correlation de pearson entre deux lignes
     * d'une image en niveaux de gris
     *
     * @param inputImageGL image d'entrée en niveaux de gris, en tableau 2D
     * @param row          le numéro de ligne
     * @return correlation de pearson de -1 à 1
     */
    public static double pearsonCorrelation(int[][] inputImageGL,
                                            int row) {
        int width = inputImageGL[0].length;
        final int STEP = (width / 256) + 1;

        double xAvg = getTotalGL(inputImageGL, row, width) / (double) width;
        double yAvg = getTotalGL(inputImageGL, row + 1, width) / (double) width;

        double xySum = 0;
        double xSum = 0;
        double ySum = 0;

        for (int col = 0; col < width; col += STEP) {
            double x = inputImageGL[row][col] - xAvg;
            double y = inputImageGL[row + 1][col] - yAvg;

            xySum += x * y;
            xSum += x * x;
            ySum += y * y;
        }

        return xySum / (Math.sqrt(xSum) * Math.sqrt(ySum));
    }

    /**
     * Calcule le score de la correlation de pearson
     * <br>
     * scrore grand = meilleur
     *
     * @param inputImageGL
     * @return
     */
    public static double scorePearson(int[][] inputImageGL) {
        final int LINE_JUMP = 10; // Combien de lignes sont sauté
        int size = inputImageGL.length;
        double score = 0;
        for (int row = 0; row < size - 1; row += LINE_JUMP) {
            score += pearsonCorrelation(inputImageGL, row);
        }
        return score;
    }

    /**
     * Trouve la clé selon le process renseigné
     *
     * @param inputImageGL image d'entrée en niveaux de gris, en tableau 2D
     * @param process      la méthode de cassage de clé
     * @return la meilleur clé
     */
    public static int breakKey(int[][] inputImageGL, String process) {
        int key = 1;

        switch (process) {
            case "euclidean":
                key = breakKeyEuclidean(inputImageGL);
                break;
            case "pearson":
                key = breakKeyPearson(inputImageGL);
                break;
            case "neighbor":
                key = breakKeyNeighbor(inputImageGL);
                break;
            default:
                break;
        }
        return key;

    }

    /**
     * Trouve le step de la clé en bruteforce avec la distance euclidienne
     *
     * @param inputImageGL Image d'entrée en niveaux de gris, en tableau 2D
     * @return La clé trouvée
     */
    public static int findSEuclidean(int[][] inputImageGL) {
        final int N_KEY = 128;
        double score;
        int size = inputImageGL.length;
        int[] perm;
        int[][] out;
        double bestScore = Double.MAX_VALUE;
        int key = 1;

        for (int k = 0; k < N_KEY; k++) {
            perm = generatePermutation(size, k);
            out = unScrambleGL(inputImageGL, perm);

            score = scoreEuclidean(out);

            if (score < bestScore) {
                bestScore = score;
                key = k;
            }
        }

        return key;
    }

    /**
     * Trouve le step de la clé en bruteforce avec la corrélation de pearson
     *
     * @param inputImageGL Image d'entrée en niveaux de gris, en tableau 2D
     * @return La clé trouvée
     */
    public static int findSPearson(int[][] inputImageGL) {
        final int N_KEY = 128;
        double score;
        int size = inputImageGL.length;
        int[] perm;
        int[][] out;
        double bestScore = Double.MIN_VALUE;
        int key = 1;

        for (int k = 0; k < N_KEY; k++) {
            perm = generatePermutation(size, k);
            out = unScrambleGL(inputImageGL, perm);

            score = scorePearson(out);

            if (score > bestScore) {
                bestScore = score;
                key = k;
            }
        }

        return key;
    }

    public static int breakKeyEuclidean(int[][] inputImageGL) {
        int s = Profiler.analyzeFindStep(Brouillimg::findSEuclidean, inputImageGL);
        int r = Profiler.analyzeFindOffest(Brouillimg::findRScrambled, inputImageGL, 2 * s + 1);
        int key = r * 128 + s;

        System.out.println("S: " + s);
        System.out.println("R: " + r);

        return key;
    }

    public static int breakKeyPearson(int[][] inputImageGL) {
        int s = Profiler.analyzeFindStep(Brouillimg::findSPearson, inputImageGL);
        int r = Profiler.analyzeFindOffest(Brouillimg::findRScrambled, inputImageGL, 2 * s + 1);
        int key = r * 128 + s;

        System.out.println("S: " + s);
        System.out.println("R: " + r);

        return key;
    }

    /**
     * Trouve la clé en trouvant 2 lignes voisines,
     * et en faisant la différence modulaire
     *
     * @param inputImageGL Image d'entrée en niveaux de gris, en tableau 2D
     * @return la meilleure clé trouvée
     */
    public static int breakKeyNeighbor(int[][] inputImageGL) {
        int size = inputImageGL.length;

        int middleIndex = bestLineVariance(inputImageGL);
//        int middleIndex = 1;
        int[] chunk = Profiler.analyzeFindChunk(Brouillimg::getNeighborLineChunk, inputImageGL, middleIndex);
        int jump = getSmallestModularDiff(chunk, size); // jump = 2s + 1

        int s = (jump - 1) / 2;
        int r = Profiler.analyzeFindOffest(Brouillimg::findRScrambled, inputImageGL, jump);
        int key;

        System.out.println("S: " + s);
        System.out.println("R: " + r);

        key = (r * 128) + s;

        return key;
    }

    public static int bestLineVariance(int[][] inputImageGL) {
        int height = inputImageGL.length;
        final int STEP = 1;
        int bestIndex = 0;
        int score;
        int bestScore = 0;

        for (int i = 0; i < height; i += STEP) {
            score = lineVarianceScore(inputImageGL, i);

            if (bestScore < score) {
                bestScore = score;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    public static int lineVarianceScore(int[][] inputImageGL, int index) {
        int width = inputImageGL[0].length;
        final int STEP = width / 512 + 1;
        int score = 0;

        for (int i = 0; i < width - 1; i += STEP) {
            score += Math.abs(inputImageGL[index][i + 1] - inputImageGL[index][i]);
        }

        return score;
    }

    /**
     * Trouve l'offest en repérant une rupture dans la
     * distance euclidienne
     *
     * @param inputImageGL Image d'entrée en niveaux de gris, en tableau 2D
     * @param jump         La différence modulaire (donc la partie qui se fait
     *                     multiplier dans la formule de brouillage)
     * @return L'offset de la clé
     */
    public static int findRScrambled(int[][] inputImageGL, int jump) {
        int size = inputImageGL.length;
        int maxSeam = 256; // Valeur max de la coupure sur 8 bits
        double worstScore = 0;
        int worstLine = 0;

        for (int i = 0; i < Math.min(maxSeam, size); i++) { // Min pour éviter le out of bounds
            double score = euclideanDistance(inputImageGL, i, -jump);
            if (score > worstScore) {
                worstScore = score;
                worstLine = i;
            }
        }

        return worstLine;
    }

    /**
     * Trouve la plus petite différence modulaire entre le milieu du chunk
     * et les index des lignes voisines
     *
     * @param chunk Tableau de 3 index de lignes voisines
     * @param size  Hauteur de l'image
     * @return La plus petite différence modulaire des deux dans le chunk
     */
    public static int getSmallestModularDiff(int[] chunk, int size) {
        int n1 = chunk[0];
        int n2 = chunk[1];
        int mid = chunk[2];

        int diff1 = (n1 - mid + size) % size;
        int diff2 = (n2 - mid + size) % size;

        return Math.min(diff1, diff2);
    }

    /**
     * Trouve les lignes voisines d'une ligne dans une image brouillée
     *
     * @param inputImageGL Image d'entrée en niveaux de gris, en tableau 2D
     * @param index        Index de la ligne entre les deux voisines
     * @return Tableau de 3 index de lignes voisines
     */
    public static int[] getNeighborLineChunk(int[][] inputImageGL, int index) {
        int size = inputImageGL.length;
        double bestScore = Double.MAX_VALUE;
        double secondBestScore = Double.MAX_VALUE;
        double score;
        int[] chunk = new int[3];

        for (int i = 1; i < size; i++) {
            score = euclideanDistance(inputImageGL, index, i);
            if (score < bestScore) {
                secondBestScore = bestScore;
                bestScore = score;
                chunk[1] = chunk[0];
                chunk[0] = (i + index) % size;
            } else if (score < secondBestScore) {
                secondBestScore = score;
                chunk[1] = (i + index) % size;
            }
        }
        chunk[2] = index;

        return chunk;
    }

    public static void profileBreakKey(BufferedImage inputImage, int choice) {
        final int N_TEST = 50;
        final int MAX_KEY = 128 * 256 - 1;
        int height = inputImage.getHeight();
        int passedTest = 0;
        int foundKey;
        int key;
        int[] perm;
        int[][] scrambledImageGL;
        BufferedImage scrambledImage;

        for (int i = 0; i < N_TEST; i++) {
            key = genRandomKey(MAX_KEY);
            perm = generatePermutation(height, key);
            scrambledImage = scrambleLines(inputImage, perm);
            scrambledImageGL = rgb2gl(scrambledImage);

            System.out.println("Test " + i + " / " + N_TEST);

            switch (choice) {
                case 1:
                    foundKey = Profiler.analyzeKeyBreakProcess(Brouillimg::breakKeyEuclidean, scrambledImageGL);
                    break;
                case 2:
                    foundKey = Profiler.analyzeKeyBreakProcess(Brouillimg::breakKeyPearson, scrambledImageGL);
                    break;
                case 3:
                    foundKey = Profiler.analyzeKeyBreakProcess(Brouillimg::breakKeyNeighbor, scrambledImageGL);
                    break;
                default:
                    return;
            }

            if (foundKey == key) {
                passedTest++;
                System.out.println("Test passé (clé : " + key + ", clé trouvée : " + foundKey + ")");
                System.out.println();
            } else {
                System.out.println("Test raté (clé : " + key + ", clé trouvée : " + foundKey + ")");
                System.out.println();
            }
        }

        int failedTest = N_TEST - passedTest;

        System.out.println("======================");
        System.out.printf("Temps moyen : %.2f ms\n", Profiler.getTimeAvg());
        System.out.println("Tests passés : " + passedTest);
        System.out.println("Tests ratés : " + failedTest);

        if (passedTest != 0) {
            System.out.printf("%.2f%% de succès\n", ((double) passedTest / N_TEST) * 100);
        } else {
            System.out.println("0% de succès\n");
        }
        Profiler.resetCounter();
        Profiler.resetTime();
    }

    public static int genRandomKey(int max) {
        return (int) (Math.random() * max);
    }
}