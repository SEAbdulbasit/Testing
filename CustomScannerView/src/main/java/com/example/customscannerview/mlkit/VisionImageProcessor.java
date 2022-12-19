package com.example.customscannerview.mlkit;

import androidx.camera.core.ImageProxy;

import com.google.mlkit.common.MlKitException;


public interface VisionImageProcessor {

    void processImageProxy(ImageProxy image, GraphicOverlay graphicOverlay) throws MlKitException;

    void stop();
}
