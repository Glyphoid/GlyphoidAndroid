package me.scai.plato.android.tasks;

import android.os.AsyncTask;

import com.squareup.otto.Bus;

import java.util.Date;
import java.util.concurrent.Semaphore;

import me.scai.plato.MathJaxWebClient.MathJaxWebClient;
import me.scai.plato.MathJaxWebClient.MathJaxWebClientException;
import me.scai.plato.android.requests.MathJaxWebClientTaskRequest;
import me.scai.plato.android.responses.MathJaxClientTaskResponse;
import me.scai.plato.android.utils.EventBus;

public class MathJaxWebClientAsyncTask extends AsyncTask<MathJaxWebClientTaskRequest, Void, MathJaxClientTaskResponse> {
    private static final Semaphore semaphore = new Semaphore(1);

    private static final String FORMAT_IMAGE_PNG = MathJaxWebClient.getFormatImagePng();
    private static final String FORMAT_STRING_MATH_ML = MathJaxWebClient.getFormatStringMathML();

    public static String getFormatStringMathML() {
        return FORMAT_STRING_MATH_ML;
    }

    public static String getFormatImagePng() {
        return FORMAT_IMAGE_PNG;
    }

    @Override
    public MathJaxClientTaskResponse doInBackground(MathJaxWebClientTaskRequest... args) {
        try {
            semaphore.acquire();
        } catch (InterruptedException exc) {
            // TODO
        }

        /* Get start time */
        long startTimeMillis = new Date().getTime();

        MathJaxWebClientTaskRequest req = args[0];
        MathJaxWebClient webClient = req.getMathJaxWebClient();

        MathJaxClientTaskResponse resp = new MathJaxClientTaskResponse();

        if (webClient != null) {
            try {
                resp.setFormat(req.getImageFormat());

                if (req.getImageFormat().equals(FORMAT_IMAGE_PNG)) {
                    byte[] pngData = webClient.tex2png(req.getMathTex(), req.getImageDpi());
                    resp.setImageData(pngData);
                } else if (req.getImageFormat().equals(FORMAT_STRING_MATH_ML)) {
                    resp.setConversionResult(webClient.tex2mathML(req.getMathTex()));
                }
            } catch (MathJaxWebClientException exc) {
                resp.setConversionResult(exc.getMessage());
            }
        }

        semaphore.release();

        return resp;
    }

    @Override
    public void onPostExecute(MathJaxClientTaskResponse resp) {
        final Bus eventBus = EventBus.getInstance();

        eventBus.post(resp);
    }

}
