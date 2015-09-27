package me.scai.plato.android.requests;

import me.scai.plato.MathJaxWebClient.MathJaxWebClient;

public class MathJaxWebClientTaskRequest {
    /* Member variables */
    private MathJaxWebClient mathJaxWebClient;

    private String mathTex;
    private String imageFormat;
    private double imageWidth;
    private double imageDpi;

    /* Constructors */
    public MathJaxWebClientTaskRequest(MathJaxWebClient mathJaxWebClient,
                                       String mathTex,
                                       String imageFormat) {
        this.mathJaxWebClient = mathJaxWebClient;

        this.mathTex     = mathTex;
        this.imageFormat = imageFormat;
    }

    public MathJaxWebClientTaskRequest(MathJaxWebClient mathJaxWebClient,
                                       String mathTex,
                                       String imageFormat,
                                       double imageWidth,
                                       double imageDpi) {
        this(mathJaxWebClient, mathTex, imageFormat);

        this.imageWidth  = imageWidth;
        this.imageDpi    = imageDpi;
    }

    /* Getters */
    public MathJaxWebClient getMathJaxWebClient() {
        return mathJaxWebClient;
    }

    public String getMathTex() {
        return mathTex;
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public double getImageWidth() {
        return imageWidth;
    }

    public double getImageDpi() {
        return imageDpi;
    }
}
