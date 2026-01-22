package com.example.maintenance_dashboard.ai; // Changed Package

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.maintenance_dashboard.data.DamageAnalysisResult; // Changed Import
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.common.ops.NormalizeOp;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;
import java.util.Map;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

public class TFLiteDamageAnalyzer implements DamageAnalyzer {

    private static final String TAG = "TFLiteDamageAnalyzer";
    private static final String MODEL_PATH = "model.tflite";
    private static final String LABELS_PATH = "labels.txt";

    // Adjust these if your model uses a different input size.
    private static final int IMAGE_SIZE_X = 224;
    private static final int IMAGE_SIZE_Y = 224;

    private final Interpreter tflite;
    private final List<String> labels;

    // Reusable buffers
    private final TensorImage inputImageBuffer;
    private final TensorBuffer outputProbabilityBuffer;

    public TFLiteDamageAnalyzer(Context context) throws IOException {
        // Load the .tflite model from assets.
        MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, MODEL_PATH);
        this.tflite = new Interpreter(tfliteModel);

        // Load labels from assets/labels.txt and clean them
        List<String> rawLabels = FileUtil.loadLabels(context, LABELS_PATH);
        this.labels = new java.util.ArrayList<>();
        for (String s : rawLabels) {
            if (s != null && !s.trim().isEmpty()) {
                this.labels.add(s.trim());
            }
        }

        // Prepare input buffer based on model input tensor.
        org.tensorflow.lite.Tensor inputTensor = tflite.getInputTensor(0);
        inputImageBuffer = new TensorImage(inputTensor.dataType());

        // Prepare output buffer dynamically based on model output tensor.
        int probabilityTensorIndex = 0;
        org.tensorflow.lite.Tensor outputTensor = tflite.getOutputTensor(probabilityTensorIndex);
        int[] probabilityShape = outputTensor.shape(); // e.g. [1, numClasses]
        org.tensorflow.lite.DataType probabilityDataType = outputTensor.dataType();
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
    }

    @Override
    public DamageAnalysisResult analyze(Bitmap bitmap) {
        // 1) Pre-process into TensorImage
        TensorImage processedImage = preprocessImage(bitmap);

        // 2) Run inference
        tflite.run(processedImage.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());

        // 3) Post-process into DamageAnalysisResult
        return postprocess();
    }

    // Now returns TensorImage, not Bitmap
    private TensorImage preprocessImage(Bitmap bitmap) {
        inputImageBuffer.load(bitmap);

        org.tensorflow.lite.Tensor inputTensor = tflite.getInputTensor(0);
        org.tensorflow.lite.DataType inputDataType = inputTensor.dataType();

        ImageProcessor.Builder builder = new ImageProcessor.Builder()
                .add(new ResizeOp(IMAGE_SIZE_Y, IMAGE_SIZE_X, ResizeOp.ResizeMethod.BILINEAR));

        // If model expects float input, normalize to [0,1].
        if (inputDataType == org.tensorflow.lite.DataType.FLOAT32) {
            builder.add(new NormalizeOp(0.0f, 255.0f));
        }
        // If model expects UINT8, do NOT normalize (keep 0..255).

        ImageProcessor imageProcessor = builder.build();
        return imageProcessor.process(inputImageBuffer);
    }

    private DamageAnalysisResult postprocess() {
        // Map output probabilities to labels.
        TensorLabel tensorLabel = new TensorLabel(labels, outputProbabilityBuffer);
        Map<String, Float> probabilityMap = tensorLabel.getMapWithFloatValue();

        Log.d(TAG, "Full Probability Map: " + probabilityMap.toString());

        // Find label with highest probability.
        String highestConfidenceLabel = null;
        float maxConfidence = -1f;

        for (Map.Entry<String, Float> entry : probabilityMap.entrySet()) {
            if (entry.getValue() > maxConfidence) {
                maxConfidence = entry.getValue();
                highestConfidenceLabel = entry.getKey();
            }
        }

        Log.d(TAG, "Highest Confidence Label: " + highestConfidenceLabel + " (Confidence: " + maxConfidence + ")");

        // Default
        DamageAnalysisResult.Severity severity = DamageAnalysisResult.Severity.MINOR;

        if (highestConfidenceLabel != null) {
            String labelLower = highestConfidenceLabel.toLowerCase().trim();
            if (labelLower.contains("major")) {
                severity = DamageAnalysisResult.Severity.MAJOR;
            } else if (labelLower.contains("moderate")) {
                severity = DamageAnalysisResult.Severity.MODERATE;
            } else if (labelLower.contains("minor")) {
                severity = DamageAnalysisResult.Severity.MINOR;
            } else {
                Log.w(TAG, "Unrecognized label: " + highestConfidenceLabel);
            }
        } else {
            Log.w(TAG, "No label found in probability map.");
        }

        String explanation = String.format(
                "Detected: %s (Confidence: %.2f)",
                highestConfidenceLabel != null ? highestConfidenceLabel : "Unknown",
                maxConfidence);

        return new DamageAnalysisResult(severity, maxConfidence, explanation);
    }
}
