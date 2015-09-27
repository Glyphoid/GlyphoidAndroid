package me.scai.plato.android.responses;


import me.scai.plato.MathJaxWebClient.MathJaxWebClient;

public class MathJaxClientTaskResponse {
    /* Member variables */
    private byte[] imageData;
    private String conversionResult;
    private String format;

    private static final String FORMAT_IMAGE_PNG = MathJaxWebClient.getFormatImagePng();
    private static final String FORMAT_STRING_MATH_ML = MathJaxWebClient.getFormatStringMathML();

    /* Constructors */
    public MathJaxClientTaskResponse() {

    }

    public MathJaxClientTaskResponse(byte[] imageData) {
        this.imageData = imageData;
    }

    /* Getters */
    public byte[] getImageData() {
        return imageData;
    }

    public String getConversionResult() {
        return conversionResult;
    }

    public String getFormat() {
        return format;
    }

    public static String getFormatImagePng() {
        return FORMAT_IMAGE_PNG;
    }

    public static String getFormatStringMathML() {
        return FORMAT_STRING_MATH_ML;
    }

    /* Setters */
    public void setImageData(byte[] imageData) {
        this.imageData = imageData;
    }

    public void setConversionResult(String conversionResult) {
        this.conversionResult = conversionResult;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
