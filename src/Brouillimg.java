import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.concurrent.atomic.AtomicLong;
// import java.util.stream.IntStream;

public class Brouillimg {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println(
                    "Usage: java Brouillimg <process> <image_claire> <clé> [image_sortie] [image de comparaison]");
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

        int[] perm = generatePermutation(height, key);

        switch (process) {
            case "scramble":
                BufferedImage scrambledImage = scrambleLines(inputImage, perm);
                ImageIO.write(scrambledImage, "png", new File(outPath));
                System.out.println("Image écrite: " + outPath);
                break;
            case "unscramble":
                BufferedImage unScrambleImage = unScrambleLines(inputImage, perm);
                if (args.length == 5) {
                    BufferedImage compImage = ImageIO.read(new File(args[4]));
                    System.out.println("Images identiques: " + isImageIdentical(unScrambleImage, compImage));
                }
                System.out.println("Image écrite: " + outPath);
                ImageIO.write(unScrambleImage, "png", new File(outPath));
                break;
            case "euclidean",
                    "pearson":
                key = breakKey(inputImageGL, process);
                System.out.println("Clé trouvé: " + key);
                break;
            default:
                throw new IOException("Méthode non renseignée");
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
     *         0..size-1
     */

    public static int[] generatePermutation(int size, int key) {
        int[] scrambleTable = new int[size];
        for (int i = 0; i < size; i++) {
            scrambleTable[scrambleId(i, size, key)] = i;
        }
        return scrambleTable;
    }

    /**
     * Génère l'inverse du tableau de permutation
     * 
     * @param perm tableau de permutation
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
     * Vérifie si les deux images sont identiques
     * 
     * @param image1 image 1
     * @param image2 image 2
     * @return Vrai si les deux images sont identique, faux sinon
     */
    public static boolean isImageIdentical(BufferedImage image1, BufferedImage image2) {
        int height1 = image1.getHeight();
        int width1 = image1.getWidth();

        int height2 = image2.getHeight();
        int width2 = image2.getWidth();

        if (height1 != height2 || width1 != width2) {
            return false;
        }

        int i = 0;

        while (i < width1 * height1) {
            if (image1.getRGB(i % width1, i / width1) != image2.getRGB(i % width2, i / width2)) {
                return false;
            }
            i++;
        }
        return true;
    }

    /**
     * Retourne l'offset de la clé
     * 
     * @param key clé de génération (15 bits)
     * @return l'offset de la clé
     */
    public static int getOffest(int key) {
        return key >> 7 & 0xFF;
    }

    /**
     * Retourne les step de la clé
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

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int[] rgb = new int[width];
        for (int y = 0; y < height; y++) { 
            inputImg.getRGB(0, perm[y], width, 1, rgb, 0, width);
            out.setRGB(0, y, width, 1, rgb, 0, width);
        }
        return out;

    }

    /**
     * Déchiffre les ligne selon un tableau de permutation donné.
     *
     * @param inputImg image d'entrée
     * @param perm     tableau permutation de la clé de déchifrement
     * @return image de sortie déchiffré
     */
    public static BufferedImage unScrambleLines(BufferedImage inputImg, int[] perm) {
        int width = inputImg.getWidth();
        int height = inputImg.getHeight();

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
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
     * @param id   indice de la ligne dans l'image claire (0..size-1)
     * @param size nombre total de lignes dans l'image
     * @param key  clé de brouillage (15 bits)
     * @return indice de la ligne dans l'image brouillée (0..size-1)
     */
    public static int scrambleId(int id, int size, int key) {
        int offset = getOffest(key);
        int step = getStep(key);
        return ((offset + (2 * step + 1) * id) % size);

    }

    /**
     * Calcule la distance euclidienne entre deux lignes d'une image en niveaux de
     * gris
     * 
     * @param inputImageGL image d'entrée en tableau 2D
     * @param size         la taille de l'image
     * @param row          le numéro de ligne de l'image (size - 1)
     * @return distance euclidienne
     */
    public static double euclideanDistance(int[][] inputImageGL, int size, int row) {
        double distance = 0;
        int xAvg = getTotalGL(inputImageGL, row, size) / size;
        int yAvg = getTotalGL(inputImageGL, row + 1, size) / size;
        for (int col = 0; col < size; col++) {
            int x = inputImageGL[row][col] - xAvg;
            int y = inputImageGL[row + 1][col] - yAvg;

            distance += Math.pow(x - y, 2);
        }

        return Math.sqrt(distance);
    }

    /**
     * Calcule la somme des niveaux de gris d'une ligne de l'image
     * 
     * @param inputImageGL image d'entrée
     * @param row          numéro de ligne de l'image
     * @param size         taille de l'image
     * @return la somme des niveaux de gris de la ligne
     */
    public static int getTotalGL(int[][] inputImageGL, int row, int size) {
        int result = 0;
        for (int i = 0; i < size; i++) {
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
    public static double scoreEuclidean(int[][] inputImageGL, int lineJump) {
        int size = inputImageGL.length;
        double score = 0;
        for (int row = 0; row < size - 1; row += lineJump) {
            score += euclideanDistance(inputImageGL, size, row);
        }
        return score;

    }

    /**
     * Calcule la correlation de pearson entre deux lignes d'une image en niveaux de
     * gris
     * 
     * @param inputImageGL image d'entrée en niveaux de gris, en tableau 2D
     * @param row          le numéro de ligne
     * @param size         la taille de l'image
     * @return correlation de pearson de -1 à 1
     */
    public static double pearsonCorrelation(int[][] inputImageGL, int row, int size) {
        int xAvg = getTotalGL(inputImageGL, row, size) / size;
        int yAvg = getTotalGL(inputImageGL, row + 1, size) / size;
        int xySum = 0;
        int xSum = 0;
        int ySum = 0;
        for (int col = 0; col < size; col++) {
            int x = inputImageGL[row][col] - xAvg;
            int y = inputImageGL[row + 1][col] - yAvg;

            xySum += x * y;
            xSum += Math.pow(x, 2);
            ySum += Math.pow(y, 2);
        }
        // System.out.println(xySum + " " + xSum + " " + ySum);
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
    public static double scorePearson(int[][] inputImageGL, int lineJump) {
        int size = inputImageGL.length;
        double score = 0;
        for (int row = 0; row < size - 1; row += lineJump) {
            score += pearsonCorrelation(inputImageGL, row, size);
        }
        return score;
    }

    /**
     * Teste les clés en bruteforce
     * 
     * @param inputImageGL image d'entrée en niveaux de gris, en tableau 2D
     * @return la meilleur clé
     */
    public static int breakKey(int[][] inputImageGL, String process) {
        int key = 1;
        double score;
        int size = inputImageGL.length;
        double bestScore;
        int nKeyS = 128;
        int nKeyR = (int) Math.pow(2, 15);
        int sLineJump = 10; // Combien de lignes sont sauté pour trouver le s
        int rLineJump = 1; // Combien de lignes sont sauté pour trouver le r

        int[] perm = generatePermutation(size, key);
        int[][] out = unScrambleGL(inputImageGL, perm);
        switch (process) {
            case "euclidean":
                bestScore = scoreEuclidean(out, sLineJump);
                for (int k = 1; k < nKeyS; k++) {
                    perm = generatePermutation(size, k);
                    out = unScrambleGL(inputImageGL, perm);

                    score = scoreEuclidean(out, sLineJump);

                    if (score < bestScore) {
                        bestScore = score;
                        key = k;
                    }
                }
                bestScore = scoreEuclidean(out, rLineJump);
                for (int k = key; k < nKeyR; k += nKeyS) {
                    perm = generatePermutation(size, k);
                    out = unScrambleGL(inputImageGL, perm);

                    score = scoreEuclidean(out, rLineJump);

                    if (score < bestScore) {
                        bestScore = score;
                        key = k;
                    }
                }
                break;
            case "pearson":
                bestScore = scorePearson(out, sLineJump);
                for (int k = 1; k < nKeyS; k++) {
                    perm = generatePermutation(size, k);
                    out = unScrambleGL(inputImageGL, perm);

                    score = scorePearson(out, rLineJump);

                    if (score > bestScore) {
                        bestScore = score;
                        key = k;
                    }
                }
                bestScore = scorePearson(out, rLineJump);
                for (int k = key; k < nKeyR; k += nKeyS) {
                    perm = generatePermutation(size, k);
                    out = unScrambleGL(inputImageGL, perm);

                    score = scorePearson(out, rLineJump);

                    if (score > bestScore) {
                        bestScore = score;
                        key = k;
                    }
                }
                break;
            default:
                break;
        }
        return key;

    }

}