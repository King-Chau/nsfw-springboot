package com.nsfw.image;

import org.tensorflow.Tensor;
import org.tensorflow.ndarray.Shape;
import org.tensorflow.types.TFloat32;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class ImageCensor {
    private static final String INPUT_OP_NAME = "input_1";
    private static final String OUTPUT_OP_NAME = "dense_3/Softmax";
    private static final String[] CLASSES = {"Drawing", "Hentai", "Neutral", "Porn", "Sexy"};
    private static final int IMAGE_SIZE = 299;

    /**
     * nsfw image classification
     */
    public static Map<String, Float> predict(String imagePath) throws Exception {
        File imgFile = new File(imagePath);
        if (!imgFile.exists() || !imgFile.isFile()) {
            throw new IllegalArgumentException("无效图片路径: " + imagePath);
        }

        float[][][][] inputData = preprocessImage(imagePath);

        try (TFloat32 inputTensor = createInputTensor(inputData, IMAGE_SIZE)) {
            try (Tensor outputTensor = ModelHolder.getSession()
                    .runner()
                    .feed(INPUT_OP_NAME, inputTensor)
                    .fetch(OUTPUT_OP_NAME)
                    .run()
                    .get(0)) {

                float[] probabilities = processOutput(outputTensor);
                printResults(probabilities);
                return mapLabels(probabilities);
            }
        }
    }

    private static TFloat32 createInputTensor(float[][][][] inputData, int imageSize) {
        TFloat32 tensor = TFloat32.tensorOf(Shape.of(1, imageSize, imageSize, 3));
        for (int b = 0; b < 1; b++) {
            for (int y = 0; y < imageSize; y++) {
                for (int x = 0; x < imageSize; x++) {
                    for (int c = 0; c < 3; c++) {
                        tensor.setFloat(inputData[b][y][x][c], b, y, x, c);
                    }
                }
            }
        }
        return tensor;
    }

    private static float[] processOutput(Tensor outputTensor) {
        if (!(outputTensor instanceof TFloat32)) {
            throw new IllegalStateException("输出类型错误: " + outputTensor.getClass());
        }

        TFloat32 floatTensor = (TFloat32) outputTensor;
        Shape shape = floatTensor.shape();
        if (shape.numDimensions() != 2 || shape.size(0) != 1 || shape.size(1) != CLASSES.length) {
            throw new IllegalStateException("输出维度不符: " + shape);
        }

        float[] results = new float[CLASSES.length];
        for (int i = 0; i < CLASSES.length; i++) {
            results[i] = floatTensor.getFloat(0, i);
        }
        return results;
    }

    private static void printResults(float[] probabilities) {
        System.out.println("📊 预测结果:");
        int maxIdx = 0;
        float maxProb = probabilities[0];

        for (int i = 0; i < CLASSES.length; i++) {
            System.out.printf(" - %-8s: %.4f%n", CLASSES[i], probabilities[i]);
            if (probabilities[i] > maxProb) {
                maxProb = probabilities[i];
                maxIdx = i;
            }
        }

        System.out.println("✅ 最终判断: " + CLASSES[maxIdx]);
    }

    private static float[][][][] preprocessImage(String path) throws Exception {
        BufferedImage img = ImageIO.read(new File(path));
        BufferedImage resized = new BufferedImage(IMAGE_SIZE, IMAGE_SIZE, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(img, 0, 0, IMAGE_SIZE, IMAGE_SIZE, null);
        g.dispose();

        float[][][][] result = new float[1][IMAGE_SIZE][IMAGE_SIZE][3];
        for (int y = 0; y < IMAGE_SIZE; y++) {
            for (int x = 0; x < IMAGE_SIZE; x++) {
                int rgb = resized.getRGB(x, y);
                result[0][y][x][0] = ((rgb >> 16) & 0xFF) / 255.0f;
                result[0][y][x][1] = ((rgb >> 8) & 0xFF) / 255.0f;
                result[0][y][x][2] = (rgb & 0xFF) / 255.0f;
            }
        }
        return result;
    }

    private static Map<String, Float> mapLabels(float[] probabilities) {
        Map<String, Float> result = new LinkedHashMap<>();
        for (int i = 0; i < CLASSES.length; i++) {
            result.put(CLASSES[i], probabilities[i]);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        String[] images = {
                "images/drawing.png",
                "images/hentai.jpeg",
                "images/neutral.jpeg",
                "images/porn.jpeg",
                "images/sexy.png"
        };
        for (String image : images) {
            System.out.println("----------------");
            System.out.println("image: " + image);
            long start = System.currentTimeMillis();
            predict(image);
            long cost = System.currentTimeMillis() - start;
            System.out.println("耗时: " + cost + "ms");
        }
        ModelHolder.close();
    }
}
