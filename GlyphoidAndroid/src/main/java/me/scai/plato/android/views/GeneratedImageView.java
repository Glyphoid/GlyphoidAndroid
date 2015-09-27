package me.scai.plato.android.views;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.squareup.otto.Bus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import me.scai.plato.MathJaxWebClient.MathJaxWebClient;
import me.scai.plato.android.utils.EventBus;
import me.scai.plato.android.events.GeneratedImageViewEvent;
import me.scai.plato.android.responses.MathJaxClientTaskResponse;
import me.scai.plato.android.tasks.MathJaxWebClientAsyncTask;
import me.scai.plato.android.requests.MathJaxWebClientTaskRequest;
import me.scai.plato.android.R;
import me.scai.plato.android.helpers.FileSystemHelper;
import me.scai.plato.android.listeners.DismissClickListener;

public class GeneratedImageView extends Fragment {
    /* Constants */
    private static String GENERATED_IMAGES_DIR = "generated_images";

    /* Member variables */
    private static String mathJaxEndpointUrl; // TODO: Move the config

    private Context ctx;

    private static String generateImagesDir;

    private Button dismiss;
    private Button shareGeneratedImage;

    private ObservableScrollView obsScrollView;
    private ImageView generatedImage;

    private MathJaxWebClient mathJaxWebClient; //TODO: Is this the right place to put it?

    private String mathTex;
    private ProgressDialog mathJaxProgressDialog;

//    private SVG svg;

    /* Constructor */
    public GeneratedImageView(Context ctx) {
        this.ctx = ctx;

        mathJaxEndpointUrl = ctx.getString(R.string.mathjax_endpoint);

        mathJaxWebClient = new MathJaxWebClient(mathJaxEndpointUrl);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        obsScrollView = (ObservableScrollView) inflater.inflate(R.layout.generated_image_fragment, container, false);

        generatedImage = (ImageView) obsScrollView.findViewById(R.id.generatedImage);

        shareGeneratedImage = (Button) obsScrollView.findViewById(R.id.shareGeneratedImage);
        dismiss = (Button) obsScrollView.findViewById(R.id.dismissGeneratedImage);

        shareGeneratedImage.setOnClickListener(shareGeneratedImageListener);
        dismiss.setOnClickListener(new DismissClickListener(getFragmentManager()));

        generatedImage.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Display progress dialog
        mathJaxProgressDialog = ProgressDialog.show(ctx,
                                                    "",
                                                    ctx.getString(R.string.waiting_for_mathjax_server_response),
                                                    true,
                                                    false); // TODO: Make cancellable

        final String mathTex = getMathTex();
        double dpi = 1600; // TODO: Programmatically set it
        MathJaxWebClientTaskRequest request = new MathJaxWebClientTaskRequest(mathJaxWebClient,
                                                                              mathTex,
                                                                              MathJaxWebClientAsyncTask.getFormatImagePng(),
                                                                              400,
                                                                              dpi);
        new MathJaxWebClientAsyncTask().execute(request);

        return obsScrollView;
    }

    @Override
    public void onStart() {
        Bus bus = EventBus.getInstance();
        bus.post(new GeneratedImageViewEvent(GeneratedImageViewEvent.EventType.Open));

        super.onStart();
    }

    @Override
    public void onStop() {
        Bus bus = EventBus.getInstance();
        bus.post(new GeneratedImageViewEvent(GeneratedImageViewEvent.EventType.Close));

        super.onStop();
    }


    /* Methods */
    public void setMathTex(String mathTex) {
        this.mathTex = mathTex;
    }

    public String getMathTex() {
        return this.mathTex;
    }

    private final View.OnClickListener shareGeneratedImageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

//            if (bitmap == null) {
//                return;
//            }
            // TODO: Use disable/enable button

            Bitmap bitmap = ((BitmapDrawable) generatedImage.getDrawable()).getBitmap();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
//            shareIntent.setType("image/jpeg");

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes);

            generateImagesDir = FileSystemHelper.ensureExternalDirectory(ctx, GENERATED_IMAGES_DIR);
            File f = new File(generateImagesDir + File.separator + "temp_shared_generated_image.png");

            try {
                f.createNewFile();
                FileOutputStream fo = new FileOutputStream(f);
                fo.write(bytes.toByteArray());
                fo.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file:///storage/sdcard0/temp_shared_generated.png")); // TODO: Do not hard code
            ctx.startActivity(Intent.createChooser(shareIntent, "Share Generated Image"));
        }
    };

    public void postMathJaxClientTaskResponse(MathJaxClientTaskResponse response) {
        // TODO: Handle and display error messages

        if (response.getFormat().equals(MathJaxWebClientAsyncTask.getFormatImagePng()) &&
            response.getImageData() != null) {
            byte[] pngData = response.getImageData();
            if (pngData != null) {
                showPngImage(pngData);
            }
        } else {
            //TODO
        }



        dismissProgressDialog();
    }

    private void showPngImage(byte[] pngData) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(pngData, 0, pngData.length);

        Bitmap newBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_4444);
        for(int x = 0; x < newBitmap.getWidth(); x++){
            for(int y = 0; y < newBitmap.getHeight(); y++){
                if(bitmap.getPixel(x, y) == Color.TRANSPARENT){
                    newBitmap.setPixel(x, y, Color.WHITE);
                } else {
                    newBitmap.setPixel(x, y, bitmap.getPixel(x, y));
                }
            }
        }

//        generatedImage.setBackgroundColor(Color.WHITE);
        generatedImage.setImageBitmap(newBitmap);
    }

    private void dismissProgressDialog() {
        if (mathJaxProgressDialog != null) {
            mathJaxProgressDialog.dismiss();
        }
    }


}
